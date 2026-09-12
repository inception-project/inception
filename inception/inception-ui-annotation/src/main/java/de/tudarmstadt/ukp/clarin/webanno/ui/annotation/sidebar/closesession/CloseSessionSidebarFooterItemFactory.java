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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.closesession;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarFooterItemFactory_ImplBase;
import de.tudarmstadt.ukp.inception.ui.core.menubar.MenuBar;

/**
 * Offers a way out of the session from the sidebar footer if the menu bar is not visible.
 */
@Order(ActionBarExtension.ORDER_CLOSE_SESSION)
public class CloseSessionSidebarFooterItemFactory
    extends SidebarFooterItemFactory_ImplBase
{
    @Override
    public boolean accepts(AnnotationPageBase aContext)
    {
        return aContext.visitChildren(MenuBar.class, (c, v) -> {
            v.stop(!((MenuBar) c).isVisible());
        });
    }

    @Override
    public Panel create(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        return new CloseSessionSidebarFooterItem(aId);
    }
}
