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
package de.tudarmstadt.ukp.inception.assistant.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getCsrfTokenFromSession;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.AssistantService;
import de.tudarmstadt.ukp.inception.assistant.AssistantWebsocketController;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import jakarta.servlet.ServletContext;

public class AssistantPanel
    extends WebMarkupContainer
{
    private static final long serialVersionUID = -9006607500867612027L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean ServletContext servletContext;
    private @SpringBean UserDao userService;
    private @SpringBean AssistantService assistantService;

    private DiamAjaxBehavior diamBehavior;

    public AssistantPanel(String aId)
    {
        super(aId);
        setOutputMarkupPlaceholderTag(true);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add(new SvelteBehavior());

        add(diamBehavior = new DiamAjaxBehavior(null));
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        var state = findParent(AnnotationPageBase.class).getModelObject();

        if (state.getDocument() == null) {
            setDefaultModel(null);
            return;
        }

        Map<String, Object> properties = Map.of( //
                "ajaxEndpointUrl", diamBehavior.getCallbackUrl(), //
                "wsEndpointUrl", constructEndpointUrl(), //
                "csrfToken", getCsrfTokenFromSession(), //
                "topicChannel", AssistantWebsocketController.getChannel(state.getProject()), //
                "dataOwner", state.getUser().getUsername(), //
                "documentId", state.getDocument().getId());

        // model will be added as props to Svelte component
        setDefaultModel(Model.ofMap(properties));
    }

    private String constructEndpointUrl()
    {
        var endPointUrl = Url.parse(format("%s%s", servletContext.getContextPath(), WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        var fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
        return fullUrl;
    }
}
