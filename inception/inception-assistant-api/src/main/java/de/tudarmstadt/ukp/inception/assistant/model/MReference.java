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
package de.tudarmstadt.ukp.inception.assistant.model;

public record MReference(String id, long documentId, String documentName, int begin, int end,
        double score)
{
    private MReference(Builder builder)
    {
        this(builder.id, builder.documentId, builder.documentName, builder.begin, builder.end,
                builder.score);
    }

    public String toString()
    {
        return "{{ref::" + id + "}}";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String id;
        private long documentId;
        private String documentName;
        private int begin;
        private int end;
        private double score;

        private Builder()
        {
        }

        public Builder withId(String aId)
        {
            id = aId;
            return this;
        }

        public Builder withDocumentId(long aDocumentId)
        {
            documentId = aDocumentId;
            return this;
        }

        public Builder withDocumentName(String aDocumentName)
        {
            documentName = aDocumentName;
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

        public Builder withScore(double aScore)
        {
            score = aScore;
            return this;
        }

        public MReference build()
        {
            return new MReference(this);
        }
    }
}
