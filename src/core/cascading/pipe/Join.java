/*
 * Copyright (c) 2007-2012 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.pipe;

import java.beans.ConstructorProperties;

import cascading.pipe.joiner.Joiner;
import cascading.tuple.Fields;

/**
 * The Join pipe allows for two or more tuple streams to join into a single stream via an optional {@link Joiner} when
 * all but one tuple stream is considered small enough to fit into memory.
 * <p/>
 * When planned onto MapReduce, this is effectively a non-blocking "asymmetrical join", where the left-most side
 * will not block (accumulate into memory) in order to complete the join, but the right-most sides will. See below...
 * <p/>
 * No aggregations can be performed with a Join pipe as there is no guarantees all value will be associated with
 * a given grouping key. In fact, an Aggregator would see the same grouping many times with a partial set of values.
 * <p/>
 * For every incoming {@link Pipe} instance, a {@link Fields} instance must be specified that denotes the field names
 * or positions that should be joined with the other given Pipe instances. If the incoming Pipe instances declare
 * one or more field with the same name, the declaredFields must be given to name the outgoing Tuple stream fields
 * to overcome field name collisions.
 * <p/>
 * By default Join performs an inner join via the {@link cascading.pipe.joiner.InnerJoin} {@link cascading.pipe.joiner.Joiner} class.
 * <p/>
 * Self joins can be achieved by using a constructor that takes a single Pipe and a numSelfJoins value. A value of
 * 1 for numSelfJoins will join the Pipe with itself once. Note that a self join will block until all data is accumulated
 * thus the stream must be reasonably small.
 * <p/>
 * Note "outer" joins on the left most side will not behave as expected. All observed keys on the right most sides
 * will be emitted with {@code null} for the left most stream, thus when running distributed, duplicate values will
 * emerge from every Map task split on the MapReduce platform.
 * <p/>
 * Join does not scale well to large data sizes and thus requires streams with more data on the left hand side to
 * join with more sparse data on the right hand side. That is, always attempt to effect M x N joins where M is large
 * and N is small, instead of where M is small and N is large. Right hand side streams will be accumulated, and
 * spilled to disk if the collection reaches a specific threshold when using Hadoop.
 * <p/>
 * If spills are happening, consider increasing the spill thresholds, see {@link cascading.tuple.SpillableTupleMap}.
 * * <p/>
 * See the {@link cascading.tuple.TupleCollectionFactory} and {@link cascading.tuple.TupleMapFactory} for a means
 * to use alternative spillable types.
 *
 * @see cascading.pipe.joiner.InnerJoin
 * @see cascading.pipe.joiner.OuterJoin
 * @see cascading.pipe.joiner.LeftJoin
 * @see cascading.pipe.joiner.RightJoin
 * @see cascading.pipe.joiner.MixedJoin
 * @see cascading.tuple.Fields
 * @see cascading.tuple.SpillableTupleMap
 */
public class Join extends Splice
  {
  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   */
  @ConstructorProperties({"joinName", "lhs", "lhsJoinFields", "rhs", "rhsJoinFields"})
  public Join( String joinName, Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields )
    {
    super( joinName, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), null, null );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param joiner
   */
  @ConstructorProperties({"joinName", "lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "joiner"})
  public Join( String joinName, Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Joiner joiner )
    {
    super( joinName, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), null, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param declaredFields
   */
  @ConstructorProperties({"joinName", "lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "declaredFields"})
  public Join( String joinName, Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Fields declaredFields )
    {
    super( joinName, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), declaredFields, null );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"joinName", "lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "declaredFields", "joiner"})
  public Join( String joinName, Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Fields declaredFields, Joiner joiner )
    {
    super( joinName, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), declaredFields, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"joinName", "pipe", "joinFields", "numSelfJoins", "declaredFields", "joiner"})
  public Join( String joinName, Pipe pipe, Fields joinFields, int numSelfJoins, Fields declaredFields, Joiner joiner )
    {
    super( joinName, pipe, joinFields, numSelfJoins, declaredFields, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param declaredFields
   */
  @ConstructorProperties({"joinName", "pipe", "joinFields", "numSelfJoins", "declaredFields"})
  public Join( String joinName, Pipe pipe, Fields joinFields, int numSelfJoins, Fields declaredFields )
    {
    super( joinName, pipe, joinFields, numSelfJoins, declaredFields, null, null );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param joiner
   */
  @ConstructorProperties({"joinName", "pipe", "joinFields", "numSelfJoins", "joiner"})
  public Join( String joinName, Pipe pipe, Fields joinFields, int numSelfJoins, Joiner joiner )
    {
    super( joinName, pipe, joinFields, numSelfJoins, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param joinName
   * @param pipes
   * @param joinFields
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"joinName", "pipes", "joinFields", "declaredFields", "joiner"})
  public Join( String joinName, Pipe[] pipes, Fields[] joinFields, Fields declaredFields, Joiner joiner )
    {
    super( joinName, pipes, joinFields, declaredFields, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   */
  @ConstructorProperties({"lhs", "lhsJoinFields", "rhs", "rhsJoinFields"})
  public Join( Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields )
    {
    super( null, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), null, null );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param joiner
   */
  @ConstructorProperties({"lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "joiner"})
  public Join( Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Joiner joiner )
    {
    super( null, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), null, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param declaredFields
   */
  @ConstructorProperties({"lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "declaredFields"})
  public Join( Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Fields declaredFields )
    {
    super( null, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), declaredFields, null );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param lhs
   * @param lhsJoinFields
   * @param rhs
   * @param rhsJoinFields
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"lhs", "lhsJoinFields", "rhs", "rhsJoinFields", "declaredFields", "joiner"})
  public Join( Pipe lhs, Fields lhsJoinFields, Pipe rhs, Fields rhsJoinFields, Fields declaredFields, Joiner joiner )
    {
    super( null, Pipe.pipes( lhs, rhs ), Fields.fields( lhsJoinFields, rhsJoinFields ), declaredFields, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"pipe", "joinFields", "numSelfJoins", "declaredFields", "joiner"})
  public Join( Pipe pipe, Fields joinFields, int numSelfJoins, Fields declaredFields, Joiner joiner )
    {
    super( null, pipe, joinFields, numSelfJoins, declaredFields, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param declaredFields
   */
  @ConstructorProperties({"pipe", "joinFields", "numSelfJoins", "declaredFields"})
  public Join( Pipe pipe, Fields joinFields, int numSelfJoins, Fields declaredFields )
    {
    super( null, pipe, joinFields, numSelfJoins, declaredFields );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param pipe
   * @param joinFields
   * @param numSelfJoins
   * @param joiner
   */
  @ConstructorProperties({"pipe", "joinFields", "numSelfJoins", "joiner"})
  public Join( Pipe pipe, Fields joinFields, int numSelfJoins, Joiner joiner )
    {
    super( null, pipe, joinFields, numSelfJoins, null, joiner );
    }

  /**
   * Constructor Join creates a new Join instance.
   *
   * @param pipes
   * @param joinFields
   * @param declaredFields
   * @param joiner
   */
  @ConstructorProperties({"pipes", "joinFields", "declaredFields", "joiner"})
  public Join( Pipe[] pipes, Fields[] joinFields, Fields declaredFields, Joiner joiner )
    {
    super( null, pipes, joinFields, declaredFields, null, joiner );
    }
  }
