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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.BOUNDARY_DISAGREEMENT;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.FEW_ITEMS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.MARGINAL_ASYMMETRY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.RATER_WITHOUT_ANNOTATIONS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SINGLE_CATEGORY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SKEWED_PREVALENCE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.UNUSABLE_DOCUMENTS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.FAILED;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.config.AgreementDiagnosticsProperties;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;

/**
 * Inspects a completed agreement computation and reports characteristics of the data that affect
 * how the resulting score should be read.
 * <p>
 * This runs <em>after</em> the agreement has been calculated, where the annotation study is
 * available, rather than trying to predict data characteristics before reading any data. The
 * diagnostics produced here are deliberately measure-agnostic: they state what is true of the data.
 * Turning an observation into advice ("consider Gwet's AC1") is the job of the measure that was
 * actually used.
 */
public class AgreementDiagnostics
{
    /**
     * Smallest number of documents without a score for which coverage is reported. A single
     * unscored document is too common to be worth a note - e.g. one that has not been started yet.
     */
    static final int MINIMUM_UNUSABLE_DOCUMENTS = 2;

    /**
     * Shares are computed in floating point, so a share that is exactly at a threshold in theory
     * can land a hair below it - 60% - 40% yields 0.19999999999999996, not 0.2. Thresholds are
     * inclusive, so shares are compared with this much slack.
     */
    private static final double SHARE_TOLERANCE = 1e-9;

    /**
     * Joins the labels that together form the subject of a diagnostic. A control character, so it
     * cannot be confused with a character of a label.
     */
    private static final String SUBJECT_SEPARATOR = "\u001F";

    /**
     * How many document names a note lists before summarizing the rest as "and N more".
     */
    private static final int MAX_LISTED_DOCUMENTS = 3;

    /**
     * The characteristics that are also checked on each scored document on its own - see
     * {@link #checkAffectedDocuments}. Only those that distort a document's score and can cancel
     * out in the pooled data are. The number of items is not: a note listing the documents that are
     * short would point at nothing the user could change.
     */
    private static final List<AgreementDiagnosticType> PER_DOCUMENT_TYPES = List.of(SINGLE_CATEGORY,
            SKEWED_PREVALENCE, MARGINAL_ASYMMETRY);

    private final AgreementDiagnosticsProperties properties;

    public AgreementDiagnostics(AgreementDiagnosticsProperties aProperties)
    {
        properties = aProperties;
    }

    /**
     * @return the configured minimum basis for a share, but never less than one: a share over zero
     *         observations is undefined and would surface as "NaN%" in the text shown to the user.
     */
    private int minimumBasisForShare()
    {
        return Math.max(1, properties.getMinimumBasisForShare());
    }

    /**
     * @return whether the share reaches the (inclusive) threshold, allowing for floating point
     *         error - see {@link #SHARE_TOLERANCE}.
     */
    private static boolean reaches(double aShare, double aThreshold)
    {
        return aShare >= aThreshold - SHARE_TOLERANCE;
    }

    /**
     * Collects the raw counts of one document's result into the study's tally. The thresholds are
     * applied later, by {@link #analyze(AgreementDataTally, AgreementSummary)}, once every document
     * of the study has been added - a single document must not decide the notes for the study.
     *
     * @param aResult
     *            the result of an agreement computation for one document.
     * @param aTally
     *            the tally to add it to.
     */
    public void tally(FullCodingAgreementResult aResult, AgreementDataTally aTally)
    {
        tally(aResult, null, aTally);
    }

