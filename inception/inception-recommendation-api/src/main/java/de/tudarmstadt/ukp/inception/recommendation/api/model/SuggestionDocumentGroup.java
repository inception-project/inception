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

import static org.apache.commons.collections4.IteratorUtils.unmodifiableIterator;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private long documentId;

    /**
     * Use {@ink #groupByType(List)} or {@link #groupsOfType(Class, List)} instead to ensure that
     * never empty groups are created.
     */
    private SuggestionDocumentGroup(List<T> aSuggestions)
    {
        groups = new ArrayList<>();
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
            Class<V> type, List<? extends AnnotationSuggestion> aSuggestions)
    {
        var filteredSuggestions = aSuggestions.stream().filter(type::isInstance) //
                .toList();
        return new SuggestionDocumentGroup<V>((List<V>) filteredSuggestions);
    }

    /**
     * @param aSuggestions
     *            the list to retrieve suggestions from
     * @return suggestions grouped by suggestion type. There will not be any empty groups in the
     *         result.
     */
    public static Map<Class<? extends AnnotationSuggestion>, SuggestionDocumentGroup<? extends AnnotationSuggestion>> //
            groupByType(List<AnnotationSuggestion> aSuggestions)
    {
        if (aSuggestions == null || aSuggestions.isEmpty()) {
            return Collections.emptyMap();
        }

        var groups = new LinkedHashMap<Class<? extends AnnotationSuggestion>, List<AnnotationSuggestion>>();

        for (var suggestion : aSuggestions) {
            var group = groups.computeIfAbsent(suggestion.getClass(), $ -> new ArrayList<>());
            group.add(suggestion);
        }

        var groupsMap = new LinkedHashMap<Class<? extends AnnotationSuggestion>, SuggestionDocumentGroup<?>>();
        for (var entry : groups.entrySet()) {
            groupsMap.put(entry.getKey(), new SuggestionDocumentGroup<>(entry.getValue()));
        }

        return groupsMap;
    }

    @Override
    public boolean add(SuggestionGroup<T> aGroup)
    {
        boolean empty = isEmpty();

        if (!empty) {
            Validate.isTrue(documentId == aGroup.getDocumentId(),
                    "All suggestions in a group must come from the same document: expected [%s] but got [%s]",
                    documentId, aGroup.getDocumentId());
        }

        // Cache information that must be consistent in the group when the first item is added
        if (empty) {
            documentId = aGroup.getDocumentId();
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

    public long getDocumentId()
    {
        return documentId;
    }

    @Override
    public boolean isEmpty()
    {
        return groups.isEmpty();
    }
}
