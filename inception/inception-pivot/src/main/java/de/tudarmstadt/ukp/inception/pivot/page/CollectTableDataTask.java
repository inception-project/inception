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
package de.tudarmstadt.ukp.inception.pivot.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTableDataProvider;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class CollectTableDataTask<A extends Serializable, T extends FeatureStructure>
    extends Task
{
    public static final String TYPE = "CollectTableDataTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;

    private final List<String> dataOwners;
    private final Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;
    private final List<? extends Extractor<T, ? extends Serializable>> rowExtractors;
    private final List<? extends Extractor<T, ? extends Serializable>> colExtractors;

    private PivotTableDataProvider.Builder<A, T> summary;

    public CollectTableDataTask(Builder<? extends Builder<?, ?, ?>, A, T> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        dataOwners = aBuilder.dataOwners;
        allAnnDocs = aBuilder.allAnnDocs;
        rowExtractors = aBuilder.rowExtractors;
        colExtractors = aBuilder.colExtractors;

        summary = PivotTableDataProvider.builder(aBuilder.rowExtractors, aBuilder.colExtractors,
                aBuilder.cellExtractors, aBuilder.aggregator);
    }

    @Override
    public void execute()
    {
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
                    for (int m = 0; m < dataOwners.size(); m++) {
                        if (getMonitor().isCancelled()) {
                            break;
                        }

                        var dataOwner = dataOwners.get(m);
                        var maybeCas = loadCas(doc, dataOwner, allAnnDocs);

                        if (maybeCas.isEmpty()) {
                            continue;
                        }

                        var cas = maybeCas.get();

                        var triggerTypes = new HashSet<String>();
                        rowExtractors.stream() //
                                .map(Extractor::getTriggerType) //
                                .filter(Optional::isPresent) //
                                .map(Optional::get) //
                                .forEach(triggerTypes::add);
                        colExtractors.stream() //
                                .map(Extractor::getTriggerType) //
                                .filter(Optional::isPresent) //
                                .map(Optional::get) //
                                .forEach(triggerTypes::add);

                        var seen = new IntOpenHashSet();
                        for (var triggerType : triggerTypes) {
                            for (var fs : cas.select(triggerType)) {
                                var addr = ICasUtil.getAddr(fs);
                                if (!seen.contains(addr)) {
                                    summary.add((T) fs);
                                    seen.add(addr);
                                }
                            }
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

    private Optional<CAS> loadCas(SourceDocument aDocument, String aDataOwner,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        throws IOException
    {
        if (CURATION_USER.equals(aDataOwner)) {
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(aDocument.getState())) {
                return Optional.empty();
            }

            return loadCas(aDocument, aDataOwner);
        }

        var annDocs = aAllAnnDocs.get(aDocument);

        var effectiveState = annDocs.stream() //
                .filter(annDoc -> aDataOwner.equals(annDoc.getUser())) //
                .map(annDoc -> annDoc.getState()) //
                .findFirst() //
                .orElse(AnnotationDocumentState.NEW);

        return switch (effectiveState) {
        case IGNORE -> Optional.empty();
        case NEW -> Optional.of(loadInitialCas(aDocument));
        default -> loadCas(aDocument, aDataOwner);
        };
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, String aDataOwner) throws IOException
    {
        var cas = documentService.readAnnotationCas(aDocument, aDataOwner, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return Optional.of(cas);
    }

    public PivotTableDataProvider<?, ?> getResult()
    {
        return summary.build();
    }

    public static <T extends FeatureStructure, R extends Serializable> Builder<Builder<?, R, T>, R, T> builder(
            List<? extends Extractor<T, ? extends Serializable>> rowExtractors,
            List<? extends Extractor<T, ? extends Serializable>> colExtractors,
            List<? extends Extractor<T, ? extends Serializable>> cellExtractors,
            Aggregator<R, Object> aAggregator)
    {
        return new Builder<>(rowExtractors, colExtractors, cellExtractors, aAggregator);
    }

    public static class Builder<B extends Builder<?, A, T>, A extends Serializable, T extends FeatureStructure>
        extends Task.Builder<B>
    {
        private final List<? extends Extractor<T, ? extends Serializable>> rowExtractors;
        private final List<? extends Extractor<T, ? extends Serializable>> colExtractors;
        private final List<? extends Extractor<T, ? extends Serializable>> cellExtractors;
        private final Aggregator<A, Object> aggregator;

        private List<String> dataOwners;
        private Map<SourceDocument, List<AnnotationDocument>> allAnnDocs;

        protected Builder(List<? extends Extractor<T, ? extends Serializable>> aRowExtractors,
                List<? extends Extractor<T, ? extends Serializable>> aColExtractors,
                List<? extends Extractor<T, ? extends Serializable>> aCellExtractors,
                Aggregator<A, Object> aAggregator)
        {
            rowExtractors = aRowExtractors;
            colExtractors = aColExtractors;
            cellExtractors = aCellExtractors;
            aggregator = aAggregator;
            withCancellable(true);
        }

        @SuppressWarnings("unchecked")
        public B withDataOwners(List<String> aDataOwners)
        {
            dataOwners = aDataOwners;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B withDocuments(Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        {
            allAnnDocs = aAllAnnDocs;
            return (B) this;
        }

        @Override
        public B withProject(Project aProject)
        {
            return super.withProject(aProject);
        }

        @Override
        public B withTrigger(String aTrigger)
        {
            return super.withTrigger(aTrigger);
        }

        @Override
        public B withSessionOwner(User aSessionOwner)
        {
            return super.withSessionOwner(aSessionOwner);
        }

        public CollectTableDataTask<A, T> build()
        {
            return new CollectTableDataTask<>(this);
        }
    }
}