    /**
     * Collects the raw counts of one document's result into the study's tally. The thresholds are
     * applied later, by {@link #analyze(AgreementDataTally, AgreementSummary)}, once every document
     * of the study has been added - a single document must not decide the notes for the study. It
     * only decides whether it is one of the documents a note points at.
     *
     * @param aResult
     *            the result of an agreement computation for one document.
     * @param aDocumentName
     *            the name of the document, or {@code null} if it is not known. Notes about
     *            individual documents then count the document, but cannot name it.
     * @param aTally
     *            the tally to add it to.
     */
    public void tally(FullCodingAgreementResult aResult, String aDocumentName,
            AgreementDataTally aTally)
    {
        if (aResult == null || aTally == null) {
            return;
        }

        // The document is tallied on its own first, so that the characteristics it exhibits by
        // itself can be judged before its counts disappear into those of the study.
        var document = new AgreementDataTally();

        // The positions are counted even when the study has no items: when incomplete positions
        // are excluded, a document on which the raters never annotated the same position has an
        // empty study, and it is precisely such a document that shows boundary disagreement.
        document.addPositions(aResult.getRelevantSets().size(),
                aResult.getIncompleteSetsByPosition().size());

        var ratersWithoutPositions = aResult.getCasGroupIds().stream() //
                .filter(rater -> aResult.getRelevantSets().stream()
                        .noneMatch(set -> set.getCasGroupIds().contains(rater))) //
                .toList();
        document.addDocument(aDocumentName, ratersWithoutPositions);

        if (!aResult.isEmpty()) {
            document.add(aResult.getStudy(), aResult.isScoringSingleValueItems());
        }

        // Only documents that produced a score go into the average, so only those can distort it
        if (!Double.isNaN(aResult.getAgreement())) {
            var observed = new ArrayList<AgreementDiagnostic>();
            if (!document.isEmpty()) {
                checkLabelDistribution(document, observed);
            }
            document.addScoredDocument(aDocumentName, observed.stream() //
                    .map(AgreementDiagnostic::getType) //
                    .filter(PER_DOCUMENT_TYPES::contains) //
                    .collect(toSet()));
        }

        aTally.add(document);
    }

    /**
     * Analyzes a study once all of its documents have been merged. Every threshold is applied to
     * the totals accumulated in the tally rather than to any single document.
     *
     * @param aTally
     *            the counts accumulated across the documents of the study.
     * @param aSummary
     *            the merged summary of the study.
     * @return the characteristics observed. Never {@code null}.
     */
    public List<AgreementDiagnostic> analyze(AgreementDataTally aTally, AgreementSummary aSummary)
    {
        return analyze(aTally, aSummary, Function.identity());
    }

    /**
     * Analyzes a study once all of its documents have been merged. Every threshold is applied to
     * the totals accumulated in the tally rather than to any single document.
     *
     * @param aTally
     *            the counts accumulated across the documents of the study.
     * @param aSummary
     *            the merged summary of the study.
     * @param aRaterNames
     *            maps the CAS group id of a rater to the name shown to the user.
     * @return the characteristics observed. Never {@code null}.
     */
    public List<AgreementDiagnostic> analyze(AgreementDataTally aTally, AgreementSummary aSummary,
            Function<String, String> aRaterNames)
    {
        var diagnostics = new ArrayList<AgreementDiagnostic>();

        analyzeTally(aTally, diagnostics);
        analyzeCoverage(aSummary, diagnostics);
        analyzeRatersWithoutAnnotations(aTally, aRaterNames, diagnostics);

        // Every position of a document in which a rater has no annotations is incomplete. The
        // boundary note would then claim that the raters disagree about where to annotate, while
        // the actual cause - a rater who has not annotated these documents - is already reported.
        if (diagnostics.stream().anyMatch(d -> d.getType() == RATER_WITHOUT_ANNOTATIONS)) {
            diagnostics.removeIf(d -> d.getType() == BOUNDARY_DISAGREEMENT);
        }

        return diagnostics;
    }

    private void analyzeRatersWithoutAnnotations(AgreementDataTally aTally,
            Function<String, String> aRaterNames, List<AgreementDiagnostic> aDiagnostics)
    {
        if (aTally == null || aTally.getEmptyDocumentCounts().isEmpty()) {
            return;
        }

        // No threshold: a single such document may already be why a rater appears to disagree
        // about where to annotate, and no share can tell a rater who has not started yet from one
        // who had nothing to annotate - so every such document is reported.
        var counts = new LinkedHashMap<String, Integer>();
        aTally.getEmptyDocumentCounts().forEach(
                (rater, count) -> counts.merge(aRaterNames.apply(rater), count, Integer::sum));

        var names = new LinkedHashMap<String, SortedSet<String>>();
        aTally.getEmptyDocumentCounts().keySet()
                .forEach(rater -> names
                        .computeIfAbsent(aRaterNames.apply(rater), _k -> new TreeSet<>())
                        .addAll(aTally.getEmptyDocumentNames(rater)));

        aDiagnostics.add(
                new RaterWithoutAnnotationsDiagnostic(counts, names, aTally.getDocumentCount()));
    }

