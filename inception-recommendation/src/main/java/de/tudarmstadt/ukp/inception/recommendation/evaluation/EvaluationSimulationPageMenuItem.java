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
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#evaluationSimulationPageMenuItem()}.
 * </p>
 */
@Order(300)
public class EvaluationSimulationPageMenuItem
    implements ProjectMenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;
    private @Autowired RecommendationService recommenderService;

    @Override
    public String getPath()
    {
        return "/evaluation";
    }

    @Override
    public String getIcon()
    {
        return "images/chart_line.png";
    }

    @Override
    public String getLabel()
    {
        return "Evaluation";
    }

    @Override
    public boolean applies(Project aProject)
    {
        // Visible if the current user is a curator
        User user = userRepo.getCurrentUser();
        if (!(projectService.isManager(aProject, user))) {
            return false;
        }

        return !recommenderService.listRecommenders(aProject).isEmpty();
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return EvaluationSimulationPage.class;
    }
}
