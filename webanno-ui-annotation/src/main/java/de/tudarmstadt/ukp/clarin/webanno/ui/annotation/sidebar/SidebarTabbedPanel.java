/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

public class SidebarTabbedPanel<T extends SidebarTab>
    extends AjaxTabbedPanel<T>
{
    private static final long serialVersionUID = -5077247310722334915L;

    private static final UrlResourceReference ICON_EXPANDED = new UrlResourceReference(
            Url.parse("images/bullet_arrow_left.png")).setContextRelative(true);

    private static final UrlResourceReference ICON_COLLAPSED = new UrlResourceReference(
            Url.parse("images/bullet_arrow_right.png")).setContextRelative(true);

    private boolean expanded = false;
    private Image icon;

    public SidebarTabbedPanel(String aId, List<T> aTabs)
    {
        super(aId, aTabs);
        
        setOutputMarkupPlaceholderTag(true);
        setOutputMarkupId(true);
        setVisible(!aTabs.isEmpty());
        
        LambdaAjaxLink showHideLink = new LambdaAjaxLink("showHideLink", this::showHideAction);
        
        showHideLink.add(icon = new Image("showHideIcon", ICON_EXPANDED));
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
    protected void onConfigure()
    {
        super.onConfigure();
        
        icon.setImageResourceReference(expanded ? ICON_EXPANDED : ICON_COLLAPSED);
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
        return new Image("image", tab.getIcon())
        {
            private static final long serialVersionUID = 1207122722078977614L;

            {
                add(new AttributeModifier("title", aTitleModel));
            }
            
            @Override
            protected boolean shouldAddAntiCacheParameter()
            {
                return false;
            }
        };
    }
}