    private void analyzeCoverage(AgreementSummary aSummary, List<AgreementDiagnostic> aDiagnostics)
    {
        if (aSummary == null) {
            return;
        }

        // Documents that one of the raters never worked on - or, for pairs with the curator, that
        // are not in a curation state - are how the work was distributed, not data that got lost.
        // Where each document goes to only some of the annotators, counting them would flag nearly
        // every pair. The note is about documents both raters worked on that still produced no
        // score. Why the others were skipped is shown with the pair's result anyway.
        var notShared = aSummary.getSkips().entrySet().stream() //
                .filter(e -> e.getKey().reason() != FAILED) //
                .mapToLong(Map.Entry::getValue) //
                .sum();
        var total = aSummary.getTotalAgreementsCount() - notShared;
        var usable = aSummary.getUsableAgreementsCount();

        // The note qualifies a score that rests on only part of the selection. Without any score
        // there is nothing to qualify - the result already shows that there is no data - and a
        // note would merely repeat that, e.g. for every pair with the curator while nothing is in
        // a curation state.
        if (usable == 0) {
            return;
        }

        // Unlike the shares computed over labels or positions, this share is not an estimate - it
        // states exactly how much of the selection went into the score. It therefore does not need
        // the minimum basis for share: "only 2 of 15 documents produced a score" is worth knowing
        // however small the selection is.
        if (total - usable < MINIMUM_UNUSABLE_DOCUMENTS) {
            return;
        }

        var unusableShare = (total - usable) / (double) total;

        if (!reaches(unusableShare, properties.getUnusableDocumentThreshold())) {
            return;
        }

        aDiagnostics.add(new AgreementDiagnostic(UNUSABLE_DOCUMENTS,
                format("Only %d of the %d documents both raters worked on produced a score.",
                        usable, total),
                unusableShare, null));
    }

    /**
     * Applies the thresholds to the counts accumulated across the whole study.
     */
    void analyzeTally(AgreementDataTally aTally, List<AgreementDiagnostic> aDiagnostics)
    {
        if (aTally == null) {
            return;
        }

        // Boundary disagreement rests on the positions rather than on the labels, so it is checked
        // even when no labels were assigned - that is the case where the raters never agreed on
        // where to annotate at all.
        checkBoundaryDisagreement(aTally, aDiagnostics);

        // The number of items does not depend on any label having been assigned either: a study
        // whose items all left the feature unset is still a study of that many items, and its
        // score is at least as fragile as one with labels. Items the measure did not score still
        // make it a study, and one whose score rests on none of its items is the most fragile.
        if (aTally.getItemCount() + aTally.getUnscoredItemCount() > 0) {
            checkItemCount(aTally.getItemCount(), aDiagnostics);
        }

        if (aTally.isEmpty()) {
            return;
        }

        checkLabelDistribution(aTally, aDiagnostics);
        checkSparseCategories(aTally.getCategoryItemCounts(), aDiagnostics);
        checkAffectedDocuments(aTally, aDiagnostics);
    }

    /**
     * Checks the shape of the label distribution. Unlike the other checks, these are also applied
     * to each document on its own - see
     * {@link #tally(FullCodingAgreementResult, String, AgreementDataTally)}.
     */
    private void checkLabelDistribution(AgreementDataTally aTally,
            List<AgreementDiagnostic> aDiagnostics)
    {
        var overallCounts = aTally.getCategoryCounts();

        checkCategoryCount(overallCounts, aDiagnostics);
        checkPrevalence(overallCounts, aTally.getAssignmentCount(), aDiagnostics);
        checkMarginalAsymmetry(aTally.getPerRaterCategoryCounts(), overallCounts.keySet(),
                aDiagnostics);
    }

