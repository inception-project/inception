/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.elastic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchResult
{
    private int took;
    private boolean timed_out;
    private ElasticSearchHits hits;

    public void setTook(int took)
    {
        this.took = took;
    }

    public boolean isTimed_out()
    {
        return timed_out;
    }

    public void setTimed_out(boolean timed_out)
    {
        this.timed_out = timed_out;
    }

    public ElasticSearchHits getHits()
    {
        return hits;
    }

    public void setHits(ElasticSearchHits hits)
    {
        this.hits = hits;
    }

    public String toString()
    {
        return String.format("{took: %d, timed_out: %s, hits: %s}", this.took, this.timed_out,
                this.hits);
    }

}
