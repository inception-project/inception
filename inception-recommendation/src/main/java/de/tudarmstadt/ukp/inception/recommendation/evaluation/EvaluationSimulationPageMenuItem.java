/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;

@Component
@Order(300)
public class EvaluationSimulationPageMenuItem
    implements MenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;

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

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies()
    {
        return SecurityUtil.monitoringEnabeled(projectService, userRepo.getCurrentUser());
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return EvaluationSimulationPage.class;
    }
}