    /**
     * Reports the characteristics that many of the scored documents exhibit on their own, although
     * the pooled data does not. The score is the average of the per-document scores, so such
     * documents distort it however the characteristic balances out across them.
     */
    private void checkAffectedDocuments(AgreementDataTally aTally,
            List<AgreementDiagnostic> aDiagnostics)
    {
        var scored = aTally.getScoredDocumentCount();

        // With a single scored document, the document is the pooled data
        if (scored < 2) {
            return;
        }

        for (var type : PER_DOCUMENT_TYPES) {
            // Where the pooled data already shows the characteristic, that note covers it
            if (aDiagnostics.stream().anyMatch(d -> d.getType() == type)) {
                continue;
            }

            var affected = aTally.getAffectedDocumentCount(type);
            if (affected == 0) {
                continue;
            }

            // Like the share of documents without a score, this is an exact account of what went
            // into the average rather than an estimate, so the minimum basis for share does not
            // apply.
            if (!reaches(affected / (double) scored, properties.getAffectedDocumentThreshold())) {
                continue;
            }

            aDiagnostics.add(new AffectedDocumentsDiagnostic(type, describeCharacteristic(type),
                    aTally.getAffectedDocumentNames(type), affected, scored));
        }
    }

    private String describeCharacteristic(AgreementDiagnosticType aType)
    {
        switch (aType) {
        case SINGLE_CATEGORY:
            return "all annotations use the same label";
        case SKEWED_PREVALENCE:
            return format("a single label covers %s or more of the assigned labels",
                    describeShare(properties.getSkewThreshold()));
        case MARGINAL_ASYMMETRY:
            return format("the raters used a label at rates %.0f or more percentage points apart",
                    properties.getMarginalAsymmetryThreshold() * 100);
        default:
            throw new IllegalArgumentException("Not checked per document: " + aType);
        }
    }

    /**
     * Describes when a note of the given kind is reported, so that the user can tell why the same
     * note does not appear for other comparisons. Each threshold is applied to a comparison on its
     * own, so a comparison just short of it is silent although its data is much the same.
     *
     * @param aDiagnostic
     *            a diagnostic produced by this analyzer.
     * @return the criterion, or empty if the note is reported without a threshold or already states
     *         its threshold itself.
     */
    public Optional<String> describeCriterion(AgreementDiagnostic aDiagnostic)
    {
        // The observation of a note about individual documents already states what each document
        // exhibits - what remains is how many of them it takes.
        if (aDiagnostic instanceof AffectedDocumentsDiagnostic) {
            return Optional.of(format(
                    "Only reported where %s or more of the scored documents show this on their "
                            + "own, while the data as a whole does not.",
                    describeShare(properties.getAffectedDocumentThreshold())));
        }

        switch (aDiagnostic.getType()) {
        case BOUNDARY_DISAGREEMENT:
            return Optional.of(format(
                    "Only reported where %s or more of the positions were annotated by only some "
                            + "of the raters.",
                    describeShare(properties.getBoundaryDisagreementThreshold())));
        case FEW_ITEMS:
            return Optional.of(format("Only reported where the score rests on fewer than %d items.",
                    properties.getFewItemsLimit()));
        case SKEWED_PREVALENCE:
            return Optional.of(format(
                    "Only reported where a single label covers %s or more of the assigned labels.",
                    describeShare(properties.getSkewThreshold())));
        case MARGINAL_ASYMMETRY:
            return Optional.of(format(
                    "Only reported where the raters used a label at rates %.0f or more percentage "
                            + "points apart.",
                    properties.getMarginalAsymmetryThreshold() * 100));
        case UNUSABLE_DOCUMENTS:
            return Optional.of(format(
                    "Only reported where %s or more of the documents both raters worked on, and at "
                            + "least %d of them, produced no score.",
                    describeShare(properties.getUnusableDocumentThreshold()),
                    MINIMUM_UNUSABLE_DOCUMENTS));
        default:
            // A single label and a rater without annotations are reported whenever they occur,
            // and the note on sparse labels names its limit in the observation already.
            return Optional.empty();
        }
    }

