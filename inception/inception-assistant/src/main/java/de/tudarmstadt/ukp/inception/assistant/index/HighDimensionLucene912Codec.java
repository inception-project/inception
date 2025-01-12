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
package de.tudarmstadt.ukp.inception.assistant.index;

import java.io.IOException;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.KnnVectorsReader;
import org.apache.lucene.codecs.KnnVectorsWriter;
import org.apache.lucene.codecs.lucene912.Lucene912Codec;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

public class HighDimensionLucene912Codec
    extends Lucene912Codec
{
    private final KnnVectorsFormat defaultKnnVectorsFormat;

    public HighDimensionLucene912Codec(int aDimension)
    {
        var knnFormat = new Lucene99HnswVectorsFormat();
        defaultKnnVectorsFormat = new HighDimensionKnnVectorsFormat(knnFormat, aDimension);
    }

    @Override
    public KnnVectorsFormat getKnnVectorsFormatForField(String field)
    {
        return defaultKnnVectorsFormat;
    }

    private static class HighDimensionKnnVectorsFormat
        extends KnnVectorsFormat
    {
        private final KnnVectorsFormat knnFormat;
        private final int maxDimensions;

        public HighDimensionKnnVectorsFormat(KnnVectorsFormat aKnnFormat, int aMaxDimensions)
        {
            super(aKnnFormat.getName());
            knnFormat = aKnnFormat;
            maxDimensions = aMaxDimensions;
        }

        @Override
        public KnnVectorsWriter fieldsWriter(SegmentWriteState aState) throws IOException
        {
            return knnFormat.fieldsWriter(aState);
        }

        @Override
        public KnnVectorsReader fieldsReader(SegmentReadState aState) throws IOException
        {
            return knnFormat.fieldsReader(aState);
        }

        @Override
        public int getMaxDimensions(String fieldName)
        {
            return maxDimensions;
        }
    }
}
