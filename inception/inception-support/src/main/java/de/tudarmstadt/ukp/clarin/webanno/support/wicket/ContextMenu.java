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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static java.util.Locale.ROOT;

import org.apache.wicket.Component;
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
        target.appendJavaScript(wrapInTryCatch(format(ROOT,
                "jQuery('%s').show().css({position:'fixed', left:'%dpx', top:'%dpx'});",
                JQueryWidget.getSelector(this), clientX, clientY)));
    }

    /**
     * Fired by a component that holds a {@link ContextMenuBehavior}
     *
     * @param target
     *            the {@link AjaxRequestTarget}
     * @param aComponent
     *            the component that holds a {@link ContextMenuBehavior}
     */
    public void onOpen(AjaxRequestTarget target, Component aComponent)
    {
        onContextMenu(target, aComponent);

        target.add(this);
        target.appendJavaScript(wrapInTryCatch(format(ROOT, "jQuery('%s').show().position(%s);",
                JQueryWidget.getSelector(this), this.getPositionOption(aComponent))));
    }
}