    /**
     * Lists document names for a note, e.g. {@code "a.txt", "b.txt", "c.txt" and 2 more}.
     *
     * @param aNames
     *            the known names, sorted.
     * @param aTotal
     *            the number of documents, which includes those without a known name.
     * @return the listing, or an empty string if no name is known.
     */
    static String listDocuments(Collection<String> aNames, int aTotal)
    {
        if (aNames.isEmpty()) {
            return "";
        }

        var quoted = aNames.stream() //
                .limit(MAX_LISTED_DOCUMENTS) //
                .map(name -> format("\"%s\"", name)) //
                .toList();
        var more = Math.max(aTotal, aNames.size()) - quoted.size();

        if (more == 0) {
            return enumerate(quoted);
        }

        return join(", ", quoted) + " and " + more + " more";
    }

    /**
     * Analyzes a single hand-built study as if it were a whole study's worth of data.
     * Package-private so the threshold logic can be tested without constructing diff-based results.
     */
    void analyzeStudy(ICodingAnnotationStudy aStudy, List<AgreementDiagnostic> aDiagnostics)
    {
        var tally = new AgreementDataTally();
        tally.add(aStudy);
        analyzeTally(tally, aDiagnostics);
    }

    private void checkItemCount(int aTotalItems, List<AgreementDiagnostic> aDiagnostics)
    {
        if (aTotalItems >= properties.getFewItemsLimit()) {
            return;
        }

        aDiagnostics.add(new AgreementDiagnostic(FEW_ITEMS,
                format("The score is based on only %d item(s).", aTotalItems), aTotalItems, null));
    }

    private void checkCategoryCount(Map<Object, Integer> aCounts,
            List<AgreementDiagnostic> aDiagnostics)
    {
        if (aCounts.size() != 1) {
            return;
        }

        var onlyCategory = String.valueOf(aCounts.keySet().iterator().next());
        aDiagnostics.add(new AgreementDiagnostic(SINGLE_CATEGORY, onlyCategory.isEmpty()
                ? "No annotation has a label - the feature was always left unset."
                : format("All annotations use the same label (%s).", quoteLabel(onlyCategory)), 1,
                onlyCategory));
    }

    private void checkPrevalence(Map<Object, Integer> aCounts, int aTotalAssignments,
            List<AgreementDiagnostic> aDiagnostics)
    {
        // A single category cannot be "skewed" relative to others - that case is reported as
        // SINGLE_CATEGORY instead.
        if (aCounts.size() < 2) {
            return;
        }

        if (aTotalAssignments < minimumBasisForShare()) {
            return;
        }

        // Only a single dominant category produces the Kappa paradox. Mass concentrated across
        // several labels does not: with the same amount of disagreement, two labels at ~47% each
        // leave Kappa almost untouched, while one label at 94% roughly halves it. So this looks at
        // the most frequent label alone rather than at the concentration of the distribution.
        var ranked = aCounts.entrySet().stream() //
                .sorted(Map.Entry.<Object, Integer> comparingByValue().reversed()) //
                .toList();

        var topCount = ranked.get(0).getValue();
        var share = topCount / (double) aTotalAssignments;

        if (!reaches(share, properties.getSkewThreshold())) {
            return;
        }

        // Labels that are equally frequent are equally dominant - naming only one of them would
        // single it out for no reason in the data. A tie at the top can only reach the threshold
        // if it has been lowered to 50% or less, but then all of the tied labels are reported.
        var dominant = ranked.stream() //
                .filter(e -> e.getValue().equals(topCount)) //
                .map(e -> String.valueOf(e.getKey())) //
                .sorted() //
                .toList();

        String observation;
        if (dominant.size() == 1) {
            observation = format("%s covers %s of the %d assigned labels",
                    describeDominantLabel(dominant.get(0)), describeShare(share),
                    aTotalAssignments);
        }
        else {
            observation = format("The labels %s each cover %s of the %d assigned labels",
                    describeLabels(dominant), describeShare(share), aTotalAssignments);
        }

        // Naming the runner-up shows the shape of the distribution - whether the dominant label
        // stands against a long tail or against one other label that nearly matched it.
        if (ranked.size() > dominant.size()) {
            var runnerUp = ranked.get(dominant.size());
            var runnerUpShare = runnerUp.getValue() / (double) aTotalAssignments;
            observation += format("; the next most frequent (%s) covers %s.",
                    describeLabel(runnerUp.getKey()), describeShare(runnerUpShare));
        }
        else {
            observation += ".";
        }

        aDiagnostics.add(new AgreementDiagnostic(SKEWED_PREVALENCE, observation, share,
                join(SUBJECT_SEPARATOR, dominant)));
    }

