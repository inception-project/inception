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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.inception.schema.api.feature.RecommendableFeatureTrait;

/**
 * Traits for multi-value input field text features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiValueStringFeatureTraits
    implements Serializable, RecommendableFeatureTrait
{
    private static final long serialVersionUID = -8450181605003189055L;

    private boolean retainSuggestionInfo = false;
    private @JsonInclude(NON_EMPTY) List<PermissionLevel> rolesSeeingSuggestionInfo = new ArrayList<>();

    public MultiValueStringFeatureTraits()
    {
        // Nothing to do
    }

    @Override
    public boolean isRetainSuggestionInfo()
    {
        return retainSuggestionInfo;
    }

    @Override
    public void setRetainSuggestionInfo(boolean aRetainSuggestionInfo)
    {
        retainSuggestionInfo = aRetainSuggestionInfo;
    }

    @Override
    public void setRolesSeeingSuggestionInfo(List<PermissionLevel> aRolesSeeingSuggestionInfo)
    {
        if (aRolesSeeingSuggestionInfo == null) {
            rolesSeeingSuggestionInfo = new ArrayList<>();
        }
        else {
            rolesSeeingSuggestionInfo = aRolesSeeingSuggestionInfo;
        }
    }

    @Override
    public List<PermissionLevel> getRolesSeeingSuggestionInfo()
    {
        return rolesSeeingSuggestionInfo;
    }
}
