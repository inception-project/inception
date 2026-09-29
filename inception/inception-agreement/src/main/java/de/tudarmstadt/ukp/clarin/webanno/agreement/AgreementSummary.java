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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import static java.util.Collections.unmodifiableMap;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.DoubleStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDataTally;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.aligning.FullAligningAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class AgreementSummary
    implements Serializable
{
    private static final long serialVersionUID = -6827325051466418904L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String type;
    private final String feature;
    private final Set<String> casGroupIds = new HashSet<>();

    private final List<Double> agreements = new ArrayList<>();
    private final Set<Object> categories = new LinkedHashSet<>();
    private final Map<String, Long> itemCounts = new HashMap<>();
    private final Map<String, Long> nonNullContentCounts = new HashMap<>();
    private final Map<String, Boolean> allNull = new HashMap<>();
    /**
     * Number of relevant positions at which each rater annotated anything. Unlike {@link #allNull},
     * which is derived from the study, this also counts positions that the study excludes as
     * incomplete - so it tells a rater who annotated nothing apart from raters who annotated at
     * different positions.
     */
    private final Map<String, Long> annotatedPositionCounts = new HashMap<>();
    /**
     * Number of documents on which the comparison was skipped, by reason.
     */
    private final Map<Skip, Long> skips = new LinkedHashMap<>();

    /**
     * Data characteristics observed for this study. They are evaluated once on the counts
     * accumulated across all merged documents (see {@link #analyzeMerged}), so each entry carries a
     * count of one here. How widespread a characteristic is across rater pairs or documents is
     * determined by the enclosing result, which counts the comparisons exhibiting it.
     */
    private final Map<AgreementDiagnostic, Integer> diagnostics = new LinkedHashMap<>();

    /**
     * The raw counts the diagnostics are derived from. Accumulated as documents are merged so that
     * the thresholds can be applied to the study as a whole rather than to any single document.
     * Which documents exhibit a characteristic on their own is recorded as well, so that notes can
     * point at the documents that distort the average score.
     * <p>
     * Only needed until {@link #analyzeMerged} has turned it into {@link #diagnostics}, after which
     * it is dropped: the summary is kept with the result shown on the page, and the tally grows
     * with the number of distinct labels, which for a free-text feature can be large.
     */
    private AgreementDataTally dataTally = new AgreementDataTally();

    private boolean empty;

    /**
     * Whether the computation that produced this summary excluded incomplete sets. This is the
     * setting the measure actually used, which need not match the traits held by the UI - measures
     * that cannot score incomplete sets hard-code it regardless of the trait.
     * <p>
     * {@code null} until a computation reports one. Summaries that never scored anything - see
     * {@link #skipped} - must not claim a setting they never used, because merging one into a study
     * would otherwise decide the study's setting on behalf of the computations that did run.
     */
    private Boolean excludeIncomplete;

    private int incompleteSetsByPosition;
    private int incompleteSetsByLabel;
    private int pluralitySets;
    private int relevantSetCount;
    private int completeSetCount;
    private int usedSetCount;

    /**
     * Sets that were part of the study but that the measure did not score, because only one rater
     * gave them a value and the measure cannot pair such a value with anything. These are only ever
     * incomplete sets that were not excluded - excluded sets never reach the study.
     */
    private int unscoredSetCount;

    public void merge(AgreementSummary aResult)
    {
        if (!type.equals(aResult.type)) {
            throw new IllegalArgumentException("All merged results must have the same type [" + type
                    + "] but encountered [" + aResult.type + "]");
        }

        if (!Objects.equals(feature, aResult.feature)) {
            throw new IllegalArgumentException("All merged results must have the same feature ["
                    + feature + "] but encountered [" + aResult.feature + "]");
        }

        // A summary that scored nothing carries no setting, so the one from the side that actually
        // computed something wins. Within a run every computation uses the same measure and traits,
        // so two differing settings cannot arise here.
        if (excludeIncomplete == null) {
            excludeIncomplete = aResult.excludeIncomplete;
        }

        casGroupIds.addAll(aResult.casGroupIds);
        agreements.addAll(aResult.agreements);
        categories.addAll(aResult.categories);

        for (var e : aResult.itemCounts.entrySet()) {
            itemCounts.merge(e.getKey(), e.getValue(), Long::sum);
        }

        for (var e : aResult.nonNullContentCounts.entrySet()) {
            nonNullContentCounts.merge(e.getKey(), e.getValue(), Long::sum);
        }

        for (var e : aResult.allNull.entrySet()) {
            allNull.merge(e.getKey(), e.getValue(), Boolean::logicalAnd);
        }

        for (var e : aResult.annotatedPositionCounts.entrySet()) {
            annotatedPositionCounts.merge(e.getKey(), e.getValue(), Long::sum);
        }

        for (var e : aResult.skips.entrySet()) {
            skips.merge(e.getKey(), e.getValue(), Long::sum);
        }

        empty &= aResult.empty;

        for (var e : aResult.diagnostics.entrySet()) {
            diagnostics.merge(e.getKey(), e.getValue(), Integer::sum);
        }

        // Once analyzed, the diagnostics are final and there is no tally left to merge into
        if (dataTally != null) {
            dataTally.add(aResult.dataTally);
        }

        if (incompleteSetsByPosition >= 0 && aResult.incompleteSetsByPosition >= 0) {
            incompleteSetsByPosition += aResult.incompleteSetsByPosition;
        }

        if (incompleteSetsByLabel >= 0 && aResult.incompleteSetsByLabel >= 0) {
            incompleteSetsByLabel += aResult.incompleteSetsByLabel;
        }

        if (pluralitySets >= 0 && aResult.pluralitySets >= 0) {
            pluralitySets += aResult.pluralitySets;
        }

        if (relevantSetCount >= 0 && aResult.relevantSetCount >= 0) {
            relevantSetCount += aResult.relevantSetCount;
        }

        if (completeSetCount >= 0 && aResult.completeSetCount >= 0) {
            completeSetCount += aResult.completeSetCount;
        }

        if (usedSetCount >= 0 && aResult.usedSetCount >= 0) {
            usedSetCount += aResult.usedSetCount;
        }

        if (unscoredSetCount >= 0 && aResult.unscoredSetCount >= 0) {
            unscoredSetCount += aResult.unscoredSetCount;
        }
    }

    public static AgreementSummary of(Serializable aResult)
    {
        return of(aResult, null);
    }

    public static AgreementSummary of(Serializable aResult, AgreementDiagnostics aDiagnostics)
    {
        return of(aResult, aDiagnostics, null);
    }

    /**
     * @param aResult
     *            the result of an agreement computation for one document.
     * @param aDiagnostics
     *            the configured analyzer, or {@code null} for a summary without diagnostics.
     * @param aDocumentName
     *            the name of the document, so that the diagnostics can point at it. May be
     *            {@code null}.
     * @return the summary.
     */
    public static AgreementSummary of(Serializable aResult, AgreementDiagnostics aDiagnostics,
            String aDocumentName)
    {
        if (aResult instanceof FullCodingAgreementResult result) {
            return new AgreementSummary(result, aDiagnostics, aDocumentName);
        }

        if (aResult instanceof FullUnitizingAgreementResult result) {
            return new AgreementSummary(result);
        }

        if (aResult instanceof FullAligningAgreementResult result) {
            return new AgreementSummary(result);
        }

        throw new IllegalArgumentException(
                "Unsupported result type: [" + aResult.getClass().getName() + "]");
    }

    /**
     * @param aLayer
     *            the layer.
     * @param aFeature
     *            the feature, if any.
     * @param aReason
     *            why the comparison produced no result for the document.
     * @param aRater
     *            the rater the reason is about, or {@code null} if it is not about a specific one.
     * @return a summary for a document on which the comparison was skipped.
     */
    public static AgreementSummary skipped(AnnotationLayer aLayer, AnnotationFeature aFeature,
            SkipReason aReason, String aRater)
    {
        var featureName = aFeature != null ? aFeature.getName() : null;
        var summary = new AgreementSummary(aLayer.getName(), featureName);
        summary.skips.put(new Skip(aReason, aRater), 1l);
        return summary;
    }

    public AgreementSummary(String aType, String aFeature)
    {
        this(aType, aFeature, Double.NaN);
    }

    /**
     * Creates a summary standing for a single computation with the given score. A score of
     * {@link Double#NaN} marks a document that could not be scored at all - see {@link #skipped}.
     */
    public AgreementSummary(String aType, String aFeature, double aAgreement)
    {
        type = aType;
        feature = aFeature;
        agreements.add(aAgreement);
        empty = Double.isNaN(aAgreement);
    }

    public AgreementSummary(FullUnitizingAgreementResult aResult)
    {
        this((FullAgreementResult_ImplBase<?>) aResult);

        incompleteSetsByLabel = -1;
        incompleteSetsByPosition = -1;
        relevantSetCount = -1;
        completeSetCount = -1;
        usedSetCount = -1;
        unscoredSetCount = -1;
        pluralitySets = -1;
    }

    public AgreementSummary(FullAligningAgreementResult aResult)
    {
        this((FullAgreementResult_ImplBase<?>) aResult);

        incompleteSetsByLabel = -1;
        incompleteSetsByPosition = -1;
        relevantSetCount = -1;
        completeSetCount = -1;
        usedSetCount = -1;
        unscoredSetCount = -1;
        pluralitySets = -1;
    }

    public AgreementSummary(FullCodingAgreementResult aResult)
    {
        this(aResult, null);
    }

    public AgreementSummary(FullCodingAgreementResult aResult, AgreementDiagnostics aDiagnostics)
    {
        this(aResult, aDiagnostics, null);
    }

    public AgreementSummary(FullCodingAgreementResult aResult, AgreementDiagnostics aDiagnostics,
            String aDocumentName)
    {
        this((FullAgreementResult_ImplBase<?>) aResult);

        incompleteSetsByLabel = aResult.getIncompleteSetsByLabel().size();
        incompleteSetsByPosition = aResult.getIncompleteSetsByPosition().size();
        pluralitySets = aResult.getPluralitySets().size();
        relevantSetCount = aResult.getRelevantSets().size();
        completeSetCount = aResult.getCompleteSets().size();

        for (var casGroupId : casGroupIds) {
            annotatedPositionCounts.put(casGroupId, 0l);
        }
        for (var set : aResult.getRelevantSets()) {
            for (var casGroupId : set.getCasGroupIds()) {
                annotatedPositionCounts.merge(casGroupId, 1l, Long::sum);
            }
        }

        excludeIncomplete = aResult.isExcludeIncomplete();

        usedSetCount = completeSetCount;
        if (!excludeIncomplete) {
            usedSetCount += incompleteSetsByLabel + incompleteSetsByPosition;
        }

        // Including the incomplete sets in the study does not mean the measure scores them. Some
        // measures drop every item on which only one rater gave a value, so these sets are not
        // used either. They are identified the same way as for the diagnostics - see
        // AgreementDataTally - so the table and the notes report the same number.
        if (!aResult.isEmpty() && !aResult.isScoringSingleValueItems()) {
            for (var item : aResult.getStudy().getItems()) {
                if (item.getRaterCount() < 2) {
                    unscoredSetCount++;
                }
            }
            usedSetCount -= unscoredSetCount;
        }

        // Only the raw counts are collected here. The thresholds are applied once the whole study
        // has been merged - see analyzeMerged - because a single document must not decide the
        // notes shown for a study spanning many documents.
        //
        // The analyzer is configurable and therefore a Spring bean. Callers that do not have one
        // to hand simply get a summary without diagnostics rather than being forced to obtain one.
        //
        // The diagnostics are advisory, so a failure there must never cost the actual score.
        if (aDiagnostics != null) {
            try {
                aDiagnostics.tally(aResult, aDocumentName, dataTally);
            }
            catch (Exception e) {
                LOG.warn("Unable to collect agreement diagnostics data", e);
            }
        }
    }

    private AgreementSummary(FullAgreementResult_ImplBase<?> aResult)
    {
        type = aResult.getType();
        feature = aResult.getFeature();
        casGroupIds.addAll(aResult.casGroupIds);
        agreements.add(aResult.agreement);
        aResult.getCategories().forEach(categories::add);
        empty = aResult.isEmpty();

        for (var casGroupId : casGroupIds) {
            itemCounts.put(casGroupId, aResult.getItemCount(casGroupId));
            nonNullContentCounts.put(casGroupId, aResult.getNonNullCount(casGroupId));
            allNull.put(casGroupId, aResult.isAllNull(casGroupId));
        }
    }

    public List<String> getCasGroupIds()
    {
        return casGroupIds.stream().sorted().toList();
    }

    private DoubleStream usableAgreements()
    {
        return agreements.stream() //
                .mapToDouble(a -> a) //
                .filter(v -> !Double.isNaN(v)); // skip documents for which we have no agreement
    }

    public double getAgreement()
    {
        return usableAgreements().average().orElse(Double.NaN);
    }

    public long getTotalAgreementsCount()
    {
        return agreements.size();
    }

    public long getUsableAgreementsCount()
    {
        return usableAgreements().count();
    }

    public String getType()
    {
        return type;
    }

    public String getFeature()
    {
        return feature;
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public long getItemCount(String aRater)
    {
        return itemCounts.getOrDefault(aRater, 0l);
    }

    public int getCategoryCount()
    {
        return categories.size();
    }

    public Long getNonNullCount(String aRater)
    {
        return nonNullContentCounts.getOrDefault(aRater, 0l);
    }

    public boolean isAllNull(String aRater)
    {
        return allNull.getOrDefault(aRater, true);
    }

    /**
     * @param aRater
     *            the rater.
     * @return the number of relevant positions at which the rater annotated anything, including
     *         positions the study excluded as incomplete.
     */
    public long getAnnotatedPositionCount(String aRater)
    {
        return annotatedPositionCounts.getOrDefault(aRater, 0l);
    }

    /**
     * @return the number of documents on which the comparison was skipped, by reason, in the order
     *         in which the reasons were first encountered.
     */
    public Map<Skip, Long> getSkips()
    {
        return unmodifiableMap(skips);
    }

    public int getIncompleteSetsByPosition()
    {
        return incompleteSetsByPosition;
    }

    public int getIncompleteSetsByLabel()
    {
        return incompleteSetsByLabel;
    }

    public int getRelevantSetCount()
    {
        return relevantSetCount;
    }

    public int getCompleteSetCount()
    {
        return completeSetCount;
    }

    public int getPluralitySets()
    {
        return pluralitySets;
    }

    public int getUsedSetCount()
    {
        return usedSetCount;
    }

    /**
     * @return the number of sets that were part of the study but that the measure did not score
     *         because only one rater gave them a value.
     */
    public int getUnscoredSetCount()
    {
        return unscoredSetCount;
    }

    /**
     * @return whether the computation excluded sets that only some of the raters annotated. When it
     *         did not, those sets were scored, so they do not contribute to the number of excluded
     *         sets even though they are still counted as incomplete.
     */
    public boolean isExcludeIncomplete()
    {
        // A summary that never scored anything has no sets to report as excluded either, so the
        // value it reports cannot mislead - it only has to be defined.
        return excludeIncomplete == null || excludeIncomplete;
    }

    /**
     * Runs the diagnostics that can only be evaluated once all documents of a rater pair have been
     * merged, such as how much of the selection actually produced a score. Per-document
     * characteristics are collected during merging instead - see the constructor.
     *
     * @param aDiagnostics
     *            the configured analyzer.
     */
    public void analyzeMerged(AgreementDiagnostics aDiagnostics)
    {
        analyzeMerged(aDiagnostics, Function.identity());
    }

    /**
     * Runs the diagnostics that can only be evaluated once all documents of a rater pair have been
     * merged, such as how much of the selection actually produced a score. Per-document
     * characteristics are collected during merging instead - see the constructor.
     *
     * @param aDiagnostics
     *            the configured analyzer.
     * @param aRaterNames
     *            maps the CAS group id of a rater to the name shown to the user.
     */
    public void analyzeMerged(AgreementDiagnostics aDiagnostics,
            Function<String, String> aRaterNames)
    {
        // Without a tally, the summary has already been analyzed - analyzing it again would only
        // lose the diagnostics derived from the tally
        if (aDiagnostics == null || dataTally == null) {
            return;
        }

        // Every diagnostic is evaluated here, on the counts accumulated across all documents of
        // the study, so each one counts once for this study no matter how many documents it spans.
        diagnostics.clear();
        try {
            aDiagnostics.analyze(dataTally, this, aRaterNames)
                    .forEach(d -> diagnostics.merge(d, 1, Integer::sum));
        }
        catch (Exception e) {
            // The diagnostics are advisory, so a failure there must never cost the actual score.
            LOG.warn("Unable to analyze agreement diagnostics", e);
            diagnostics.clear();
        }

        dataTally = null;
    }

    /**
     * @return the data characteristics observed across the merged computations, most widespread
     *         first.
     */
    public List<AgreementDiagnostic> getDiagnostics()
    {
        return diagnostics.entrySet().stream() //
                .sorted(Map.Entry.<AgreementDiagnostic, Integer> comparingByValue().reversed()) //
                .map(Map.Entry::getKey) //
                .toList();
    }

    /**
     * Records a data characteristic observed for this computation.
     *
     * @param aDiagnostic
     *            the characteristic.
     */
    public void addDiagnostic(AgreementDiagnostic aDiagnostic)
    {
        diagnostics.merge(aDiagnostic, 1, Integer::sum);
    }

    /**
     * @param aDiagnostic
     *            a diagnostic reported by {@link #getDiagnostics()}.
     * @return in how many of the merged computations the characteristic was observed.
     */
    public int getDiagnosticCount(AgreementDiagnostic aDiagnostic)
    {
        return diagnostics.getOrDefault(aDiagnostic, 0);
    }

    /**
     * Why a comparison was skipped on a document and, where the reason is about a specific rater,
     * which one.
     *
     * @param reason
     *            the reason.
     * @param rater
     *            the rater, or {@code null} if the reason is not about a specific one.
     */
    public record Skip(SkipReason reason, String rater)
        implements Serializable
    {}
}