    /**
     * Describes the dominant label as the subject of the observation.
     * <p>
     * The empty label is not a label the raters chose - it means the feature was left unset. It is
     * still a category as far as the measure is concerned, and a dominant one distorts the score
     * exactly like any other, so it must not be hidden. But reporting it as a label named
     * {@code ""} suggests rebalancing the label distribution, when what is actually needed is
     * filling the feature in.
     */
    private static String describeDominantLabel(Object aLabel)
    {
        var label = String.valueOf(aLabel);

        if (label.isEmpty()) {
            return "No label at all (the feature was left unset)";
        }

        return format("One label (\"%s\")", label);
    }

    /**
     * Describes a label where the surrounding sentence already supplies the context, such as the
     * runner-up in the prevalence observation.
     */
    private static String describeLabel(Object aLabel)
    {
        var label = String.valueOf(aLabel);

        if (label.isEmpty()) {
            return "no label at all";
        }

        return format("\"%s\"", label);
    }

    /**
     * Quotes a label for a list of labels. The empty label means the feature was left unset -
     * showing it as {@code ""} reads like a label that happens to have no name.
     */
    static String quoteLabel(String aLabel)
    {
        return aLabel.isEmpty() ? "(no label)" : format("\"%s\"", aLabel);
    }

    /**
     * Describes several labels as an enumeration, e.g. {@code "A", "B" and "C"}.
     */
    private static String describeLabels(List<?> aLabels)
    {
        return enumerate(aLabels.stream().map(AgreementDiagnostics::describeLabel).toList());
    }

    static String enumerate(List<String> aItems)
    {
        if (aItems.size() == 1) {
            return aItems.get(0);
        }

        return join(", ", aItems.subList(0, aItems.size() - 1)) + " and "
                + aItems.get(aItems.size() - 1);
    }

    /**
     * Renders a share, keeping it distinguishable from the extremes it rounds to. A real but tiny
     * share shown as "0%" names a label and then says it does not occur; a dominant-but-not-total
     * share shown as "100%" claims nothing else was used while the sentence goes on to name another
     * label. Both read as contradictions.
     */
    private static String describeShare(double aShare)
    {
        if (aShare > 0 && aShare < 0.01) {
            return "less than 1%";
        }

        if (aShare < 1 && aShare > 0.99) {
            return "more than 99%";
        }

        return format("%.0f%%", aShare * 100);
    }

    /**
     * @param aItemCounts
     *            per label, the number of items it was used on - not how often it was assigned,
     *            which would let the same evidence count once per rater.
     */
    private void checkSparseCategories(Map<Object, Integer> aItemCounts,
            List<AgreementDiagnostic> aDiagnostics)
    {
        var sparse = aItemCounts.entrySet().stream() //
                .filter(e -> e.getValue() < properties.getSparseCategoryLimit()) //
                .map(e -> String.valueOf(e.getKey())) //
                .sorted() //
                .toList();

        if (sparse.isEmpty()) {
            return;
        }

        aDiagnostics
                .add(new SparseCategoriesDiagnostic(sparse, properties.getSparseCategoryLimit()));
    }

