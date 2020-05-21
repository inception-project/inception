/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.guidelines;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;

@Order(800)
@Component
public class GuidelinesActionBarExtension implements ActionBarExtension
{
    private final ProjectService projectService;

    public GuidelinesActionBarExtension(ProjectService aProjectService)
    {
        super();
        projectService = aProjectService;
    }
    
    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        // Hide the guidelines item if there are no guidelines
        return ActionBarExtension.super.accepts(aPage)
                && aPage.getModelObject().getProject() != null
                && projectService.hasGuidelines(aPage.getModelObject().getProject());
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new GuidelinesActionBarItem(aId, aPage);
    }
}
