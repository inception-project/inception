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
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
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

    private final List<String> annotators;
    private final DefaultAgreementTraits traits;
    private final AnnotationFeature feature;
    private final AgreementMeasure<?> measure;
    private final Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

    private PairwiseAnnotationResult summary;

    public CalculatePairwiseAgreementTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        annotators = aBuilder.annotators;
        traits = aBuilder.traits;
        feature = aBuilder.feature;
        measure = aBuilder.measure;
        allAnnDocs = aBuilder.allAnnDocs;
    }

    @Override
    public void execute()
    {
        summary = new PairwiseAnnotationResult(feature, traits);

        var maxProgress = allAnnDocs.size();
        var progress = 0;

        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        for (var doc : docs) {
            var monitor = getMonitor();
            if (monitor.isCancelled()) {
                break;
            }

            monitor.setProgressWithMessage(progress, maxProgress,
                    LogMessage.info(this, doc.getName()));

            try (var session = CasStorageSession.openNested()) {
                AgreementSummary initialAgreementResult = null;

                for (int m = 0; m < annotators.size(); m++) {
                    var annotator1 = annotators.get(m);
                    var maybeCas1 = LazyInitializer.<Optional<CAS>> builder()
                            .setInitializer(() -> loadCas(doc, annotator1, allAnnDocs)).get();

                    for (int n = 0; n < annotators.size(); n++) {
                        var annotator2 = annotators.get(n);
                        var maybeCas2 = LazyInitializer.<Optional<CAS>> builder()
                                .setInitializer(() -> loadCas(doc, annotator2, allAnnDocs)).get();

                        // Triangle matrix mirrored
                        if (n < m) {
                            // So, theoretically, if cas1 and cas2 are both empty, then both are
                            // the initial CAS - so there must be full agreement. However, we
                            // would still need to count the units, categories, etc.
                            if (maybeCas1.get().isEmpty() && maybeCas2.get().isEmpty()) {
                                if (initialAgreementResult == null) {
                                    var casMap = new LinkedHashMap<String, CAS>();
                                    casMap.put("INITIAL1", loadInitialCas(doc));
                                    casMap.put("INITIAL2", loadInitialCas(doc));
                                    initialAgreementResult = AgreementSummary
                                            .of(measure.getAgreement(casMap));
                                }
                                var res = initialAgreementResult.remap(
                                        Map.of("INITIAL1", annotator1, "INITIAL2", annotator2));
                                summary.mergeResult(annotator1, annotator2, res);
                            }
                            else {
                                var cas1 = maybeCas1.get().isPresent() ? maybeCas1.get().get()
                                        : loadInitialCas(doc);
                                var cas2 = maybeCas2.get().isPresent() ? maybeCas2.get().get()
                                        : loadInitialCas(doc);

                                var casMap = new LinkedHashMap<String, CAS>();
                                casMap.put(annotator1, cas1);
                                casMap.put(annotator2, cas2);
                                var res = AgreementSummary.of(measure.getAgreement(casMap));
                                summary.mergeResult(annotator1, annotator2, res);
                            }
                        }
                    }
                }

                progress++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
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

    private Optional<CAS> loadCas(SourceDocument aDocument, String aDataOwner,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        throws IOException
    {
        var annDocs = aAllAnnDocs.get(aDocument);

        if (annDocs.stream().noneMatch(annDoc -> aDataOwner.equals(annDoc.getUser()))) {
            return Optional.empty();
        }

        if (!documentService.existsCas(aDocument, aDataOwner)) {
            Optional.empty();
        }

        var cas = documentService.readAnnotationCas(aDocument, aDataOwner, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return Optional.of(cas);
    }

    public PairwiseAnnotationResult getResult()
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
        private List<String> annotators;
        private DefaultAgreementTraits traits;
        private AnnotationFeature feature;
        private AgreementMeasure<?> measure;
        private Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

        protected Builder()
        {
            withCancellable(true);
        }

        @SuppressWarnings("unchecked")
        public T withAnnotators(List<String> aAnnotators)
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
