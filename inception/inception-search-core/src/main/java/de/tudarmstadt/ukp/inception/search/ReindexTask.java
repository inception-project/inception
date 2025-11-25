/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.search;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.DISCARD_OR_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;
import static de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState.KEY_SEARCH_STATE;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.casToByteArray;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.Progress;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.search.index.IndexRebuildRequiredException;
import de.tudarmstadt.ukp.inception.search.model.BulkIndexingContext;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexingTask_ImplBase;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import jakarta.persistence.NoResultException;

/**
 * Search indexer task. Runs the re-indexing process for a given project
 */
public class ReindexTask
    extends IndexingTask_ImplBase
    implements ProjectTask
{
    public static final String TYPE = "ReindexTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired SearchServiceImpl searchService;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired PreferencesService preferencesService;

    public ReindexTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE) //
                .withCancellable(false) //
                .withScope(PROJECT));
    }

    @Override
    public String getTitle()
    {
        return "Rebuilding index...";
    }

    @Override
    public void execute() throws IOException
    {
        var project = getProject();

        LOG.info("Re-indexing project {}. This may take a while...", project);

        try {
            project = projectService.getProject(getProject().getId());
        }
        catch (NoResultException e) {
            LOG.info("Re-indexing project {} skipped - project no longer exists", project);
            return;
        }

        try (var pooledIndex = searchService.acquireIndex(project.getId())) {
            if (searchService.isPerformNoMoreActions(pooledIndex)) {
                return;
            }

            var index = pooledIndex.get();
            index.setInvalid(true);
            searchService.writeIndex(pooledIndex);

            // Clear the index
            try {
                index.getPhysicalIndex().clear();
            }
            catch (IndexRebuildRequiredException e) {
                // We can ignore this since we are rebuilding the index already anyway
            }

            var usersWithPermissions = projectService.listUsersWithAnyRoleInProject(project)
                    .stream() //
                    .map(User::getUsername) //
                    .collect(toUnmodifiableSet());
            var sourceDocuments = documentService.listSupportedSourceDocuments(project);
            var annotationDocuments = documentService.listAnnotationDocuments(project).stream()
                    .filter(annDoc -> usersWithPermissions.contains(annDoc.getUser())) //
                    .filter(annDoc -> sourceDocuments.contains(annDoc.getDocument())) //
                    .toList();

            // We do not need write access and do not want to add to the exclusive access CAS cache,
            // so we would normally use SHARED_READ_ONLY_ACCESS. However, that mode can only be used
            // with AUTO_CAS_UPGRADE which makes things slow. We want NO_CAS_UPGRADE.
            // So we use UNMANAGED_NON_INITIALIZING_ACCESS for the annotation CASes to avoid
            // initializing CASes for users who have not started working on a document but for which
            // an AnnotationDocument item exists (e.g. locked documents).
            // For INITIAL_CASes, we use UNMANAGED_ACCESS since the INITIAL_CAS should always
            // exist.
            final var accessModeAnnotationCas = UNMANAGED_NON_INITIALIZING_ACCESS;
            final var accessModeInitialCas = UNMANAGED_ACCESS;
            final var casUpgradeMode = NO_CAS_UPGRADE;

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, project);

            try (var progress = getMonitor().openScope("documents",
                    annotationDocuments.size() + sourceDocuments.size())) {
                try (var indexContext = BulkIndexingContext.init(project, schemaService, true,
                        prefs)) {
                    // Index all the source documents
                    for (var doc : sourceDocuments) {
                        if (searchService.isPerformNoMoreActions(pooledIndex)) {
                            return;
                        }

                        if (getMonitor().isCancelled()) {
                            progress.update(up -> up.addMessage(LogMessage.info(this,
                                    "Indexing aborted. Search cannot be used.")));
                            break;
                        }

                        progress.update(up -> up.increment() //
                                .addMessage(LogMessage.info(this, "Source document: %s",
                                        doc.getName())));

                        try (var session = CasStorageSession.openNested()) {
                            // Index source document
                            var casAsByteArray = casToByteArray(
                                    documentService.createOrReadInitialCas(doc, casUpgradeMode,
                                            accessModeInitialCas));
                            searchService.indexDocument(pooledIndex, doc, casAsByteArray);

                            // Index curation document (if available)
                            if (documentService.existsCas(doc, CURATION_SET)
                                    && asList(CURATION_IN_PROGRESS, CURATION_FINISHED)
                                            .contains(doc.getState())) {
                                try {
                                    var aDoc = documentService.getAnnotationDocument(doc,
                                            CURATION_SET);
                                    var curationCasAsByteArray = casToByteArray(
                                            documentService.readAnnotationCas(doc, CURATION_SET,
                                                    casUpgradeMode, accessModeInitialCas));
                                    searchService.indexDocument(pooledIndex, aDoc, "reindex",
                                            curationCasAsByteArray);
                                }
                                catch (NoResultException e) {
                                    LOG.warn(
                                            "Found curation CAS for document {} but no annotation document",
                                            doc);
                                }
                            }
                        }
                        catch (Exception e) {
                            LOG.error("Error indexing document {}", doc, e);
                        }
                    }

                    // Index all the annotation documents (from annotators)
                    for (var doc : annotationDocuments) {
                        if (searchService.isPerformNoMoreActions(pooledIndex)) {
                            return;
                        }

                        if (getMonitor().isCancelled()) {
                            progress.update(up -> up.addMessage(LogMessage.info(this,
                                    "Indexing aborted. Search cannot be used.")));
                            break;
                        }

                        progress.update(up -> up.increment() //
                                .addMessage(LogMessage.info(this, "Annotation document: %s @ %s",
                                        doc.getUser(), doc.getName())));

                        try (var session = CasStorageSession.openNested()) {
                            var casAsByteArray = casToByteArray(documentService.readAnnotationCas(
                                    doc, casUpgradeMode, accessModeAnnotationCas));
                            searchService.indexDocument(pooledIndex, doc, "reindex",
                                    casAsByteArray);
                        }
                        catch (FileNotFoundException e) {
                            // Ignore it if a annotation CAS does not exist yet
                        }
                        catch (Exception e) {
                            LOG.error("Error indexing document {}", doc, e);
                        }
                    }
                }

                // After re-indexing, reset the invalid flag
                if (!getMonitor().isCancelled()) {
                    index.setInvalid(false);
                }

                searchService.writeIndex(pooledIndex);
            }
        }
        catch (IOException e) {
            LOG.error("Re-indexing project {} failed!", project, e);
        }

        if (!getMonitor().isCancelled()) {
            LOG.info("Re-indexing project {} complete!", project);
        }
        else {
            LOG.info("Re-indexing project {} aborted!", project);
        }
    }

    @Deprecated
    @Override
    public Progress getProgress()
    {
        return getMonitor().toProgress();
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a re-indexing task for a project is coming in, we can throw out any scheduled tasks
        // for indexing individual source/annotation documents in the project.
        if (aTask instanceof IndexSourceDocumentTask
                || aTask instanceof IndexAnnotationDocumentTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
            }
        }

        if (aTask instanceof ReindexTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return DISCARD_OR_QUEUE_THIS;
            }
        }

        return NO_MATCH;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends IndexingTask_ImplBase.Builder<T>
    {
        public ReindexTask build()
        {
            return new ReindexTask(this);
        }
    }
}
