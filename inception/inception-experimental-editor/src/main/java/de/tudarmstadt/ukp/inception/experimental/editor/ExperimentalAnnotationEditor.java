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
package de.tudarmstadt.ukp.inception.experimental.editor;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import javax.servlet.ServletContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.experimental.editor.resources.ExperimentalAPIEditorReference;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;

public class ExperimentalAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = 2983502506977571078L;

    private @SpringBean ServletContext servletContext;

    public ExperimentalAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        aResponse.render(JavaScriptHeaderItem
                .forScript("; localStorage.setItem('url','" + constructEndpointUrl() + "')", "0"));
        aResponse.render(forReference(ExperimentalAPIEditorReference.get()));
    }

    private String constructEndpointUrl()
    {
        Url endPointUrl = Url.parse(String.format("%s%s", servletContext.getContextPath(),
                WebsocketConfig.WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {

    }

}
