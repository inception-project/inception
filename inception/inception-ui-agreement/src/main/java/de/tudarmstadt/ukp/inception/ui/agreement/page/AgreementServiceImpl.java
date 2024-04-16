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
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.COMPLETE;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.csv.CSVFormat.RFC4180;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class AgreementServiceImpl
    implements AgreementService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final AnnotationSchemaService schemaService;

    public AgreementServiceImpl(DocumentService aDocumentService,
            AnnotationSchemaService aSchemaService)
    {
        documentService = aDocumentService;
        schemaService = aSchemaService;
    }

    @Override
    public Map<SourceDocument, List<AnnotationDocument>> getDocumentsToEvaluate(Project aProject,
            List<SourceDocument> documents, DefaultAgreementTraits traits)
    {
        var states = new ArrayList<AnnotationDocumentState>();
        states.add(AnnotationDocumentState.FINISHED);
        if (!traits.isLimitToFinishedDocuments()) {
            states.add(AnnotationDocumentState.IN_PROGRESS);
        }

        var allAnnDocs = documentService.listAnnotationDocumentsInState(aProject, //
                states.toArray(AnnotationDocumentState[]::new)).stream() //
                .collect(groupingBy(AnnotationDocument::getDocument));

        if (isNotEmpty(documents)) {
            allAnnDocs.keySet().retainAll(documents);
        }
        return allAnnDocs;
    }

    @Override
    public void exportDiff(OutputStream aOut, AnnotationFeature aFeature,
            DefaultAgreementTraits traits, User aCurrentUser, List<SourceDocument> aDocuments,
            List<String> aAnnotators)
    {
        var project = aFeature.getProject();
        var allAnnDocs = getDocumentsToEvaluate(project, aDocuments, traits);

        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        var adapters = getDiffAdapters(schemaService, asList(aFeature.getLayer()));

        var tagset = schemaService.listTags(aFeature.getTagset()).stream() //
                .map(Tag::getName) //
                .collect(toCollection(LinkedHashSet::new));

        var countWritten = 0;
        for (var doc : docs) {
            try (var session = CasStorageSession.openNested()) {
                var casMap = new LinkedHashMap<String, CAS>();
                for (var annotator : aAnnotators) {
                    var maybeCas = loadCas(doc, annotator, allAnnDocs);
                    var cas = maybeCas.isPresent() ? maybeCas.get() : loadInitialCas(doc);
                    casMap.put(annotator, cas);
                }

                var diff = doDiff(adapters, traits.getLinkCompareBehavior(), casMap);

                var result = AgreementUtils.makeCodingStudy(diff, aFeature.getLayer().getName(),
                        aFeature.getName(), tagset, traits.isExcludeIncomplete(), casMap);

                try (var printer = new CSVPrinter(
                        new OutputStreamWriter(CloseShieldOutputStream.wrap(aOut), UTF_8),
                        RFC4180)) {

                    configurationSetsWithItemsToCsv(printer, result, countWritten == 0);
                }

                countWritten++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
            }
        }
    }

    @Override
    public void exportPairwiseDiff(OutputStream aOut, AnnotationFeature aFeature, String aMeasure,
            DefaultAgreementTraits traits, User aCurrentUser, List<SourceDocument> aDocuments,
            String aAnnotator1, String aAnnotator2)
    {
        exportDiff(aOut, aFeature, traits, aCurrentUser, aDocuments,
                asList(aAnnotator1, aAnnotator2));
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

    public static void configurationSetsWithItemsToCsv(CSVPrinter aOut,
            FullCodingAgreementResult aAgreement, boolean aIncludeHeader)
        throws IOException
    {
        if (aIncludeHeader) {
            var headers = new ArrayList<>(asList("Type", "Collection", "Document", "Layer",
                    "Feature", "Position", "Flags"));
            headers.addAll(aAgreement.getCasGroupIds());
            aOut.printRecord(headers);
        }

        var relevantSets = aAgreement.getRelevantSets();
        var completeItemIterator = aAgreement.getStudy().getItems().iterator();
        for (var cfgSet : relevantSets) {
            var row = new ArrayList<String>();
            var pos = cfgSet.getPosition();

            row.add(pos.getClass().getSimpleName());
            row.add(pos.getCollectionId());
            row.add(pos.getDocumentId());
            row.add(pos.getType());
            row.add(aAgreement.getFeature());
            row.add(cfgSet.getPosition().toMinimalString());
            row.add(cfgSet.getTags().stream().map(s -> s.toString())
                    .collect(Collectors.joining(", ")));

            if (cfgSet.getTags().contains(COMPLETE)) {
                // The study contains only the COMPLETE items
                var item = completeItemIterator.next();

                for (var unit : item.getUnits()) {
                    row.add(String.valueOf(unit.getCategory()));
                }
            }
            else {
                for (var rater : aAgreement.getCasGroupIds()) {
                    var values = cfgSet.getValues(rater);
                    if (values != null) {
                        row.add(join(", ", values.stream().map($ -> Objects.toString($, ""))
                                .sorted().toList()));
                    }
                    else {
                        row.add("");
                    }
                }
            }

            aOut.printRecord(row);
        }
    }
}
