/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.apache.commons.collections4.IteratorUtils.unmodifiableIterator;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Container for {@link SuggestionGroup suggestion groups} all coming from a single document.
 * No guarantees about layers and features though.
 */
public class SuggestionDocumentGroup
    extends AbstractCollection<SuggestionGroup>
{
    private List<SuggestionGroup> groups;
    
    public SuggestionDocumentGroup()
    {
        groups = new ArrayList<>();
    }
    
    @Override
    public boolean add(SuggestionGroup aGroup)
    {
        if (!groups.isEmpty()) {
            SuggestionGroup representative = groups.get(0);
            
            Validate.isTrue(representative.getDocumentName().equals(aGroup.getDocumentName()),
                    "All suggestions in a group must come from the same document: expected [%s] but got [%s]",
                    representative.getDocumentName(), aGroup.getDocumentName());
        }
        
        return groups.add(aGroup);
    }

    @Override
    public Iterator<SuggestionGroup> iterator()
    {
        return unmodifiableIterator(groups.iterator());
    }

    @Override
    public int size()
    {
        return groups.size();
    }
    
    public String getDocumentName()
    {
        return groups.get(0).getDocumentName();
    }
}
