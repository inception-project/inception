/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.kb;

import java.io.Serializable;
import java.util.Optional;

import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Traits for knowledge-base-related features.
 */
public class ConceptFeatureTraits
    implements Serializable
{
    private static final long serialVersionUID = 6303541487449965932L;

    private @Autowired KnowledgeBaseService kbService;

    private String repositoryId;
    private String scope;
    private ConceptFeatureValueType allowedValueType;

    public ConceptFeatureTraits()
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
        return allowedValueType != null ? allowedValueType : ConceptFeatureValueType.INSTANCE;
    }

    public void setAllowedValueType(ConceptFeatureValueType aAllowedType) {
        allowedValueType = aAllowedType;
    }

    public boolean isKBEnabled(Project aProject)
    {
        Optional<KnowledgeBase> kb = Optional.empty();
        String repositoryId = getRepositoryId();
        if (repositoryId != null) {
            kb = kbService.getKnowledgeBaseById(aProject, getRepositoryId());
        }
        return kb.isPresent() && kb.get().isEnabled() || repositoryId == null;
    }
}

