/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import java.util.Iterator;
import java.util.List;

import opennlp.tools.namefind.NameSample;
import opennlp.tools.util.ObjectStream;

class NameSampleStream
    implements ObjectStream<NameSample>, AutoCloseable
{
    private List<NameSample> samples;
    private Iterator<NameSample> iterator;

    public NameSampleStream(List<NameSample> aSamples)
    {
        samples = aSamples;
        iterator = samples.iterator();
    }

    @Override
    public NameSample read()
    {
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    @Override
    public void reset() throws UnsupportedOperationException
    {
        iterator = samples.iterator();
    }

    @Override
    public void close()
    {
        samples = null;
        iterator = null;
    }
}