    private void checkMarginalAsymmetry(Map<Integer, Map<Object, Integer>> aPerRaterCounts,
            Iterable<Object> aCategories, List<AgreementDiagnostic> aDiagnostics)
    {
        if (aPerRaterCounts.size() < 2) {
            return;
        }

        // Compare every rater against every other on every category and keep the largest gap in
        // relative category usage. A large gap means the raters applied the scheme differently
        // overall - pooled-marginal measures count this as disagreement, Cohen's Kappa does not.
        var raters = new ArrayList<>(aPerRaterCounts.keySet());
        var gaps = new LinkedHashMap<String, Double>();

        for (var i = 0; i < raters.size(); i++) {
            for (var j = i + 1; j < raters.size(); j++) {
                var countsA = aPerRaterCounts.get(raters.get(i));
                var countsB = aPerRaterCounts.get(raters.get(j));

                var totalA = countsA.values().stream().mapToInt(Integer::intValue).sum();
                var totalB = countsB.values().stream().mapToInt(Integer::intValue).sum();

                if (totalA < minimumBasisForShare() || totalB < minimumBasisForShare()) {
                    continue;
                }

                for (var category : aCategories) {
                    var shareA = countsA.getOrDefault(category, 0) / (double) totalA;
                    var shareB = countsB.getOrDefault(category, 0) / (double) totalB;
                    gaps.merge(String.valueOf(category), Math.abs(shareA - shareB), Math::max);
                }
            }
        }

        var worstGap = gaps.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);

        // A gap of zero means the raters used the scheme identically, which is the opposite of
        // asymmetry - report nothing, even where the configured threshold is zero.
        if (worstGap <= SHARE_TOLERANCE
                || !reaches(worstGap, properties.getMarginalAsymmetryThreshold())) {
            return;
        }

        // Every label at the largest gap is named. With two labels, this is always both of them:
        // whatever share one rater gives to one label more, they give to the other label less, so
        // the imbalance belongs to neither label in particular. Gaps are compared with a tolerance
        // because e.g. 0.7 - 0.4 and 0.6 - 0.3 differ in floating point. The sorted labels also
        // form the subject, so every comparison with the same imbalance is grouped into one note.
        var worstLabels = gaps.entrySet().stream() //
                .filter(e -> e.getValue() >= worstGap - SHARE_TOLERANCE) //
                .map(Map.Entry::getKey) //
                .sorted() //
                .toList();

        aDiagnostics.add(new AgreementDiagnostic(MARGINAL_ASYMMETRY,
                format("Raters used the %s %s at noticeably different rates "
                        + "(%.0f percentage points apart).",
                        worstLabels.size() == 1 ? "label" : "labels",
                        enumerate(worstLabels.stream().map(AgreementDiagnostics::quoteLabel)
                                .toList()),
                        worstGap * 100),
                worstGap, join(SUBJECT_SEPARATOR, worstLabels)));
    }

    private void checkBoundaryDisagreement(AgreementDataTally aTally,
            List<AgreementDiagnostic> aDiagnostics)
    {
        var relevant = aTally.getRelevantPositionCount();

        if (relevant < minimumBasisForShare()) {
            return;
        }

        var incompleteByPosition = aTally.getIncompletePositionCount();
        var share = incompleteByPosition / (double) relevant;

        // No incomplete position means the raters agreed on every boundary - report nothing, even
        // where the configured threshold is zero, rather than claim that 0% disagree.
        if (incompleteByPosition == 0
                || !reaches(share, properties.getBoundaryDisagreementThreshold())) {
            return;
        }

        var observation = format(
                "%.0f%% of the positions were annotated by only some of the raters.", share * 100);

        // Whether such positions count at all depends on the measure. Where it ignores them, the
        // share alone would suggest they lowered the score, so say what the score rests on instead.
        var unscored = aTally.getUnscoredItemCount();
        if (unscored > 0) {
            observation += format(
                    " The measure ignores the %d item(s) with a value from only one rater, "
                            + "so the score rests on the remaining %d.",
                    unscored, aTally.getItemCount());
        }

        aDiagnostics.add(new AgreementDiagnostic(BOUNDARY_DISAGREEMENT, observation, share, null));
    }
}
