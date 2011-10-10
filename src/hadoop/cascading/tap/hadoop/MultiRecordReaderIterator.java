/*
 * Copyright (c) 2007-2011 Concurrent, Inc. All Rights Reserved.
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

package cascading.tap.hadoop;

import java.io.IOException;

import cascading.tap.Tap;
import cascading.tap.TapException;
import cascading.tuple.CloseableIterator;
import cascading.tuple.Tuple;
import cascading.tuple.TupleIterator;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class TapIterator is an implementation of {@link TupleIterator}. It is returned by {@link cascading.tap.Tap} instances when
 * opening the taps resource for reading.
 */
public class MultiRecordReaderIterator implements CloseableIterator<RecordReader>
  {
  /** Field LOG */
  private static final Logger LOG = LoggerFactory.getLogger( MultiRecordReaderIterator.class );

  /** Field tap */
  private final Tap tap;
  /** Field inputFormat */
  private InputFormat inputFormat;
  /** Field conf */
  private final JobConf conf;
  /** Field splits */
  private InputSplit[] splits;
  /** Field reader */
  private RecordReader reader;

  /** Field lastReader */
  private RecordReader lastReader;

  /** Field currentSplit */
  private int currentSplit = 0;
  /** Field complete */
  private boolean complete = false;

  /**
   * Constructor TapIterator creates a new TapIterator instance.
   *
   * @throws IOException when
   */
  public MultiRecordReaderIterator( Tap tap, JobConf jobConf ) throws IOException
    {
    this.tap = tap;
    this.conf = new JobConf( jobConf );

    initialize();
    }

  private void initialize() throws IOException
    {
    tap.sourceConfInit( null, conf );

    if( !tap.resourceExists( conf ) )
      {
      complete = true;
      return;
      }

    inputFormat = conf.getInputFormat();

    if( inputFormat instanceof JobConfigurable )
      ( (JobConfigurable) inputFormat ).configure( conf );

    splits = inputFormat.getSplits( conf, 1 );

    if( splits.length == 0 )
      {
      complete = true;
      return;
      }
    }

  private RecordReader makeReader( int currentSplit ) throws IOException
    {
    LOG.debug( "reading split: {}", currentSplit );

    return inputFormat.getRecordReader( splits[ currentSplit ], conf, Reporter.NULL );
    }

  /**
   * Method hasNext returns true if there more {@link Tuple} instances available.
   *
   * @return boolean
   */
  public boolean hasNext()
    {
    getNextReader();

    return !complete;
    }

  /**
   * Method next returns the next {@link Tuple}.
   *
   * @return Tuple
   */
  public RecordReader next()
    {
    if( complete )
      throw new IllegalStateException( "no more values" );

    try
      {
      getNextReader();

      return reader;
      }
    finally
      {
      reader = null;
      }
    }

  private void getNextReader()
    {
    if( complete || reader != null )
      return;

    try
      {
      if( currentSplit < splits.length )
        {
        if( lastReader != null )
          lastReader.close();

        reader = makeReader( currentSplit++ );
        lastReader = reader;
        }
      else
        {
        complete = true;
        }
      }
    catch( IOException exception )
      {
      throw new TapException( "could not get next tuple", exception );
      }
    }

  public void remove()
    {
    throw new UnsupportedOperationException( "unimplemented" );
    }

  @Override
  public void close() throws IOException
    {
    if( lastReader != null )
      lastReader.close();
    }
  }