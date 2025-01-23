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
package de.tudarmstadt.ukp.inception.assistant.documents;

import java.util.Objects;

public record Chunk(long documentId, String documentName, String section, String text, int begin,
        int end, double score)
{

    private Chunk(Builder builder)
    {
        this(builder.documentId, builder.documentName, builder.section, builder.text, builder.begin,
                builder.end, builder.score);
    }

    public Chunk merge(Chunk aChunk)
    {
        assert documentId == aChunk.documentId;
        assert Objects.equals(documentName, aChunk.documentName);
        assert Objects.equals(section, aChunk.section);

        var mText = text + "\n" + aChunk.text;
        var mScore = (score + aChunk.score) / 2.0;
        var mBegin = Math.min(begin, aChunk.begin);
        var mEnd = Math.max(end, aChunk.end);

        return new Chunk(documentId, documentName, section, mText, mBegin, mEnd, mScore);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private long documentId;
        private String documentName;
        private String section;
        private String text;
        private int begin;
        private int end;
        private double score = Double.NaN;

        private Builder()
        {
        }

        public Builder withSection(String aSection)
        {
            section = aSection;
            return this;
        }

        public Builder withText(String aText)
        {
            text = aText;
            return this;
        }

        public Chunk build()
        {
            return new Chunk(this);
        }

        public Builder withScore(double aScore)
        {
            score = aScore;
            return this;
        }

        public Builder withDocumentId(long aId)
        {
            documentId = aId;
            return this;
        }

        public Builder withDocumentName(String aName)
        {
            documentName = aName;
            return this;
        }

        public Builder withBegin(int aBegin)
        {
            begin = aBegin;
            return this;
        }

        public Builder withEnd(int aEnd)
        {
            end = aEnd;
            return this;
        }
    }
}
