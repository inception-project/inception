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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.RATER_WITHOUT_ANNOTATIONS;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Raters who have nothing that the comparison looks at in at least one of the documents included in
 * the calculation. The raters themselves are the finding, so - like
 * {@link SparseCategoriesDiagnostic} - an instance that stands for several comparisons names the
 * raters of all of them rather than those of the first one observed.
 * <p>
 * The observation only states what the data shows. Whether a rater has not started yet or there was
 * nothing to annotate cannot be told from the data, so the note does not guess.
 */
public class RaterWithoutAnnotationsDiagnostic
    extends AgreementDiagnostic
{
    private static final long serialVersionUID = 2217493186395061245L;

    /**
     * The raters (by UI name) without annotations to compare, sorted.
     */
    private final List<String> raters;

    /**
     * Per rater (by UI name), the number of documents without annotations to compare. For an
     * instance that stands for several comparisons, this is {@code null} - the numbers of different
     * comparisons are based on different documents and cannot be combined.
     */
    private final LinkedHashMap<String, Integer> emptyDocumentCounts;

    /**
     * Per rater (by UI name), the names of the documents without annotations to compare, sorted.
     * Unlike the counts, the names of several comparisons can be combined: whether a rater has
     * anything in a document does not depend on whom they are compared with.
     */
    private final LinkedHashMap<String, List<String>> emptyDocumentNames;

    /**
     * The number of documents the study is based on, or {@code null} for an instance that stands
     * for several comparisons - see {@link #emptyDocumentCounts}.
     */
    private final Integer documentCount;

    /**
     * @param aEmptyDocumentCounts
     *            per rater (by UI name), the number of documents in which they have no annotations
     *            to compare.
     * @param aDocumentCount
     *            the number of documents the study is based on.
     */
    public RaterWithoutAnnotationsDiagnostic(Map<String, Integer> aEmptyDocumentCounts,
            int aDocumentCount)
    {
        this(aEmptyDocumentCounts, Map.of(), aDocumentCount);
    }

    /**
     * @param aEmptyDocumentCounts
     *            per rater (by UI name), the number of documents in which they have no annotations
     *            to compare.
     * @param aEmptyDocumentNames
     *            per rater (by UI name), the names of those documents, as far as they are known.
     * @param aDocumentCount
     *            the number of documents the study is based on.
     */
    public RaterWithoutAnnotationsDiagnostic(Map<String, Integer> aEmptyDocumentCounts,
            Map<String, ? extends Collection<String>> aEmptyDocumentNames, int aDocumentCount)
    {
        this(new ArrayList<>(new TreeSet<>(aEmptyDocumentCounts.keySet())),
                new LinkedHashMap<>(new TreeMap<>(aEmptyDocumentCounts)),
                sortedNames(aEmptyDocumentNames), aDocumentCount);
    }

    private RaterWithoutAnnotationsDiagnostic(List<String> aRaters,
            LinkedHashMap<String, Integer> aEmptyDocumentCounts,
            LinkedHashMap<String, List<String>> aEmptyDocumentNames, Integer aDocumentCount)
    {
        // Not quantitative: the raters are the finding, so there is no value whose range would be
        // shown.
        super(RATER_WITHOUT_ANNOTATIONS,
                describe(aRaters, aEmptyDocumentCounts, aEmptyDocumentNames, aDocumentCount),
                Double.NaN, null);
        raters = aRaters;
        emptyDocumentCounts = aEmptyDocumentCounts;
        emptyDocumentNames = aEmptyDocumentNames;
        documentCount = aDocumentCount;
    }

    /**
     * Creates an instance that stands for several comparisons. It names the raters and documents of
     * all of them, but carries no counts.
     */
    private static RaterWithoutAnnotationsDiagnostic merged(Collection<String> aRaters,
            Map<String, ? extends Collection<String>> aEmptyDocumentNames)
    {
        return new RaterWithoutAnnotationsDiagnostic(new ArrayList<>(new TreeSet<>(aRaters)), null,
                sortedNames(aEmptyDocumentNames), null);
    }

    private static LinkedHashMap<String, List<String>> sortedNames(
            Map<String, ? extends Collection<String>> aNames)
    {
        var sorted = new LinkedHashMap<String, List<String>>();
        new TreeMap<>(aNames).forEach((rater, names) -> {
            if (!names.isEmpty()) {
                sorted.put(rater, new ArrayList<>(new TreeSet<>(names)));
            }
        });
        return sorted;
    }

    /**
     * @return the raters (by UI name) without annotations to compare, sorted.
     */
    public List<String> getRaters()
    {
        return unmodifiableList(raters);
    }

    /**
     * @param aRater
     *            the UI name of a rater.
     * @return the number of documents in which the rater has no annotations to compare, or nothing
     *         if the rater is not one of them or the instance stands for several comparisons.
     */
    public OptionalInt getEmptyDocumentCount(String aRater)
    {
        if (emptyDocumentCounts == null || !emptyDocumentCounts.containsKey(aRater)) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(emptyDocumentCounts.get(aRater));
    }

    /**
     * @param aRater
     *            the UI name of a rater.
     * @return the names of the documents in which the rater has no annotations to compare, sorted,
     *         as far as they are known.
     */
    public List<String> getEmptyDocumentNames(String aRater)
    {
        return unmodifiableList(emptyDocumentNames.getOrDefault(aRater, new ArrayList<>()));
    }

    /**
     * @return the number of documents the study is based on, or nothing for an instance that stands
     *         for several comparisons.
     */
    public OptionalInt getDocumentCount()
    {
        return documentCount != null ? OptionalInt.of(documentCount) : OptionalInt.empty();
    }

    @Override
    public AgreementDiagnostic mergeWith(AgreementDiagnostic aOther)
    {
        if (!(aOther instanceof RaterWithoutAnnotationsDiagnostic other)) {
            return this;
        }

        var allRaters = new TreeSet<>(raters);
        allRaters.addAll(other.raters);

        var names = new TreeMap<String, TreeSet<String>>();
        for (var side : List.of(emptyDocumentNames, other.emptyDocumentNames)) {
            side.forEach((rater, docs) -> names.computeIfAbsent(rater, _k -> new TreeSet<>())
                    .addAll(docs));
        }

        return merged(allRaters, names);
    }

    private static String describe(List<String> aRaters, Map<String, Integer> aEmptyDocumentCounts,
            Map<String, List<String>> aEmptyDocumentNames, Integer aDocumentCount)
    {
        var raters = aRaters.stream() //
                .map(rater -> describeRater(rater,
                        aEmptyDocumentCounts != null ? aEmptyDocumentCounts.get(rater) : null,
                        aEmptyDocumentNames.getOrDefault(rater, new ArrayList<>()), aDocumentCount)) //
                .toList();

        // A merged instance names the raters of several comparisons, and a rater without
        // annotations in one of them need not be without annotations in the others.
        return format("No annotations to compare from %s%s.",
                AgreementDiagnostics.enumerate(raters),
                aDocumentCount == null ? " in at least one comparison" : "");
    }

    private static String describeRater(String aRater, Integer aCount, List<String> aNames,
            Integer aDocumentCount)
    {
        // Merged instances carry no counts, but their names can still be listed
        if (aDocumentCount == null) {
            return aNames.isEmpty() ? aRater
                    : format("%s (in %s)", aRater,
                            AgreementDiagnostics.listDocuments(aNames, aNames.size()));
        }

        // With a single document, the note is about that document anyway
        if (aDocumentCount == 1) {
            return aRater;
        }

        if (aNames.isEmpty()) {
            return format("%s (in %d of %d documents)", aRater, aCount, aDocumentCount);
        }

        return format("%s (in %d of %d documents: %s)", aRater, aCount, aDocumentCount,
                AgreementDiagnostics.listDocuments(aNames, aCount));
    }
}
