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
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBinding;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingTrait;

/**
 * Traits for knowledge-base-related features.
 */
public class ConceptFeatureTraits
    implements Serializable, KeyBindingTrait
{
    private static final long serialVersionUID = 6303541487449965932L;

    private String repositoryId;
    private String scope;
    private ConceptFeatureValueType allowedValueType;
    private List<KeyBinding> keyBindings = new ArrayList<>();

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

    @Override
    public List<KeyBinding> getKeyBindings()
    {
        return keyBindings;
    }

    @Override
    public void setKeyBindings(List<KeyBinding> aKeyBindings)
    {
        if (aKeyBindings == null) {
            keyBindings = new ArrayList<>();
        }
        else {
            keyBindings = aKeyBindings;
        }
    }
}

