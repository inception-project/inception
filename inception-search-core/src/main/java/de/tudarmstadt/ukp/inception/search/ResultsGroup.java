/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search;

import java.io.Serializable;
import java.util.List;

public class ResultsGroup
    implements Serializable
{
    private static final long serialVersionUID = -4448435773623997560L;
    
    private final String groupKey;
    private final List<SearchResult> results;

    public ResultsGroup(String aGroupKey, List<SearchResult> aResults)
    {
        groupKey = aGroupKey;
        results = aResults;
    }

    public String getGroupKey()
    {
        return groupKey;
    }

    public List<SearchResult> getResults()
    {
        return results;
    }
}
