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

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

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

    public ActiveLearningSidebarFactory(RecommendationService aRecommendationService)
    {
        recommendationService = aRecommendationService;
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
                + "project contains enabled recommenders.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorState> aState)
    {
        return new ActiveLearningSidebarIcon(aId, aState);
    }

    @Override
    public boolean available(Project aProject)
    {
        return recommendationService.existsEnabledRecommender(aProject);
    }

    @Override
    public boolean applies(AnnotatorState aState)
    {
        return available(aState.getProject());
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        return new ActiveLearningSidebar(aId, aActionHandler, aCasProvider, aAnnotationPage);
    }
}
