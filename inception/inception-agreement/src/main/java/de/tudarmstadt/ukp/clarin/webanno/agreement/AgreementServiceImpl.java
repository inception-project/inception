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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
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
import java.io.UncheckedIOException;
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

public class AgreementServiceImpl
    implements AgreementService
{
    private static final String NO_ANNOTATION = "<no annotation>";
    private static final String NO_LABEL = "<no label>";
    private static final String NOT_EVALUATED = "<not evaluated>";

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

        // The columns stay the same for every document, even though not every rater takes part in
        // every document - otherwise the rows of the documents would not line up under the header.
        var columns = aAnnotators.stream() //
                .map(AnnotationSet::id) //
                .sorted() //
                .toList();

        var countWritten = 0;
        for (var doc : docs) {
            var annDocs = allAnnDocs.get(doc);
            try (var session = CasStorageSession.openNested()) {
                var casMap = loadCasForRaters(doc, annDocs, aAnnotators, aTraits);

                // As in the agreement tables, a document on which fewer than two of the raters
                // take part offers nothing to compare.
                if (casMap.size() < 2) {
                    continue;
                }

                var diff = doDiff(adapters, casMap);

                var result = CodingStudyUtils.makeCodingStudy(diff, aLayer.getName(), featureName,
                        tagset, aTraits.isExcludeIncomplete(), casMap);

                try (var printer = new CSVPrinter(
                        new OutputStreamWriter(CloseShieldOutputStream.wrap(aOut), UTF_8),
                        RFC4180)) {

                    configurationSetsWithItemsToCsv(printer, result, columns, countWritten == 0,
                            userService);
                }

                countWritten++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
            }
        }

        // Even if no document had anything to compare, the export should still say which columns
        // it would have had rather than being an empty file.
        if (countWritten == 0) {
            try (var printer = new CSVPrinter(
                    new OutputStreamWriter(CloseShieldOutputStream.wrap(aOut), UTF_8), RFC4180)) {
                printHeader(printer, columns, userService);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Loads the data of the raters taking part in the given document. Which raters take part is
     * decided the same way as in the agreement calculation, so that the diff shows what the
     * agreement tables are based on: a rater who does not take part is left out rather than being
     * represented by an empty CAS, which would make it look as if the rater disagreed with
     * everybody.
     */
    private LinkedHashMap<String, CAS> loadCasForRaters(SourceDocument aDocument,
            List<AnnotationDocument> aAnnDocs, List<AnnotationSet> aAnnotators,
            DefaultAgreementTraits aTraits)
        throws IOException
    {
        var casMap = new LinkedHashMap<String, CAS>();

        for (var annotator : aAnnotators) {
            loadCas(aDocument, annotator, aAnnDocs, aTraits) //
                    .ifPresent(cas -> casMap.put(annotator.id(), cas));
        }

        return casMap;
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, AnnotationSet aSet,
            List<AnnotationDocument> aAnnDocs, DefaultAgreementTraits aTraits)
        throws IOException
    {
        if (CURATION_SET.equals(aSet)) {
            if (!AgreementService.isCurationToEvaluate(aDocument, aTraits)) {
                return Optional.empty();
            }

            return loadCas(aDocument, aSet);
        }

        if (INITIAL_SET.equals(aSet)) {
            return Optional.of(loadInitialCas(aDocument));
        }

        if (aAnnDocs.stream().noneMatch(annDoc -> aSet.id().equals(annDoc.getUser()))) {
            return Optional.empty();
        }

        // A rater whose document is in an evaluated state but who has not saved anything yet takes
        // part with the unannotated document.
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

    private CAS loadInitialCas(SourceDocument aDocument) throws IOException
    {
        var cas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        return cas;
    }

    public static void configurationSetsWithItemsToCsv(CSVPrinter aOut,
            FullCodingAgreementResult aAgreement, boolean aIncludeHeader)
        throws IOException
    {
        configurationSetsWithItemsToCsv(aOut, aAgreement, aAgreement.getCasGroupIds(),
                aIncludeHeader, null);
    }

    private static void printHeader(CSVPrinter aOut, List<String> aColumns, UserDao aUserService)
        throws IOException
    {
        var headers = new ArrayList<>(
                asList("Type", "Collection", "Document", "Layer", "Feature", "Position", "Flags"));
        aColumns.stream() //
                .map($ -> getUserName(aUserService, $)) //
                .forEach(headers::add);
        aOut.printRecord(headers);
    }

    /**
     * @param aColumns
     *            the raters to write a column for. Raters that are not part of the given result are
     *            marked as not evaluated.
     */
    static void configurationSetsWithItemsToCsv(CSVPrinter aOut,
            FullCodingAgreementResult aAgreement, List<String> aColumns, boolean aIncludeHeader,
            UserDao aUserService)
        throws IOException
    {
        if (aIncludeHeader) {
            printHeader(aOut, aColumns, aUserService);
        }

        var raters = aAgreement.getCasGroupIds();

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

            // The study contains only the USED items
            var item = cfgSet.getTags().contains(USED) ? usedItemIterator.next() : null;

            for (var column : aColumns) {
                var raterIdx = raters.indexOf(column);
                if (raterIdx < 0) {
                    row.add(NOT_EVALUATED);
                }
                else if (item != null) {
                    var category = item.getUnit(raterIdx).getCategory();
                    if (category == null) {
                        row.add(NO_ANNOTATION);
                    }
                    else if ("".equals(category)) {
                        row.add(NO_LABEL);
                    }
                    else {
                        row.add(String.valueOf(category));
                    }
                }
                else {
                    var values = cfgSet.getValues(column);
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
