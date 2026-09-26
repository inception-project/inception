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
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarContext;
import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ActiveLearningAutoConfiguration#activeLearningSidebarFactory}.
 * </p>
 */
@Order(4000)
public class ActiveLearningSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private final RecommendationService recommendationService;
    private final WorkloadManagementService workloadManagementService;

    public ActiveLearningSidebarFactory(RecommendationService aRecommendationService,
            WorkloadManagementService aWorkloadManagementService)
    {
        recommendationService = aRecommendationService;
        workloadManagementService = aWorkloadManagementService;
    }

    @Override
    public String getDisplayName()
    {
        return "Active Learning";
    }

    @Override
    public String getDescription()
    {
        return "Guides the user through recommendations to be reviewed. Only available if the "
                + "project contains enabled recommenders and the user may open documents in any "
                + "order.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorViewState> aState)
    {
        return new ActiveLearningSidebarIcon(aId, aState);
    }

    @Override
    public boolean available(Project aProject)
    {
        return recommendationService.existsEnabledRecommender(aProject) && workloadManagementService
                .getWorkloadManagerExtension(aProject).isDocumentRandomAccessAllowed(aProject);
    }

    @Override
    public boolean accepts(SidebarContext aContext)
    {
        return super.accepts(aContext) && aContext.page() instanceof AnnotationPage;
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, SidebarContext aContext)
    {
        return new ActiveLearningSidebar(aId, aContext);
    }
}
