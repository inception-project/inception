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
package de.tudarmstadt.ukp.inception.diam.debugeditor;

import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.diam.model.ViewportDefinition;
import de.tudarmstadt.ukp.inception.support.axios.AxiosResourceReference;
import de.tudarmstadt.ukp.inception.support.vue.VueComponent;

public class DiamDebugEditorComponent
    extends VueComponent
{
    private static final long serialVersionUID = -3927310514831796946L;

    private @SpringBean ServletContext servletContext;

    private IModel<AnnotatorState> state;

    public DiamDebugEditorComponent(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, "DiamDebugEditorComponent.vue");
        setOutputMarkupPlaceholderTag(true);
        state = aModel;

    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        Map<String, Object> properties = Map.of( //
                "wsEndpoint", constructEndpointUrl(), //
                "topicChannel",
                new ViewportDefinition(state.getObject().getDocument(),
                        state.getObject().getUser().getUsername(), 0, Integer.MAX_VALUE)
                                .getTopic());

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

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(JSONPatchResourceReference.get()));
        aResponse.render(forReference(new WebjarsJavaScriptResourceReference(
                "webstomp-client/current/dist/webstomp.min.js")));
        aResponse.render(forReference(AxiosResourceReference.get()));
    }
}
