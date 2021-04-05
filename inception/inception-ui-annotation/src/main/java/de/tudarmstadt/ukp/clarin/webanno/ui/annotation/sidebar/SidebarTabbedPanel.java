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

import java.util.List;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

public class SidebarTabbedPanel<T extends SidebarTab>
    extends AjaxTabbedPanel<T>
{
    private static final long serialVersionUID = -5077247310722334915L;

    private boolean expanded = false;

    public SidebarTabbedPanel(String aId, List<T> aTabs)
    {
        super(aId, aTabs);

        setOutputMarkupPlaceholderTag(true);
        setOutputMarkupId(true);
        setVisible(!aTabs.isEmpty());

        LambdaAjaxLink showHideLink = new LambdaAjaxLink("showHideLink", this::showHideAction);

        showHideLink.add(new Icon("showHideIcon",
                LoadableDetachableModel.of(() -> isExpanded() ? chevron_left_s : chevron_right_s)));
        ((WebMarkupContainer) get("tabs-container")).add(showHideLink);
    }

    private void showHideAction(AjaxRequestTarget aTarget)
    {
        expanded = !expanded;
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
            aTarget.ifPresent(_target -> _target.add(getPage()));
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
        return "nav flex-column dashboard-sidebar dashboard-sidebar-light border border-left-0";
    }
}
