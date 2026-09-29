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
package de.tudarmstadt.ukp.clarin.webanno.agreement.task;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementService.isCurationToEvaluate;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.FAILED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_ANNOTATED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_IN_CURATION_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.Task;

public class CalculatePairwiseAgreementTask
    extends Task
{
    public static final String TYPE = "CalculatePairwiseAgreementTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;
    private @Autowired AgreementDiagnostics agreementDiagnostics;

    private final List<AnnotationSet> annotators;
    private final DefaultAgreementTraits traits;
    private final AnnotationLayer layer;
    private final AnnotationFeature feature;
    private final AgreementMeasure<?> measure;
    private final Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

    private PairwiseAgreementResult summary;

    public CalculatePairwiseAgreementTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        annotators = aBuilder.annotators;
        traits = aBuilder.traits;
        layer = aBuilder.layer;
        feature = aBuilder.feature;
        measure = aBuilder.measure;
        allAnnDocs = aBuilder.allAnnDocs;
    }

    @Override
    public void execute()
    {
        summary = new PairwiseAgreementResult(feature, traits);

        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        try (var progress = getMonitor().openScope("documents", allAnnDocs.size())) {
            for (var doc : docs) {
                if (getMonitor().isCancelled()) {
                    break;
                }

                progress.update(up -> up.increment() //
                        .status("%s", doc.getName()).statusToLog());

                try (var session = CasStorageSession.openNested()) {
                    // Each rater's data is loaded at most once per document - also if loading
                    // fails, so a broken CAS is not retried and logged again for every pair.
                    var loadedCases = new HashMap<AnnotationSet, LoadedCas>();

                    for (int m = 0; m < annotators.size(); m++) {
                        if (getMonitor().isCancelled()) {
                            break;
                        }

                        var annotator1 = annotators.get(m);

                        for (int n = 0; n < annotators.size(); n++) {
                            if (getMonitor().isCancelled()) {
                                break;
                            }

                            if (!(n < m)) {
                                // Triangle matrix mirrored
                                continue;
                            }

                            var annotator2 = annotators.get(n);

                            if ((CURATION_SET.equals(annotator1) || CURATION_SET.equals(annotator2))
                                    && !isCurationToEvaluate(doc, traits)) {
                                LOG.trace(
                                        "Skipping combination {}/{}@{}: {} not in a curation state",
                                        annotator1, annotator2, doc, annotator1);
                                summary.mergeResult(annotator1.id(), annotator2.id(),
                                        AgreementSummary.skipped(layer, feature,
                                                NOT_IN_CURATION_STATE, null));
                                continue;
                            }

                            var cas1 = loadedCases.computeIfAbsent(annotator1,
                                    set -> tryLoadCas(doc, set));
                            if (skipIfUnusable(doc, annotator1, annotator2, annotator1, cas1)) {
                                continue;
                            }

                            var cas2 = loadedCases.computeIfAbsent(annotator2,
                                    set -> tryLoadCas(doc, set));
                            if (skipIfUnusable(doc, annotator1, annotator2, annotator2, cas2)) {
                                continue;
                            }

                            // A failure affects only this pair on this document. Recording it as
                            // skipped keeps the document in the pair's total, so the coverage note
                            // still reports it instead of the document silently vanishing.
                            try {
                                var casMap = new LinkedHashMap<String, CAS>();
                                casMap.put(annotator1.id(), cas1.cas().get());
                                casMap.put(annotator2.id(), cas2.cas().get());
                                var res = AgreementSummary.of(measure.getAgreement(casMap),
                                        agreementDiagnostics, doc.getName());
                                summary.mergeResult(annotator1.id(), annotator2.id(), res);
                            }
                            catch (Exception e) {
                                LOG.error("Unable to calculate agreement for {}/{}@{}", annotator1,
                                        annotator2, doc, e);
                                summary.mergeResult(annotator1.id(), annotator2.id(),
                                        AgreementSummary.skipped(layer, feature, FAILED, null));
                            }
                        }
                    }
                }
                catch (Exception e) {
                    LOG.error("Unable to load data", e);
                }
            }
        }

        // Only now does each rater pair know across how many documents it was actually able to
        // produce a score, so the diagnostics that depend on that can be evaluated.
        summary.analyzeMerged(agreementDiagnostics, raterNames(annotators));
    }

    /**
     * @return a function mapping the CAS group id of each of the given raters to the name shown to
     *         the user, falling back to the id itself.
     */
    static Function<String, String> raterNames(Collection<AnnotationSet> aRaters)
    {
        var names = aRaters.stream() //
                .collect(toMap(AnnotationSet::id, set -> set.name() != null ? set.name() : set.id(),
                        (a, b) -> a));
        return id -> names.getOrDefault(id, id);
    }

    private CAS loadInitialCas(SourceDocument aDocument) throws IOException
    {
        var cas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        return cas;
    }

    private LoadedCas tryLoadCas(SourceDocument aDocument, AnnotationSet aSet)
    {
        try {
            return new LoadedCas(loadCas(aDocument, aSet, allAnnDocs), false);
        }
        catch (Exception e) {
            LOG.error("Unable to load data of {}@{}", aSet, aDocument, e);
            return new LoadedCas(Optional.empty(), true);
        }
    }

    /**
     * Records the pair as skipped on the given document if the data of one of its raters could not
     * be loaded or does not exist.
     *
     * @return whether the pair was skipped.
     */
    private boolean skipIfUnusable(SourceDocument aDocument, AnnotationSet aAnnotator1,
            AnnotationSet aAnnotator2, AnnotationSet aRater, LoadedCas aCas)
    {
        if (aCas.failed()) {
            summary.mergeResult(aAnnotator1.id(), aAnnotator2.id(),
                    AgreementSummary.skipped(layer, feature, FAILED, aRater.id()));
            return true;
        }

        if (aCas.cas().isEmpty()) {
            LOG.trace("Skipping combination {}/{}@{}: {} has no data", aAnnotator1, aAnnotator2,
                    aDocument, aRater);
            summary.mergeResult(aAnnotator1.id(), aAnnotator2.id(),
                    AgreementSummary.skipped(layer, feature, NOT_ANNOTATED, aRater.id()));
            return true;
        }

        return false;
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, AnnotationSet aSet,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        throws IOException
    {
        if (CURATION_SET.equals(aSet)) {
            if (!isCurationToEvaluate(aDocument, traits)) {
                return Optional.empty();
            }

            return loadCas(aDocument, aSet);
        }

        if (INITIAL_SET.equals(aSet)) {
            return Optional.of(loadInitialCas(aDocument));
        }

        var annDocs = aAllAnnDocs.get(aDocument);

        if (annDocs.stream().noneMatch(annDoc -> aSet.id().equals(annDoc.getUser()))) {
            return Optional.empty();
        }

        if (!documentService.existsCas(aDocument, aSet)) {
            return Optional.of(loadInitialCas(aDocument));
        }

        return loadCas(aDocument, aSet);
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, AnnotationSet aSet) throws IOException
    {
        var cas = documentService.readAnnotationCas(aDocument, aSet, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        return Optional.of(cas);
    }

    /**
     * The outcome of loading the data of one rater for a document. If loading failed, the CAS is
     * empty and {@code failed} is set, which distinguishes it from a rater who has no data.
     */
    private record LoadedCas(Optional<CAS> cas, boolean failed) {}

    public PairwiseAgreementResult getResult()
    {
        return summary;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        private List<AnnotationSet> annotators;
        private DefaultAgreementTraits traits;
        private AnnotationLayer layer;
        private AnnotationFeature feature;
        private AgreementMeasure<?> measure;
        private Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

        protected Builder()
        {
            withCancellable(true);
        }

        @SuppressWarnings("unchecked")
        public T withAnnotators(List<AnnotationSet> aAnnotators)
        {
            annotators = aAnnotators;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withTraits(DefaultAgreementTraits aTraits)
        {
            traits = aTraits;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withLayer(AnnotationLayer aLayer)
        {
            layer = aLayer;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withFeature(AnnotationFeature aFeature)
        {
            feature = aFeature;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withMeasure(AgreementMeasure<?> aMeasure)
        {
            measure = aMeasure;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withDocuments(Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        {
            allAnnDocs = aAllAnnDocs;
            return (T) this;
        }

        public CalculatePairwiseAgreementTask build()
        {
            return new CalculatePairwiseAgreementTask(this);
        }
    }
}
