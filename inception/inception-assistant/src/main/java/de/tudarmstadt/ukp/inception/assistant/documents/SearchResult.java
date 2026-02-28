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

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for search results containing the matched chunks and the total number of matches for the
 * query.
 */
public record SearchResult(List<Chunk> matches, OptionalInt totalMatches,
        Optional<Boolean> truncated)
{
    private static final SearchResult EMPTY_RESULT = builder() //
            .withTotalMatches(0) //
            .withTruncated(false) //
            .build();

    @JsonCreator
    public SearchResult(@JsonProperty("matches") List<Chunk> matches,
            @JsonProperty("totalMatches") OptionalInt totalMatches,
            @JsonProperty("truncated") Optional<Boolean> truncated)
    {
        this.matches = matches != null ? List.copyOf(matches) : List.of();
        this.totalMatches = totalMatches;
        this.truncated = truncated;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static SearchResult emptyResult()
    {
        return EMPTY_RESULT;
    }

    public static final class Builder
    {
        private List<Chunk> matches = emptyList();
        private Integer totalMatches = 0;
        private Boolean truncated = false;

        private Builder()
        {
        }

        public Builder withMatches(List<Chunk> aMatches)
        {
            matches = aMatches != null ? List.copyOf(aMatches) : List.of();
            return this;
        }

        public Builder withTotalMatches(Integer aTotal)
        {
            totalMatches = aTotal;
            return this;
        }

        public Builder withTruncated(Boolean aTruncated)
        {
            truncated = aTruncated;
            return this;
        }

        public SearchResult build()
        {
            return new SearchResult(matches,
                    totalMatches != null ? OptionalInt.of(totalMatches) : OptionalInt.empty(),
                    Optional.of(truncated));
        }
    }
}
