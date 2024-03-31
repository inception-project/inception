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
package de.tudarmstadt.ukp.inception.ui.agreement.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class AgreementServiceImpl
    implements AgreementService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final AgreementMeasureSupportRegistry agreementRegistry;

    public AgreementServiceImpl(DocumentService aDocumentService,
            AgreementMeasureSupportRegistry aAgreementRegistry)
    {
        documentService = aDocumentService;
        agreementRegistry = aAgreementRegistry;
    }

    @Override
    public void exportAgreement(AnnotationFeature aFeature, OutputStream aOut, String aAnnotator1,
            String aAnnotator2, User aCurrentUser, String aMeasure, DefaultAgreementTraits traits)
    {
        var aProject = aFeature.getProject();
        var measure = agreementRegistry.getMeasure(aFeature, aMeasure, traits);

        var states = new ArrayList<AnnotationDocumentState>();
        states.add(AnnotationDocumentState.FINISHED);
        if (!traits.isLimitToFinishedDocuments()) {
            states.add(AnnotationDocumentState.IN_PROGRESS);
        }

        var allAnnDocs = documentService.listAnnotationDocumentsInState(aProject, //
                states.toArray(AnnotationDocumentState[]::new)).stream() //
                .collect(groupingBy(AnnotationDocument::getDocument));

        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();
        var countWritten = 0;
        for (var doc : docs) {
            try (var session = CasStorageSession.openNested()) {
                var maybeCas1 = loadCas(doc, aAnnotator1, allAnnDocs);
                var maybeCas2 = loadCas(doc, aAnnotator1, allAnnDocs);
                var cas1 = maybeCas1.isPresent() ? maybeCas1.get() : loadInitialCas(doc);
                var cas2 = maybeCas2.isPresent() ? maybeCas2.get() : loadInitialCas(doc);

                var casMap = new LinkedHashMap<String, CAS>();
                casMap.put(aAnnotator1, cas1);
                casMap.put(aAnnotator2, cas2);
                var res = measure.getAgreement(casMap);
                AgreementUtils.generateCsvReport(aOut, (FullCodingAgreementResult) res,
                        countWritten == 0);
                countWritten++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
            }
        }
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
}
