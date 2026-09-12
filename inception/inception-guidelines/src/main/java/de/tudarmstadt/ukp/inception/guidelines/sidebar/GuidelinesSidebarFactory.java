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
package de.tudarmstadt.ukp.inception.guidelines.sidebar;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.guidelines.GuidelinesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;

@Order(4600)
public class GuidelinesSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
{
    private final GuidelinesService guidelinesService;

    public GuidelinesSidebarFactory(GuidelinesService aGuidelinesService)
    {
        guidelinesService = aGuidelinesService;
    }

    @Override
    public String getDisplayName()
    {
        return "Guidelines";
    }

    @Override
    public String getDescription()
    {
        return "Lists the annotation guidelines uploaded for this project.";
    }

    @Override
    public Component createIcon(String aId, IModel<AnnotatorViewState> aState)
    {
        return new Icon(aId, FontAwesome7IconType.book_open_s);
    }

    @Override
    public boolean available(Project aProject)
    {
        return guidelinesService.hasGuidelines(aProject);
    }

    @Override
    public boolean accepts(AnnotationPageBase aContext)
    {
        // Hide the tab entirely if the project has no guidelines to show.
        var project = aContext.getModelObject().getProject();
        return project != null && guidelinesService.hasGuidelines(project);
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        return new GuidelinesSidebar(aId, aAnnotationPage);
    }
}
