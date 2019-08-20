/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
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

    public PredictionTask(User aUser, Project aProject, String aTrigger)
    {
        super(aUser, aProject, aTrigger);
    }

    @Override
    public void run()
    {
        User user = getUser();

        Project project = getProject();
        List<SourceDocument> docs = documentService.listSourceDocuments(project);

        log.debug("[{}][{}]: Starting prediction for project [{}] triggered by [{}]...", getId(),
                user.getUsername(), project, getTrigger());
        
        long startTime = System.currentTimeMillis();

        Predictions predictions = recommendationService.computePredictions(user, project, docs);
        
        log.debug("[{}][{}]: Prediction complete ({} ms)", getId(), user.getUsername(),
                (System.currentTimeMillis() - startTime));

        recommendationService.putIncomingPredictions(user, project, predictions);
    }
}
