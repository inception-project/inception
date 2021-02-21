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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import static java.util.Comparator.comparing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SentenceOverview
    implements Serializable
{
    private static final long serialVersionUID = -6632707037285383353L;

    private Map<Integer, UnitState> sentenceBeginIndex = new HashMap<>();

    public List<UnitState> getSentenceInfos()
    {
        return sentenceBeginIndex.values().stream() //
                .sorted(comparing(UnitState::getBegin)) //
                .collect(Collectors.toList());
    }

    public void addSentenceInfo(UnitState aSegment)
    {
        sentenceBeginIndex.put(aSegment.getBegin(), aSegment);
    }
}
