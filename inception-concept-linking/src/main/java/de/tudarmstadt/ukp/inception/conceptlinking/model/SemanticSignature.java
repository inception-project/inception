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
package de.tudarmstadt.ukp.inception.conceptlinking.model;

import java.util.Set;

/**
 * Captures the directly related entities and related relations of a given entity A. An entity B is
 * considered related to A iff A has B as an attribute in property P, or if B has A as an attribute
 * in property P.
 */
public class SemanticSignature
{
    private Set<String> relatedRelations;
    private Set<String> relatedEntities;

    public SemanticSignature(Set<String> relatedEntities, Set<String> relatedRelations)
    {
        this.relatedEntities = relatedEntities;
        this.relatedRelations = relatedRelations;
    }

    public Set<String> getRelatedRelations()
    {
        return relatedRelations;
    }

    public void setRelatedRelations(Set<String> relatedRelations)
    {
        this.relatedRelations = relatedRelations;
    }

    public Set<String> getRelatedEntities()
    {
        return relatedEntities;
    }

    public void setRelatedEntities(Set<String> relatedEntities)
    {
        this.relatedEntities = relatedEntities;
    }
}
