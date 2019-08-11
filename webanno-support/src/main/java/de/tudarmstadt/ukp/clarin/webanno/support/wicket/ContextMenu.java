/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;

import com.googlecode.wicket.jquery.ui.widget.menu.ContextMenuBehavior;

public class ContextMenu
    extends com.googlecode.wicket.jquery.ui.widget.menu.ContextMenu
{
    private static final long serialVersionUID = -1839334030165463085L;

    public ContextMenu(String aId)
    {
        super(aId);
    }

    /**
     * Fired by a component that holds a {@link ContextMenuBehavior}
     *
     * @param target
     *            the {@link AjaxRequestTarget}
     */
    public void onOpen(AjaxRequestTarget target)
    {
        onContextMenu(target, null);

        final IRequestParameters request = getRequest().getPostParameters();

        int clientX = request.getParameterValue("clientX").toInt();
        int clientY = request.getParameterValue("clientY").toInt();

        target.add(this);
        target.appendJavaScript(String.format(
                "jQuery('%s').show().css({position:'fixed', left:'%dpx', top:'%dpx'});",
                JQueryWidget.getSelector(this), clientX, clientY));
    }
}
