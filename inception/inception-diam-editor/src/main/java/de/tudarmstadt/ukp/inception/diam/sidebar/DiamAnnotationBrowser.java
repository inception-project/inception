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
package de.tudarmstadt.ukp.inception.diam.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getCsrfTokenFromSession;
import static de.tudarmstadt.ukp.inception.diam.sidebar.preferences.DiamSidebarManagerPrefs.KEY_DIAM_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.diam.model.compactv2.CompactSerializerV2Impl;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;
import jakarta.servlet.ServletContext;

public class DiamAnnotationBrowser
    extends WebMarkupContainer
{
    private static final long serialVersionUID = 3956364643964484470L;

    private @SpringBean ServletContext servletContext;
    private @SpringBean PreferencesService userPrefService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private final String userPreferencesKey;
    private final ContextMenu contextMenu;

    private DiamAjaxBehavior diamBehavior;

    public DiamAnnotationBrowser(String aId, String aUserPreferencesKey, ContextMenu aContextMenu)
    {
        super(aId);
        userPreferencesKey = aUserPreferencesKey;
        contextMenu = aContextMenu;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add(diamBehavior = createDiamBehavior());
        add(new SvelteBehavior());
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

        var viewport = new ViewportDefinition(state.getDocument(), state.getUser().getUsername(), 0,
                Integer.MAX_VALUE, CompactSerializerV2Impl.ID);

        var managerPrefs = userPrefService
                .loadDefaultTraitsForProject(KEY_DIAM_SIDEBAR_MANAGER_PREFS, state.getProject());

        Map<String, Object> properties = Map.of( //
                "ajaxEndpointUrl", diamBehavior.getCallbackUrl(), //
                "wsEndpointUrl", constructEndpointUrl(), //
                "csrfToken", getCsrfTokenFromSession(), //
                "topicChannel", viewport.getTopic(), //
                "pinnedGroups", managerPrefs.getPinnedGroups(), //
                "userPreferencesKey", userPreferencesKey);

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

    protected DiamAjaxBehavior createDiamBehavior()
    {
        return new DiamAjaxBehavior(contextMenu);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        aResponse.render(forReference(DiamJavaScriptReference.get()));
    }
}
