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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet;

import java.util.Objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;

import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public class ClientUrlAjaxBehavior
    extends AbstractDefaultAjaxBehavior
{
    private static final long serialVersionUID = 2932789184741205389L;

    private String clientUrl;

    @Override
    protected void onBind()
    {
        super.onBind();
        if (!(getComponent() instanceof Label)) {
            throw new IllegalArgumentException("The component must be a Label.");
        }
        getComponent().setOutputMarkupId(true);
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);

        aResponse.render(OnDomReadyHeaderItem.forScript(getJsScript()));
    }

    @Override
    protected void respond(AjaxRequestTarget target)
    {
        if (getComponent().getRequest() instanceof ServletWebRequest request) {
            var oldClientUrl = clientUrl;
            clientUrl = request.getPostParameters().getParameterValue("url").toString();
            if (!Objects.equals(oldClientUrl, clientUrl)) {
                WicketUtil.refreshPage(target, getComponent().getPage());
            }
        }
    }

    public String getClientUrl()
    {
        return clientUrl;
    }

    private CharSequence getJsScript()
    {
        return "let currentUrl = window.location.origin + window.location.pathname;\n" //
                + "Wicket.Ajax.ajax({\n" //
                + "    m: 'post',\n" //
                + "    c: '" + getComponent().getMarkupId() + "',\n" //
                + "    u: '" + getCallbackUrl() + "',\n" //
                + "    ep: { url: currentUrl },\n" //
                + "    sh: [],\n" //
                + "    fh: []\n" //
                + "});\n" //
                + "document.getElementById('" + getComponent().getMarkupId()
                + "').innerText = currentUrl;\n";
    }
}
