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
package de.tudarmstadt.ukp.inception.annotation.feature.misc;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.inception.support.text.NaturalStringComparator;

public class TagRanker
{
    private boolean tagCreationAllowed;
    private int maxResults;

    public List<ReorderableTag> rank(String aTerm, List<ReorderableTag> aTagSet)
    {
        List<ReorderableTag> matches = new ArrayList<>();

        if (isBlank(aTerm)) {
            List<ReorderableTag> availableTags = new LinkedList<>(aTagSet);

            ListIterator<ReorderableTag> tagIterator = availableTags.listIterator();
            while (tagIterator.hasNext()) {
                ReorderableTag t = tagIterator.next();

                if (t.getReordered()) {
                    matches.add(t);
                    tagIterator.remove();
                }
            }

            availableTags.sort(comparing(ReorderableTag::getName, new NaturalStringComparator()));

            availableTags.stream() //
                    .limit(Math.max(maxResults - matches.size(), 0)) //
                    .forEachOrdered(matches::add);
            return matches;
        }

        boolean exactMatchSeen = false;

        // Now go through the remaining tags and try ranking them sensibly
        List<Pair<ReorderableTag, Integer>> scoredTags = new ArrayList<>();
        for (ReorderableTag t : aTagSet) {
            if (!containsIgnoreCase(t.getName(), aTerm)) {
                if (t.getReordered()) {
                    scoredTags.add(Pair.of(t, 10));
                }
                continue;
            }

            if (!StringUtils.contains(t.getName(), aTerm)) {
                scoredTags.add(Pair.of(t, t.getReordered() ? 11 : 1));
                continue;
            }

            if (!startsWithIgnoreCase(t.getName(), aTerm)) {
                scoredTags.add(Pair.of(t, t.getReordered() ? 12 : 2));
                continue;
            }

            if (!startsWith(t.getName(), aTerm)) {
                scoredTags.add(Pair.of(t, t.getReordered() ? 13 : 3));
                continue;
            }

            if (!t.getName().equals(aTerm)) {
                scoredTags.add(Pair.of(t, t.getReordered() ? 14 : 4));
                continue;
            }

            scoredTags.add(Pair.of(t, 100));
            exactMatchSeen = true;
        }

        Comparator<Pair<ReorderableTag, Integer>> cmp = comparing(Pair::getValue, reverseOrder());
        cmp = cmp.thenComparing(p -> p.getKey().getName(), new NaturalStringComparator());
        Collections.sort(scoredTags, cmp);

        // If adding own tags is allowed, the always return the current input as the
        // first choice.
        if (tagCreationAllowed && !exactMatchSeen) {
            matches.add(0, new ReorderableTag(aTerm, "New unsaved tag..."));
        }

        scoredTags.stream().limit(Math.max(maxResults - matches.size(), 0)) //
                .map(Pair::getKey) //
                .forEachOrdered(matches::add);

        return matches;
    }

    public boolean isTagCreationAllowed()
    {
        return tagCreationAllowed;
    }

    public void setTagCreationAllowed(boolean aTagCreationAllowed)
    {
        tagCreationAllowed = aTagCreationAllowed;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int aMaxResults)
    {
        maxResults = aMaxResults;
    }
}
