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

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.agreement.BasicAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
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

    private final List<User> annotators;
    private final DefaultAgreementTraits traits;
    private final AnnotationFeature feature;
    private final AgreementMeasure<?> measure;
    private final Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

    private PairwiseAnnotationResult result;

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
        result = new PairwiseAnnotationResult(feature, traits);

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

            var annDocs = allAnnDocs.get(doc);

            monitor.setProgressWithMessage(progress, maxProgress,
                    LogMessage.info(this, doc.getName()));

            try (var session = CasStorageSession.openNested()) {
                for (int m = 0; m < annotators.size(); m++) {
                    var annotator1 = annotators.get(m).getUsername();
                    if (annDocs.stream().noneMatch(a -> annotator1.equals(a.getUser()))) {
                        continue;
                    }

                    var cas1 = loadCas(doc, annotator1);

                    for (int n = 0; n < annotators.size(); n++) {
                        var annotator2 = annotators.get(n).getUsername();

                        var cas2 = loadCas(doc, annotator2);

                        // Triangle matrix mirrored
                        if (n < m) {
                            var casMap = new LinkedHashMap<String, CAS>();
                            casMap.put(annotator1, cas1);
                            casMap.put(annotator2, cas2);
                            var res = BasicAgreementResult.of(measure.getAgreement(casMap));

                            var existingRes = result.getStudy(annotators.get(m).getUsername(),
                                    annotators.get(n).getUsername());
                            if (existingRes != null) {
                                existingRes.merge(res);
                            }
                            else {
                                result.add(annotators.get(m).getUsername(),
                                        annotators.get(n).getUsername(), res);
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

    private CAS loadCas(SourceDocument doc, String annotator1) throws IOException
    {
        var cas = documentService.readAnnotationCas(doc, annotator1, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", doc.getName());
        FSUtil.setFeature(dmd, "collectionId", doc.getProject().getName());

        return cas;
    }

    public PairwiseAnnotationResult getResult()
    {
        return result;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        private List<User> annotators;
        private DefaultAgreementTraits traits;
        private AnnotationFeature feature;
        private AgreementMeasure<?> measure;
        private Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

        protected Builder()
        {
            withCancellable(true);
        }

        @SuppressWarnings("unchecked")
        public T withAnnotators(List<User> aAnnotators)
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
