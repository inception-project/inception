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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
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
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class CalculatePairwiseAgreementTask
    extends Task
{
    public static final String TYPE = "CalculatePairwiseAgreementTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;

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
                        .addMessage(LogMessage.info(this, doc.getName())));

                try (var session = CasStorageSession.openNested()) {
                    for (int m = 0; m < annotators.size(); m++) {
                        if (getMonitor().isCancelled()) {
                            break;
                        }

                        var annotator1 = annotators.get(m);
                        var maybeCas1 = LazyInitializer.<Optional<CAS>> builder()
                                .setInitializer(() -> loadCas(doc, annotator1, allAnnDocs)).get();

                        for (int n = 0; n < annotators.size(); n++) {
                            if (getMonitor().isCancelled()) {
                                break;
                            }

                            if (!(n < m)) {
                                // Triangle matrix mirrored
                                continue;
                            }

                            var annotator2 = annotators.get(n);

                            if ((CURATION_SET.equals(annotator1)
                                    || CURATION_USER.equals(annotator2))
                                    && !asList(CURATION_IN_PROGRESS, CURATION_FINISHED)
                                            .contains(doc.getState())) {
                                LOG.trace(
                                        "Skipping combination {}/{}@{}: {} not in a curation state",
                                        annotator1, annotator2, doc, annotator1);
                                summary.mergeResult(annotator1.id(), annotator2.id(),
                                        AgreementSummary.skipped(layer, feature));
                                continue;
                            }

                            if (maybeCas1.get().isEmpty()) {
                                LOG.trace("Skipping combination {}/{}@{}: {} has no data",
                                        annotator1, annotator2, doc, annotator1);
                                summary.mergeResult(annotator1.id(), annotator2.id(),
                                        AgreementSummary.skipped(layer, feature));
                                continue;
                            }

                            var maybeCas2 = LazyInitializer.<Optional<CAS>> builder()
                                    .setInitializer(() -> loadCas(doc, annotator2, allAnnDocs))
                                    .get();

                            if (maybeCas2.get().isEmpty()) {
                                LOG.trace("Skipping combination {}/{}@{}: {} has no data",
                                        annotator1, annotator2, doc, annotator2);
                                summary.mergeResult(annotator1.id(), annotator2.id(),
                                        AgreementSummary.skipped(layer, feature));
                                continue;
                            }

                            var casMap = new LinkedHashMap<String, CAS>();
                            casMap.put(annotator1.id(), maybeCas1.get().get());
                            casMap.put(annotator2.id(), maybeCas2.get().get());
                            var res = AgreementSummary.of(measure.getAgreement(casMap));
                            summary.mergeResult(annotator1.id(), annotator2.id(), res);
                        }
                    }
                }
                catch (Exception e) {
                    LOG.error("Unable to load data", e);
                }
            }
        }
    }

    private CAS loadInitialCas(SourceDocument aDocument) throws IOException
    {
        var cas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return cas;
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, AnnotationSet aSet,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        throws IOException
    {
        if (CURATION_SET.equals(aSet)) {
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(aDocument.getState())) {
                return Optional.empty();
            }

            return loadCas(aDocument, aSet);
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

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return Optional.of(cas);
    }

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
