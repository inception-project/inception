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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.docnav.DocumentNavigator;

@Order(ActionBarExtension.ORDER_DOCUMENT_NAVIGATOR)
@Component
public class AnnotationDocumentNavigatorActionBarExtension
    implements ActionBarExtension
{
    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
    }

    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        return aContext.page() instanceof AnnotationPage;
    }

    @Override
    public Panel createActionBarItem(String aId, ActionBarContext aContext)
    {
        return new DocumentNavigator(aId, aContext.page());
    }
}
