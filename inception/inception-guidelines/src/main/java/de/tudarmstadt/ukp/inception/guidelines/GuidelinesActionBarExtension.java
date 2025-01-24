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
package de.tudarmstadt.ukp.inception.guidelines;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;

@Order(ActionBarExtension.ORDER_GUIDELINES)
@Component
public class GuidelinesActionBarExtension
    implements ActionBarExtension
{
    private final GuidelinesService guidelinesService;

    public GuidelinesActionBarExtension(GuidelinesService aGuidelinesService)
    {
        super();
        guidelinesService = aGuidelinesService;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        // Hide the guidelines item if there are no guidelines
        return ActionBarExtension.super.accepts(aPage)
                && aPage.getModelObject().getProject() != null
                && guidelinesService.hasGuidelines(aPage.getModelObject().getProject());
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new GuidelinesActionBarItem(aId, aPage);
    }
}
