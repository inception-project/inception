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

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Stream;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.EventType;

public class DashboardMenu
    extends Panel
{
    private static final long serialVersionUID = 8582941766827165724L;

    private @SpringBean UserDao userRepository;
    private @SpringBean PreferencesService userPrefService;

    private WebMarkupContainer spacer;
    private WebMarkupContainer wrapper;
    private IModel<Boolean> pinState;
    private LambdaAjaxLink pin;

    public static final PreferenceKey<PinState> KEY_PINNED = new PreferenceKey<>(PinState.class,
            "dashboard-menus/pinned");

    public DashboardMenu(String aId, final IModel<List<MenuItem>> aModel)
    {
        this(aId, aModel, true);

        var user = userRepository.getCurrentUser();
        pinState = new LambdaModelAdapter.Builder<Boolean>() //
                .getting(() -> userPrefService.loadTraitsForUser(KEY_PINNED, user).isPinned)
                .setting(v -> userPrefService.saveTraitsForUser(KEY_PINNED, user, new PinState(v)))
                .build();
    }

    public DashboardMenu(String aId, final IModel<List<MenuItem>> aModel, boolean aInitialState)
    {
        this(aId, aModel, Model.of(aInitialState));
    }

    public DashboardMenu(String aId, final IModel<List<MenuItem>> aModel, IModel<Boolean> aPinState)
    {
        super(aId, aModel);
        pinState = aPinState;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        setOutputMarkupId(true);

        spacer = new WebMarkupContainer("spacer");
        spacer.add(LambdaBehavior.visibleWhen(pinState.map(flag -> !flag)));
        add(spacer);

        wrapper = new WebMarkupContainer("wrapper");
        wrapper.add(AttributeModifier.append("class",
                pinState.map(flag -> flag ? "" : "collapsed expand-on-hover")));

        pin = new LambdaAjaxLink("pin", this::actionTogglePin);
        pin.add(AttributeModifier.append("class", pinState.map(flag -> flag ? "active" : "")));
        wrapper.add(pin);

        wrapper.add(new ListView<MenuItem>("items", getModel())
        {
            private static final long serialVersionUID = 6345129880666905375L;

            @Override
            protected void populateItem(ListItem<MenuItem> aItem)
            {
                generateMenuItem(aItem);
            }
        });

        add(wrapper);
    }

    @SuppressWarnings("unchecked")
    public IModel<List<MenuItem>> getModel()
    {
        return (IModel<List<MenuItem>>) getDefaultModel();
    }

    public IModel<Boolean> getPinState()
    {
        return pinState;
    }

    public void setPinState(IModel<Boolean> aPinState)
    {
        pinState = aPinState;
    }

    public DashboardMenu setPinnable(boolean aPinnable)
    {
        pin.setVisible(aPinnable);
        return this;
    }

    private void actionTogglePin(AjaxRequestTarget aTarget)
    {
        pinState.setObject(!pinState.getObject());
        aTarget.add(this);
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
                        "Menu item targeting a specific project must be on a project page");
            }

            Project project = currentPage.getProject();

            aItem.setVisible(projectMenuItem.applies(currentPage.getProject()));

            var pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            menulink = new BookmarkablePageLink<>("item", pageClass, pageParameters);
        }
        else {
            menulink = new BookmarkablePageLink<>("item", pageClass);
        }

        menulink.add(item.getIcon("icon"));
        menulink.add(new Label("label", item.getLabel()));
        menulink.add(AttributeAppender.append("class",
                () -> getPage().getClass().equals(pageClass) ? "active" : ""));
        if (item.shortcut() != null && item.shortcut().length > 0) {
            menulink.add(new InputBehavior(item.shortcut(), EventType.click)
            {
                private static final long serialVersionUID = -3230776977218522942L;

                @Override
                protected Boolean getDisable_in_input()
                {
                    return true;
                };
            });
            menulink.add(AttributeModifier.append("title",
                    "[" + Stream.of(item.shortcut()).map(Object::toString).collect(joining(" + "))
                            + "]"));
        }
        aItem.add(menulink);
    }
}
