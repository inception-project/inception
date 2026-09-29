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
package de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

/**
 * The raw counts the diagnostics are derived from, accumulated across the documents of one study.
 * <p>
 * A study spans all the documents compared for one rater pair (or, in the per-document table, all
 * raters of one document), but the agreement is computed one document at a time. Diagnosing each
 * document on its own would let a single document speak for the whole study - a project of many
 * small documents would permanently look like it had too few items, and the quantities quoted in
 * the notes would be one document's rather than the study's. So the counts are tallied here as the
 * documents are merged, and the thresholds are applied once to the totals.
 * <p>
 * The score of a study is the average of its per-document scores, though, and pooling hides
 * characteristics that distort a document's score but cancel out across documents - two documents
 * that each use only one label, but a different one, pool to a balanced distribution. So the tally
 * also records which of the scored documents exhibit such a characteristic on their own, by name,
 * so that the notes can point at them.
 */
public class AgreementDataTally
    implements Serializable
{
    private static final long serialVersionUID = 8213675204118327590L;

    private final Map<Object, Integer> categoryCounts = new LinkedHashMap<>();

    /**
     * Per category, the number of items on which at least one rater used it. Unlike
     * {@link #categoryCounts}, this does not grow with the number of raters: a label that five
     * raters put on the same single item is still evidence from one item only.
     */
    private final Map<Object, Integer> categoryItemCounts = new LinkedHashMap<>();

    /**
     * Category counts per rater, keyed by the rater index within the study.
     * <p>
     * Accumulating these across documents assumes index N denotes the same rater in every document
     * added to this tally. That holds for the pairwise calculation, which builds a fresh two-entry
     * map per document in a fixed rater order. It would <em>not</em> hold for a caller that builds
     * the rater map from whichever annotation documents happen to exist, because a rater missing
     * from one document shifts every later index - such a caller must tally each document
     * separately rather than merging them here.
     */
    private final Map<Integer, Map<Object, Integer>> perRaterCategoryCounts = new LinkedHashMap<>();

    private int itemCount;
    private int assignmentCount;
    private int relevantPositionCount;
    private int incompletePositionCount;

    /**
     * Items that were part of the study but did not count towards the score because only one rater
     * gave them a value and the measure does not score such items.
     */
    private int unscoredItemCount;

    private int documentCount;

    /**
     * Per rater (by CAS group id), the number of documents in which the rater has no relevant
     * position, i.e. nothing that the comparison looks at.
     */
    private final Map<String, Integer> emptyDocumentCounts = new LinkedHashMap<>();

    /**
     * Per rater (by CAS group id), the names of the documents counted in
     * {@link #emptyDocumentCounts}. A document added without a name is counted there, but not named
     * here.
     */
    private final Map<String, SortedSet<String>> emptyDocumentNames = new LinkedHashMap<>();

    /**
     * Documents that produced a score, i.e. those the average score of the study is taken over.
     */
    private int scoredDocumentCount;

    /**
     * Per characteristic, the number of scored documents that exhibit it on their own.
     */
    private final Map<AgreementDiagnosticType, Integer> affectedDocumentCounts = new EnumMap<>(
            AgreementDiagnosticType.class);

    /**
     * Per characteristic, the names of the documents counted in {@link #affectedDocumentCounts}. A
     * document added without a name is counted there, but not named here.
     */
    private final Map<AgreementDiagnosticType, SortedSet<String>> affectedDocumentNames = //
            new EnumMap<>(AgreementDiagnosticType.class);

    /**
     * Adds the label distribution of one document's study to this tally, counting every item in the
     * study.
     *
     * @param aStudy
     *            the study computed for a single document.
     */
    public void add(ICodingAnnotationStudy aStudy)
    {
        add(aStudy, true);
    }

    /**
     * Adds the label distribution of one document's study to this tally.
     *
     * @param aStudy
     *            the study computed for a single document.
     * @param aScoringSingleValueItems
     *            whether the measure scores items on which only one rater gave a value. If it does
     *            not, such items are left out of the label statistics, because the label
     *            distribution the notes talk about must be the one the score is based on. They are
     *            still counted as {@link #getUnscoredItemCount() unscored}.
     */
    public void add(ICodingAnnotationStudy aStudy, boolean aScoringSingleValueItems)
    {
        if (aStudy == null) {
            return;
        }

        for (var item : aStudy.getItems()) {
            // The rater count of an item is the number of raters that gave it a value
            if (!aScoringSingleValueItems && item.getRaterCount() < 2) {
                unscoredItemCount++;
                continue;
            }

            itemCount++;
            var itemCategories = new HashSet<Object>();

            for (var unit : item.getUnits()) {
                var category = unit.getCategory();

                // A null category means the rater did not annotate the position at all - which is
                // only part of the study when incomplete positions are included. It is not a label
                // anybody used, so it does not participate in the label statistics. A label that
                // was left unset is not null here, but the empty label, which is a category.
                if (category == null) {
                    continue;
                }

                itemCategories.add(category);
                categoryCounts.merge(category, 1, Integer::sum);
                perRaterCategoryCounts
                        .computeIfAbsent(unit.getRaterIdx(), _k -> new LinkedHashMap<>())
                        .merge(category, 1, Integer::sum);
                assignmentCount++;
            }

            for (var category : itemCategories) {
                categoryItemCounts.merge(category, 1, Integer::sum);
            }
        }
    }

    /**
     * Adds one document's position counts to this tally.
     *
     * @param aRelevantPositions
     *            the positions considered for agreement in that document.
     * @param aIncompletePositions
     *            those of them that only some of the raters annotated.
     */
    public void addPositions(int aRelevantPositions, int aIncompletePositions)
    {
        relevantPositionCount += aRelevantPositions;
        incompletePositionCount += aIncompletePositions;
    }

    /**
     * Records that one more document was added to this tally and which of its raters have nothing
     * in it that the comparison looks at.
     *
     * @param aRatersWithoutPositions
     *            the CAS group ids of the raters without any relevant position in the document.
     */
    public void addDocument(Collection<String> aRatersWithoutPositions)
    {
        addDocument(null, aRatersWithoutPositions);
    }

    /**
     * Records that one more document was added to this tally and which of its raters have nothing
     * in it that the comparison looks at.
     *
     * @param aDocumentName
     *            the name of the document, or {@code null} if it is not known.
     * @param aRatersWithoutPositions
     *            the CAS group ids of the raters without any relevant position in the document.
     */
    public void addDocument(String aDocumentName, Collection<String> aRatersWithoutPositions)
    {
        documentCount++;
        for (var rater : aRatersWithoutPositions) {
            emptyDocumentCounts.merge(rater, 1, Integer::sum);
            if (aDocumentName != null) {
                emptyDocumentNames.computeIfAbsent(rater, _k -> new TreeSet<>()).add(aDocumentName);
            }
        }
    }

    /**
     * Records that one more document produced a score and which characteristics it exhibits on its
     * own.
     *
     * @param aDocumentName
     *            the name of the document, or {@code null} if it is not known.
     * @param aCharacteristics
     *            the characteristics observed on the document alone.
     */
    public void addScoredDocument(String aDocumentName,
            Set<AgreementDiagnosticType> aCharacteristics)
    {
        scoredDocumentCount++;
        for (var type : aCharacteristics) {
            affectedDocumentCounts.merge(type, 1, Integer::sum);
            if (aDocumentName != null) {
                affectedDocumentNames.computeIfAbsent(type, _k -> new TreeSet<>())
                        .add(aDocumentName);
            }
        }
    }

    /**
     * Folds another tally into this one, so that merging two summaries also merges what their
     * diagnostics are based on.
     *
     * @param aOther
     *            the tally to absorb.
     */
    public void add(AgreementDataTally aOther)
    {
        if (aOther == null) {
            return;
        }

        itemCount += aOther.itemCount;
        unscoredItemCount += aOther.unscoredItemCount;
        documentCount += aOther.documentCount;
        scoredDocumentCount += aOther.scoredDocumentCount;
        assignmentCount += aOther.assignmentCount;
        relevantPositionCount += aOther.relevantPositionCount;
        incompletePositionCount += aOther.incompletePositionCount;

        for (var e : aOther.categoryCounts.entrySet()) {
            categoryCounts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        for (var e : aOther.emptyDocumentCounts.entrySet()) {
            emptyDocumentCounts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        for (var e : aOther.emptyDocumentNames.entrySet()) {
            emptyDocumentNames.computeIfAbsent(e.getKey(), _k -> new TreeSet<>())
                    .addAll(e.getValue());
        }

        for (var e : aOther.affectedDocumentCounts.entrySet()) {
            affectedDocumentCounts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        for (var e : aOther.affectedDocumentNames.entrySet()) {
            affectedDocumentNames.computeIfAbsent(e.getKey(), _k -> new TreeSet<>())
                    .addAll(e.getValue());
        }

        for (var e : aOther.categoryItemCounts.entrySet()) {
            categoryItemCounts.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        for (var e : aOther.perRaterCategoryCounts.entrySet()) {
            var target = perRaterCategoryCounts.computeIfAbsent(e.getKey(),
                    _k -> new LinkedHashMap<>());
            for (var c : e.getValue().entrySet()) {
                target.merge(c.getKey(), c.getValue(), Integer::sum);
            }
        }
    }

    public Map<Object, Integer> getCategoryCounts()
    {
        return Collections.unmodifiableMap(categoryCounts);
    }

    /**
     * @return per category, the number of items on which at least one rater used it.
     */
    public Map<Object, Integer> getCategoryItemCounts()
    {
        return Collections.unmodifiableMap(categoryItemCounts);
    }

    public Map<Integer, Map<Object, Integer>> getPerRaterCategoryCounts()
    {
        return Collections.unmodifiableMap(perRaterCategoryCounts);
    }

    public int getItemCount()
    {
        return itemCount;
    }

    /**
     * @return the number of items in the study that did not count towards the score because only
     *         one rater gave them a value and the measure does not score such items. These items
     *         are not included in {@link #getItemCount()}.
     */
    public int getUnscoredItemCount()
    {
        return unscoredItemCount;
    }

    /**
     * @return the number of documents added to this tally.
     */
    public int getDocumentCount()
    {
        return documentCount;
    }

    /**
     * @return per rater (by CAS group id), the number of documents in which the rater has nothing
     *         that the comparison looks at. Raters who have something in every document are not
     *         included.
     */
    public Map<String, Integer> getEmptyDocumentCounts()
    {
        return Collections.unmodifiableMap(emptyDocumentCounts);
    }

    /**
     * @param aRater
     *            the CAS group id of a rater.
     * @return the names of the documents in which the rater has nothing that the comparison looks
     *         at, sorted. Documents added without a name are missing here.
     */
    public SortedSet<String> getEmptyDocumentNames(String aRater)
    {
        return Collections.unmodifiableSortedSet(
                emptyDocumentNames.getOrDefault(aRater, Collections.emptySortedSet()));
    }

    /**
     * @return the number of documents that produced a score.
     */
    public int getScoredDocumentCount()
    {
        return scoredDocumentCount;
    }

    /**
     * @param aType
     *            a characteristic.
     * @return the number of scored documents that exhibit the characteristic on their own.
     */
    public int getAffectedDocumentCount(AgreementDiagnosticType aType)
    {
        return affectedDocumentCounts.getOrDefault(aType, 0);
    }

    /**
     * @param aType
     *            a characteristic.
     * @return the names of the scored documents that exhibit the characteristic on their own,
     *         sorted. Documents added without a name are missing here.
     */
    public SortedSet<String> getAffectedDocumentNames(AgreementDiagnosticType aType)
    {
        return Collections.unmodifiableSortedSet(
                affectedDocumentNames.getOrDefault(aType, Collections.emptySortedSet()));
    }

    public int getAssignmentCount()
    {
        return assignmentCount;
    }

    public int getRelevantPositionCount()
    {
        return relevantPositionCount;
    }

    public int getIncompletePositionCount()
    {
        return incompletePositionCount;
    }

    public boolean isEmpty()
    {
        return assignmentCount == 0;
    }
}
