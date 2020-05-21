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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import static java.lang.Integer.MAX_VALUE;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.DefaultWorkflowActionBarExtension;

@Order(1000)
@Component
public class CurationWorkflowActionBarExtension implements ActionBarExtension
{
    @Override
    public String getRole()
    {
        return DefaultWorkflowActionBarExtension.class.getName();
    }
    
    @Override
    public int getPriority()
    {
        return MAX_VALUE;
    }
    
    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new CuratorWorkflowActionBarItemGroup(aId, aPage);
    }
}
