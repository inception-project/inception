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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType.chevron_left_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType.chevron_right_s;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.preferences.Key;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class SidebarTabbedPanel<T extends SidebarTab>
    extends AjaxTabbedPanel<T>
{
    private static final long serialVersionUID = -5077247310722334915L;

    public static final Key<AnnotationSidebarState> KEY_SIDEBAR_STATE = new Key<>(
            AnnotationSidebarState.class, "annotation/left-sidebar");

    private boolean expanded = false;

    private @SpringBean UserDao userService;
    private @SpringBean PreferencesService prefService;

    private IModel<AnnotatorState> state;

    public SidebarTabbedPanel(String aId, List<T> aTabs, IModel<AnnotatorState> aState)
    {
        super(aId, aTabs);

        state = aState;

        setOutputMarkupPlaceholderTag(true);
        setOutputMarkupId(true);
        setVisible(!aTabs.isEmpty());

        LambdaAjaxLink showHideLink = new LambdaAjaxLink("showHideLink", this::showHideAction);

        showHideLink.add(new Icon("showHideIcon",
                LoadableDetachableModel.of(() -> isExpanded() ? chevron_left_s : chevron_right_s)));
        ((WebMarkupContainer) get("tabs-container")).add(showHideLink);

        loadSidebarState();
    }

    private void showHideAction(AjaxRequestTarget aTarget)
    {
        expanded = !expanded;
        saveSidebarState();
        WicketUtil.refreshPage(aTarget, getPage());
    }

    public boolean isExpanded()
    {
        return expanded;
    }

    @Override
    protected void onBeforeRender()
    {
        super.onBeforeRender();
        get("panel").setVisible(expanded);
    }

    @Override
    protected void onAjaxUpdate(Optional<AjaxRequestTarget> aTarget)
    {
        super.onAjaxUpdate(aTarget);
        if (!expanded) {
            expanded = true;
            saveSidebarState();
            aTarget.ifPresent(_target -> WicketUtil.refreshPage(_target, getPage()));
        }
    }

    @Override
    public TabbedPanel<T> setSelectedTab(int aIndex)
    {
        TabbedPanel<T> t = super.setSelectedTab(aIndex);
        saveSidebarState();
        return t;
    }

    private void saveSidebarState()
    {
        AnnotationSidebarState sidebarState = new AnnotationSidebarState();
        sidebarState.setSelectedTab(getTabs().get(getSelectedTab()).getFactoryId());
        sidebarState.setExpanded(expanded);
        User user = userService.getCurrentUser();
        prefService.saveTraitsForUserAndProject(KEY_SIDEBAR_STATE, user,
                state.getObject().getProject(), sidebarState);
    }

    private void loadSidebarState()
    {
        User user = userService.getCurrentUser();
        AnnotationSidebarState sidebarState = prefService.loadTraitsForUserAndProject(
                KEY_SIDEBAR_STATE, user, state.getObject().getProject());
        if (isNotBlank(sidebarState.getSelectedTab())) {
            var tabFactories = getTabs().stream().map(SidebarTab::getFactoryId)
                    .collect(Collectors.toList());
            var tabIndex = tabFactories.indexOf(sidebarState.getSelectedTab());
            if (tabIndex >= 0) {
                super.setSelectedTab(tabIndex);
                expanded = sidebarState.isExpanded();
            }
        }
    }

    @Override
    protected Component newTitle(String aTitleId, IModel<?> aTitleModel, int aIndex)
    {
        SidebarTab tab = getTabs().get(aIndex);
        Icon icon = new Icon("icon", tab.getIcon());
        icon.add(new AttributeModifier("title", aTitleModel));
        return icon;
    }

    @Override
    protected String getSelectedTabCssClass()
    {
        return "active";
    }

    @Override
    protected String getTabContainerCssClass()
    {
        return "nav flex-column dashboard-sidebar collapsed dashboard-sidebar-light border border-start-0";
    }
}
