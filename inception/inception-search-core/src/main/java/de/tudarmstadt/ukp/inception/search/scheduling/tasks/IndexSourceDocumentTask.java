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

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.DISCARD_OR_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.search.SearchCasUtils.casToByteArray;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.model.Progress;

/**
 * Document indexer task. Indexes the given document in a project
 */
public class IndexSourceDocumentTask
    extends IndexingTask_ImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired SearchService searchService;
    private @Autowired DocumentService documentService;

    private int done = 0;

    public IndexSourceDocumentTask(SourceDocument aSourceDocument, String aTrigger)
    {
        super(aSourceDocument, aTrigger);
    }

    @Override
    public void execute()
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            var cas = documentService.createOrReadInitialCas(getSourceDocument(), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            searchService.indexDocument(getSourceDocument(), casToByteArray(cas));
        }
        catch (IOException e) {
            log.error("Error indexing source document {}", getSourceDocument(), e);
        }

        done++;
    }

    @Override
    public Progress getProgress()
    {
        return new Progress(done, 1);
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a re-indexing task for the project is scheduled, we do not need to schedule a new
        // source indexing task
        if (aTask instanceof ReindexTask) {
            if (Objects.equals(((ReindexTask) aTask).getProject().getId(),
                    getSourceDocument().getProject().getId())) {
                return DISCARD_OR_QUEUE_THIS;
            }
        }

        if (aTask instanceof IndexSourceDocumentTask) {
            if (Objects.equals(getSourceDocument().getId(),
                    ((IndexSourceDocumentTask) aTask).getSourceDocument().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
            }
        }

        return NO_MATCH;
    }
}
