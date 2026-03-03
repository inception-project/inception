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
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.DISCARD_OR_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.Progress;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.search.ReindexTask;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * (Re)indexes the annotation document for a specific user.
 */
public class IndexAnnotationDocumentTask
    extends IndexingTask_ImplBase
{
    public static final String TYPE = "IndexAnnotationDocumentTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired SearchService searchService;
    private @Autowired DocumentService documentService;

    private int done = 0;

    public IndexAnnotationDocumentTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder //
                .withProject(aBuilder.annotationDocument.getProject()) //
                .withType(TYPE));
    }

    @Override
    public String getTitle()
    {
        return "Indexing annotations...";
    }

    @Override
    public void execute()
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            var aDoc = getAnnotationDocument();
            var cas = documentService.readAnnotationCas(aDoc.getDocument(),
                    AnnotationSet.forUser(aDoc.getUser()), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            searchService.indexDocument(aDoc, WebAnnoCasUtil.casToByteArray(cas));
        }
        catch (IOException e) {
            LOG.error("Error indexing annotation document {}", getSourceDocument(), e);
        }

        done++;
    }

    @Deprecated
    @Override
    public Progress getProgress()
    {
        return new Progress("", done, 1);
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a re-indexing task for the project is scheduled, we do not need to schedule a new
        // annotation indexing task
        if (aTask instanceof ReindexTask) {
            if (Objects.equals(((ReindexTask) aTask).getProject().getId(),
                    getAnnotationDocument().getProject().getId())) {
                return DISCARD_OR_QUEUE_THIS;
            }
        }

        if (aTask instanceof IndexAnnotationDocumentTask) {
            if (Objects.equals(getAnnotationDocument().getId(),
                    ((IndexAnnotationDocumentTask) aTask).getAnnotationDocument().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
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
        public IndexAnnotationDocumentTask build()
        {
            Validate.notNull(annotationDocument, "Annotation document must be specified");

            return new IndexAnnotationDocumentTask(this);
        }
    }
}
