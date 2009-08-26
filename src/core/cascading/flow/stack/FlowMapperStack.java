/*
 * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.flow.stack;

import cascading.flow.FlowElement;
import cascading.flow.FlowStep;
import cascading.flow.Scope;
import cascading.flow.StepCounters;
import cascading.flow.hadoop.HadoopFlowProcess;
import cascading.pipe.Each;
import cascading.pipe.Group;
import cascading.pipe.Pipe;
import cascading.tap.Tap;
import cascading.tuple.Tuple;
import cascading.util.Util;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public class FlowMapperStack
  {
  /** Field LOG */
  private static final Logger LOG = Logger.getLogger( FlowMapperStack.class );

  /** Field step */
  private final FlowStep step;
  /** Field currentSource */
  private final Tap currentSource;
  /** Field flowSession */
  private final HadoopFlowProcess flowProcess;

  /** Field stack */
  private Stack stacks[];

  /** Class Stack is a simple holder for stack head and tails */
  private class Stack
    {
    /** Field stackHead */
    MapperStackElement head;
    /** Field stackTail */
    MapperStackElement tail;
    }

  public FlowMapperStack( HadoopFlowProcess flowProcess ) throws IOException
    {
    this.flowProcess = flowProcess;

    Configuration conf = flowProcess.getConfiguration();
    step = (FlowStep) Util.deserializeBase64( conf.getRaw( "cascading.flow.step" ) );

    // is set by the MultiInputSplit
    currentSource = (Tap) Util.deserializeBase64( conf.getRaw( "cascading.step.source" ) );

    if( LOG.isDebugEnabled() )
      LOG.debug( "map current source: " + currentSource );

    buildStack();

    for( Stack stack : stacks )
      stack.tail.open();
    }

  private void buildStack() throws IOException
    {
    Set<Scope> incomingScopes = step.getNextScopes( currentSource );

    stacks = new Stack[incomingScopes.size()];

    int i = 0;

    for( Scope incomingScope : incomingScopes )
      {
      FlowElement operator = step.getNextFlowElement( incomingScope );

      stacks[ i ] = new Stack();

      stacks[ i ].tail = null;

      while( operator instanceof Each )
        {
        Tap trap = step.getMapperTrap( ( (Pipe) operator ).getName() );
        stacks[ i ].tail = new EachMapperStackElement( stacks[ i ].tail, flowProcess, incomingScope, trap, (Each) operator );

        incomingScope = step.getNextScope( operator );
        operator = step.getNextFlowElement( incomingScope );
        }

      if( operator instanceof Group )
        {
        Scope outgoingScope = step.getNextScope( operator ); // is always Group

        Tap trap = step.getMapperTrap( ( (Pipe) operator ).getName() );
        stacks[ i ].tail = new GroupMapperStackElement( stacks[ i ].tail, flowProcess, incomingScope, trap, (Group) operator, outgoingScope );
        }
      else if( operator instanceof Tap )
        {
        stacks[ i ].tail = new TapMapperStackElement( stacks[ i ].tail, flowProcess, incomingScope, (Tap) operator );
        }
      else
        throw new IllegalStateException( "operator should be group or tap, is instead: " + operator.getClass().getName() );

      stacks[ i ].head = (MapperStackElement) stacks[ i ].tail.resolveStack();
      stacks[ i ].tail.setLastOutput( flowProcess.getContext() );

      i++;
      }
    }

  public void map( Object key, Object value ) throws IOException
    {
    flowProcess.increment( StepCounters.Tuples_Read, 1 );

    for( int i = 0; i < stacks.length; i++ )
      {
      Tuple tuple = currentSource.source( key, value );

      if( LOG.isDebugEnabled() )
        {
        if( tuple == null )
          LOG.debug( "map skipping key and value" );

        if( LOG.isTraceEnabled() )
          {
          if( key instanceof Tuple )
            LOG.trace( "map key: " + ( (Tuple) key ).print() );
          else
            LOG.trace( "map key: [" + key + "]" );

          if( tuple != null )
            LOG.trace( "map value: " + tuple.print() );
          }
        }

      // skip the key/value pair if null is returned from the source
      if( tuple == null )
        return;

      try
        {
        stacks[ i ].head.collect( tuple );
        }
      catch( StackException exception )
        {
        if( exception.getCause() instanceof IOException )
          throw (IOException) exception.getCause();

        throw (RuntimeException) exception.getCause();
        }
      }
    }

  public void close() throws IOException
    {
    for( int i = 0; i < stacks.length; i++ )
      stacks[ i ].head.close();
    }
  }