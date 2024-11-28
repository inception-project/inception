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
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PerDocumentAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class CalculatePerDocumentAgreementTask
    extends Task
{
    public static final String TYPE = "CalculatePerDocumentAgreementTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;

    private final Set<String> annotators;
    private final DefaultAgreementTraits traits;
    private final AnnotationFeature feature;
    private final AgreementMeasure<?> measure;
    private final Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

    private PerDocumentAgreementResult summary;

    public CalculatePerDocumentAgreementTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        annotators = new HashSet<>(aBuilder.annotators);
        traits = aBuilder.traits;
        feature = aBuilder.feature;
        measure = aBuilder.measure;
        allAnnDocs = aBuilder.allAnnDocs;
    }

    @Override
    public void execute()
    {
        summary = new PerDocumentAgreementResult(feature, traits);

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
                var casMap = new LinkedHashMap<String, CAS>();
                for (var annDoc : allAnnDocs.get(doc)) {
                    var dataOwner = annDoc.getUser();
                    if (!annotators.contains(dataOwner)) {
                        continue;
                    }

                    casMap.put(dataOwner, loadCas(annDoc.getDocument(), dataOwner));
                }

                if (annotators.contains(CURATION_USER)) {
                    casMap.put(CURATION_USER, loadCas(doc, CURATION_USER));
                }

                LOG.trace("Calculating agreement on {} for [{}] annotators", doc, casMap.size());
                var agreementResult = AgreementSummary.of(measure.getAgreement(casMap));
                summary.mergeResult(doc, agreementResult);

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

    private CAS loadCas(SourceDocument aDocument, String aDataOwner) throws IOException
    {
        if (CURATION_USER.equals(aDataOwner)) {
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(aDocument.getState())) {
                return loadInitialCas(aDocument);
            }
        }

        if (!documentService.existsCas(aDocument, aDataOwner)) {
            return loadInitialCas(aDocument);
        }

        var cas = documentService.readAnnotationCas(aDocument, aDataOwner, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return cas;
    }

    public PerDocumentAgreementResult getResult()
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
        private AnnotationLayer layer;
        private AnnotationFeature feature;
        private AgreementMeasure<?> measure;
        private Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;
        private Set<AnnotationDocumentState> states = new HashSet<>();

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

        @SuppressWarnings("unchecked")
        public T withStates(Collection<AnnotationDocumentState> aStates)
        {
            states.clear();
            if (aStates != null) {
                states.addAll(aStates);
            }
            return (T) this;
        }

        public CalculatePerDocumentAgreementTask build()
        {
            return new CalculatePerDocumentAgreementTask(this);
        }
    }
}
