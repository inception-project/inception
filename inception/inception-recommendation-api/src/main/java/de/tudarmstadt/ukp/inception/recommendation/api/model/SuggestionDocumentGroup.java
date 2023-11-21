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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.IteratorUtils.unmodifiableIterator;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Container for {@link SuggestionGroup suggestion groups} all coming from a single document. No
 * guarantees about layers and features though.
 * 
 * @param <T>
 *            the suggestion type
 */
public class SuggestionDocumentGroup<T extends AnnotationSuggestion>
    extends AbstractCollection<SuggestionGroup<T>>
{
    private Collection<SuggestionGroup<T>> groups;
    private String documentName;

    public SuggestionDocumentGroup()
    {
        groups = new ArrayList<>();
    }

    public SuggestionDocumentGroup(List<T> aSuggestions)
    {
        this();
        addAll(SuggestionGroup.group(aSuggestions));
    }

    /**
     * @param type
     *            the type of suggestions to retrieve
     * @param aSuggestions
     *            the list to retrieve suggestions from
     * @param <V>
     *            the suggestion type
     * @return a SuggestionDocumentGroup where only suggestions of type V are added
     */
    @SuppressWarnings("unchecked")
    public static <V extends AnnotationSuggestion> SuggestionDocumentGroup<V> groupsOfType(
            Class<V> type, List<AnnotationSuggestion> aSuggestions)
    {
        List<AnnotationSuggestion> filteredSuggestions = aSuggestions.stream()
                .filter(type::isInstance) //
                .collect(toList());
        return new SuggestionDocumentGroup<V>((List<V>) filteredSuggestions);
    }

    @Override
    public boolean add(SuggestionGroup<T> aGroup)
    {
        boolean empty = isEmpty();

        if (!empty) {
            Validate.isTrue(documentName.equals(aGroup.getDocumentName()),
                    "All suggestions in a group must come from the same document: expected [%s] but got [%s]",
                    documentName, aGroup.getDocumentName());
        }

        // Cache information that must be consistent in the group when the first item is added
        if (empty) {
            documentName = aGroup.getDocumentName();
        }

        return groups.add(aGroup);
    }

    @Override
    public Iterator<SuggestionGroup<T>> iterator()
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
        return documentName;
    }

    @Override
    public boolean isEmpty()
    {
        return groups.isEmpty();
    }
}
