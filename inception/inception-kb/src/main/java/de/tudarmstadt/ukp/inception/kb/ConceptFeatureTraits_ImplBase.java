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
package de.tudarmstadt.ukp.inception.kb;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Traits for knowledge-base-related features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ConceptFeatureTraits_ImplBase
    implements Serializable
{
    private static final long serialVersionUID = 3351053348043107018L;

    private String repositoryId;
    private String scope;
    private ConceptFeatureValueType allowedValueType;
    private boolean retainSuggestionInfo = false;

    public ConceptFeatureTraits_ImplBase()
    {
        // Nothing to do
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(String aKnowledgeBaseId)
    {
        repositoryId = aKnowledgeBaseId;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String aScope)
    {
        scope = aScope;
    }

    public ConceptFeatureValueType getAllowedValueType()
    {
        return allowedValueType != null ? allowedValueType : ConceptFeatureValueType.ANY_OBJECT;
    }

    public void setAllowedValueType(ConceptFeatureValueType aAllowedType)
    {
        allowedValueType = aAllowedType;
    }

    public boolean isRetainSuggestionInfo()
    {
        return retainSuggestionInfo;
    }

    public void setRetainSuggestionInfo(boolean aRetainSuggestionInfo)
    {
        retainSuggestionInfo = aRetainSuggestionInfo;
    }
}
