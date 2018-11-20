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

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchHits
{
    private int total;
    private float max_score;
    private ArrayList<ElasticSearchHit> hits;

    public int getTotal()
    {
        return total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public float getMax_score()
    {
        return max_score;
    }

    public void setMax_score(float max_score)
    {
        this.max_score = max_score;
    }

    public ArrayList<ElasticSearchHit> getHits()
    {
        return hits;
    }

    public void setHits(ArrayList<ElasticSearchHit> hits)
    {
        this.hits = hits;
    }

    public String toString()
    {
        return String.format("{total: %d, max_score: %f, hits: %s}", this.total, this.max_score,
                this.hits);
    }

}
