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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.addeditor;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarFooterItemFactory_ImplBase;

/**
 * Offers a way to split the page into a second editor, and to unsplit it again, from the sidebar
 * footer.
 */
@Order(ActionBarExtension.ORDER_SETTINGS - 1000)
public class AddEditorSidebarFooterItemFactory
    extends SidebarFooterItemFactory_ImplBase
{
    @Override
    public boolean accepts(AnnotationPageBase aContext)
    {
        if (!(aContext instanceof AnnotationPageBase2 page)) {
            return false;
        }

        return page.getWorkspace().getMaxEditors() > 1;
    }

    @Override
    public Panel create(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        return new AddEditorSidebarFooterItem(aId, aAnnotationPage);
    }
}
