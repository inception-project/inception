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
package de.tudarmstadt.ukp.inception.experimental.editor.diamdebugeditor;

import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.diam.model.compactv2.CompactSerializerV2Impl;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;

public class DiamAnnotationBrowser
    extends WebMarkupContainer
{
    private static final long serialVersionUID = 3956364643964484470L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private @SpringBean ServletContext servletContext;

    private DiamAjaxBehavior diamBehavior;

    public DiamAnnotationBrowser(String aId)
    {
        super(aId);
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

        Map<String, Object> properties = Map.of( //
                "ajaxEndpointUrl", diamBehavior.getCallbackUrl(), //
                "wsEndpointUrl", constructEndpointUrl(), //
                "topicChannel", viewport.getTopic());

        // model will be added as props to vue component
        setDefaultModel(Model.ofMap(properties));
    }

    private String constructEndpointUrl()
    {
        Url endPointUrl = Url.parse(format("%s%s", servletContext.getContextPath(), WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        String fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
        return fullUrl;
    }

    protected DiamAjaxBehavior createDiamBehavior()
    {
        var diam = new DiamAjaxBehavior();
        // diam.addPriorityHandler(new ShowContextMenuHandler());
        return diam;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        aResponse.render(forReference(DiamJavaScriptReference.get()));
    }

}
