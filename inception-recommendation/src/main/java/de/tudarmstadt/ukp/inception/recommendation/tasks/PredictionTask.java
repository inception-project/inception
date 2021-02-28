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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends Task
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;

    private final SourceDocument currentDocument;
    private final List<LogMessage> logMessages = new ArrayList<>();

    public PredictionTask(User aUser, Project aProject, String aTrigger,
            SourceDocument aCurrentDocument)
    {
        super(aUser, aProject, aTrigger);
        currentDocument = aCurrentDocument;
    }

    @Override
    public void run()
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            User user = getUser();
            String username = user.getUsername();

            Project project = getProject();

            List<SourceDocument> docs = documentService.listSourceDocuments(project);
            List<SourceDocument> inherit = Collections.emptyList();

            // Limit prediction to a single document and inherit the rest?
            if (!recommendationService.isPredictForAllDocuments(username, project)) {
                inherit = docs.stream().filter(d -> !d.equals(currentDocument))
                        .collect(Collectors.toList());
                docs = Collections.singletonList(currentDocument);
                log.debug("[{}][{}]: Limiting prediction to [{}]", getId(), username,
                        currentDocument.getName());
            }

            log.debug(
                    "[{}][{}]: Starting prediction for project [{}] on [{}] docs triggered by [{}]",
                    getId(), username, project, docs.size(), getTrigger());

            long startTime = System.currentTimeMillis();

            Predictions predictions = recommendationService.computePredictions(user, project, docs,
                    inherit);
            predictions.inheritLog(logMessages);

            log.debug("[{}][{}]: Prediction complete ({} ms)", getId(), username,
                    (System.currentTimeMillis() - startTime));

            recommendationService.putIncomingPredictions(user, project, predictions);
        }
    }

    public void inheritLog(List<LogMessage> aLogMessages)
    {
        logMessages.addAll(aLogMessages);
    }
}
