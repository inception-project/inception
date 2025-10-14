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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.USED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
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
import java.util.Set;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class AgreementServiceImpl
    implements AgreementService
{
    private static final String NO_ANNOTATION = "<no annotation>";
    private static final String NO_LABEL = "<no label>";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final AnnotationSchemaService schemaService;
    private final UserDao userService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public AgreementServiceImpl(DocumentService aDocumentService,
            AnnotationSchemaService aSchemaService, UserDao aUserService,
            DiffAdapterRegistry aDiffAdapterRegistry)
    {
        documentService = aDocumentService;
        schemaService = aSchemaService;
        userService = aUserService;
        diffAdapterRegistry = aDiffAdapterRegistry;
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
            for (var doc : documents) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }
        else {
            for (var doc : documentService.listSourceDocuments(aProject)) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }

        return allAnnDocs;
    }

    @Override
    public void exportDiff(OutputStream aOut, AnnotationLayer aLayer, AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits, List<SourceDocument> aDocuments,
            List<AnnotationSet> aAnnotators)
    {
        var project = aLayer.getProject();

        var allAnnDocs = getDocumentsToEvaluate(project, aDocuments, aTraits);
        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        var adapters = diffAdapterRegistry.getDiffAdapters(asList(aLayer));

        Set<String> tagset = aFeature != null
                ? schemaService.listTags(aFeature.getTagset()).stream() //
                        .map(Tag::getName) //
                        .collect(toCollection(LinkedHashSet::new))
                : emptySet();

        var featureName = aFeature != null ? aFeature.getName() : null;

        var countWritten = 0;
        for (var doc : docs) {
            var annDocs = allAnnDocs.get(doc);
            try (var session = CasStorageSession.openNested()) {
                var casMap = loadCasForAnnotators(doc, annDocs, aAnnotators);

                var diff = doDiff(adapters, casMap);

                var result = CodingStudyUtils.makeCodingStudy(diff, aLayer.getName(), featureName,
                        tagset, aTraits.isExcludeIncomplete(), casMap);

                try (var printer = new CSVPrinter(
                        new OutputStreamWriter(CloseShieldOutputStream.wrap(aOut), UTF_8),
                        RFC4180)) {

                    configurationSetsWithItemsToCsv(printer, result, countWritten == 0,
                            userService);
                }

                countWritten++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
            }
        }
    }

    private LinkedHashMap<String, CAS> loadCasForAnnotators(SourceDocument aDocument,
            List<AnnotationDocument> aAnnDocs, List<AnnotationSet> aAnnotators)
        throws IOException
    {
        var casMap = new LinkedHashMap<String, CAS>();

        for (var annotator : aAnnotators) {
            var maybeCas = loadCas(aDocument, annotator, aAnnDocs);
            var cas = maybeCas.isPresent() ? maybeCas.get() : loadInitialCas(aDocument);
            casMap.put(annotator.id(), cas);
        }

        return casMap;
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, AnnotationSet aSet,
            List<AnnotationDocument> aAnnDocs)
        throws IOException
    {
        if (AnnotationSet.CURATION_SET.equals(aSet)) {
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(aDocument.getState())) {
                return Optional.empty();
            }

            return loadCas(aDocument, aSet);
        }

        if (aAnnDocs.stream().noneMatch(annDoc -> aSet.id().equals(annDoc.getUser()))) {
            return Optional.empty();
        }

        if (!documentService.existsCas(aDocument, aSet)) {
            return Optional.empty();
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
        configurationSetsWithItemsToCsv(aOut, aAgreement, aIncludeHeader, null);
    }

    private static void configurationSetsWithItemsToCsv(CSVPrinter aOut,
            FullCodingAgreementResult aAgreement, boolean aIncludeHeader, UserDao aUserService)
        throws IOException
    {
        if (aIncludeHeader) {
            var headers = new ArrayList<>(asList("Type", "Collection", "Document", "Layer",
                    "Feature", "Position", "Flags"));
            aAgreement.getCasGroupIds().stream() //
                    .map($ -> getUserName(aUserService, $)) //
                    .forEach(headers::add);
            aOut.printRecord(headers);
        }

        var relevantSets = aAgreement.getRelevantSets();
        var usedItemIterator = aAgreement.getStudy().getItems().iterator();
        for (var cfgSet : relevantSets) {
            var row = new ArrayList<String>();
            var pos = cfgSet.getPosition();

            row.add(pos.getClass().getSimpleName());
            row.add(pos.getCollectionId());
            row.add(pos.getDocumentId());
            row.add(pos.getType());
            row.add(aAgreement.getFeature());
            row.add(cfgSet.getPosition().toMinimalString());
            row.add(cfgSet.getTags().stream().map(s -> s.toString()).collect(joining(", ")));

            if (cfgSet.getTags().contains(USED)) {
                // The study contains only the USED items
                var item = usedItemIterator.next();

                for (var unit : item.getUnits()) {
                    if (unit.getCategory() == null) {
                        row.add(NO_ANNOTATION);
                    }
                    else if ("".equals(unit.getCategory())) {
                        row.add(NO_LABEL);
                    }
                    else {
                        row.add(String.valueOf(unit.getCategory()));
                    }
                }
            }
            else {
                for (var rater : aAgreement.getCasGroupIds()) {
                    var values = cfgSet.getValues(rater);
                    if (values != null) {
                        row.add(join(", ", values.stream() //
                                .map($ -> Objects.toString($, NO_LABEL)) //
                                .sorted().toList()));
                    }
                    else {
                        row.add(NO_ANNOTATION);
                    }
                }
            }

            aOut.printRecord(row);
        }
    }

    private static String getUserName(UserDao aUserService, String aUserName)
    {
        if (aUserService == null) {
            return aUserName;
        }

        var user = aUserService.getUserOrCurationUser(aUserName);
        if (user != null) {
            return user.getUiName();
        }

        return aUserName;
    }
}
