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
package de.tudarmstadt.ukp.clarin.webanno.ui.menu;

import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.annotationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.curationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_AUTOMATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_CORRECTION;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * Main menu page.
 */
public class MainMenuPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;

    private ListView<MenuItem> menu;

    public MainMenuPage()
    {
        setStatelessHint(true);
        setVersioned(false);

        // In case we restore a saved session, make sure the user actually still exists in the DB.
        // redirect to login page (if no usr is found, admin/admin will be created)
        User user = userRepository.getCurrentUser();
        if (user == null) {
            setResponsePage(LoginPage.class);
        }

        // if not either a curator or annotator, display warning message
        if (!annotationEnabeled(projectService, user, PROJECT_TYPE_ANNOTATION)
                && !annotationEnabeled(projectService, user, PROJECT_TYPE_AUTOMATION)
                && !annotationEnabeled(projectService, user, PROJECT_TYPE_CORRECTION)
                && !curationEnabeled(projectService, user)) {
            warn("You are not member of any projects to annotate or curate.");
        }

        menu = new ListView<MenuItem>("menu",
                LoadableDetachableModel.of(menuItemService::getMenuItems))
        {
            private static final long serialVersionUID = -5492972164756003552L;

            @Override
            protected void populateItem(ListItem<MenuItem> aItem)
            {
                MenuItem item = aItem.getModelObject();
                final Class<? extends Page> pageClass = item.getPageClass();
                BookmarkablePageLink<Void> menulink = new BookmarkablePageLink<>("menulink",
                        pageClass);
                menulink.add(
                        new Image("icon", new UrlResourceReference(Url.parse(item.getIcon()))));
                menulink.add(new Label("label", item.getLabel()));
                menulink.setVisible(item.applies());
                aItem.add(menulink);
            }
        };
        add(menu);
    }
}
