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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Example:
 * 
 * <pre>
 * <code>
 * {
 *   "entity_group": "PER",
 *   "score": 0.9992707967758179,
 *   "word": "John Smith",
 *   "start": 0,
 *   "end": 10
 * }
 * </code>
 * </pre>
 */
@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class HfEntityGroup
{
    private @JsonProperty("entity_group") String entityGroup;
    private @JsonProperty("score") double score;
    private @JsonProperty("word") String word;
    private @JsonProperty("start") int start;
    private @JsonProperty("end") int end;

    public String getEntityGroup()
    {
        return entityGroup;
    }

    public void setEntityGroup(String aEntityGroup)
    {
        entityGroup = aEntityGroup;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    public String getWord()
    {
        return word;
    }

    public void setWord(String aWord)
    {
        word = aWord;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart(int aStart)
    {
        start = aStart;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
    }
}
