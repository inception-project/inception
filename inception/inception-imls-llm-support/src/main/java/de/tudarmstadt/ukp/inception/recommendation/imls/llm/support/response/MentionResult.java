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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MentionResult
{
    public static final String PROP_MENTIONS = "mentions";

    private final @JsonProperty(value = PROP_MENTIONS, required = true) List<Mention> mentions;

    @JsonCreator
    public MentionResult(@JsonProperty(PROP_MENTIONS) List<Mention> aMentions)
    {
        if (aMentions != null) {
            mentions = unmodifiableList(new ArrayList<>(aMentions));
        }
        else {
            mentions = emptyList();
        }
    }

    public List<Mention> getMentions()
    {
        return mentions;
    }
}
