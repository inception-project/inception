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
package de.tudarmstadt.ukp.inception.ui.core.dashboard;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.UrlResourceReference;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;

public class DashboardMenu
    extends Panel
{
    private static final long serialVersionUID = 8582941766827165724L;

    public DashboardMenu(String aId, final IModel<List<MenuItem>> aModel)
    {
        super(aId, aModel);

        add(new ListView<MenuItem>("items", aModel)
        {
            private static final long serialVersionUID = 6345129880666905375L;

            @Override
            protected void populateItem(ListItem<MenuItem> aItem)
            {
                generateMenuItem(aItem);
            }
        });
    }

    private void generateMenuItem(ListItem<MenuItem> aItem)
    {
        MenuItem item = aItem.getModelObject();
        final Class<? extends Page> pageClass = item.getPageClass();

        Link<Void> menulink;
        if (item instanceof ProjectMenuItem) {
            ProjectMenuItem projectMenuItem = (ProjectMenuItem) item;
            ProjectPageBase currentPage = findParent(ProjectPageBase.class);
            
            if (currentPage == null) {
                throw new IllegalStateException(
                        "Menu item targetting a specific project must be on a project page");
            }
            
            Project project = currentPage.getProject();
            long projectId = project.getId();

            aItem.setVisible(projectMenuItem.applies(currentPage.getProject()));

            menulink = new BookmarkablePageLink<>("item", pageClass,
                    new PageParameters().set(PAGE_PARAM_PROJECT, projectId));
        }
        else {
            menulink = new BookmarkablePageLink<>("item", pageClass);
        }

        UrlResourceReference imageRef = new UrlResourceReference(Url.parse(item.getIcon()));
        imageRef.setContextRelative(true);
        menulink.add(new Image("icon", imageRef));
        menulink.add(new Label("label", item.getLabel()));
        aItem.add(menulink);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssHeaderItem
                .forReference(new WebjarsCssResourceReference("hover/current/css/hover.css")));
    }
}
