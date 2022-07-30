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
package de.tudarmstadt.ukp.inception.externaleditor;

import static de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorLoader.PLUGINS_EDITOR_BASE_URL;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorPluginDescripion;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class ExternalAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final String PLUGIN_SCHEME = "plugin:";

    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean ServletContext context;

    private final String editorFactoryId;

    public ExternalAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        editorFactoryId = aEditorFactoryId;
    }

    @Override
    protected Component makeView()
    {
        if (getDescription().getView().startsWith(PLUGIN_SCHEME)) {
            String resPath = substringAfter(getDescription().getView(), PLUGIN_SCHEME);
            return new ExternalAnnotationEditorStaticIFrameView(CID_VIS,
                    getUrlForPluginAsset(resPath));
        }

        AnnotatorState state = getModelObject();

        AnnotationDocument annDoc = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());

        return documentViewExtensionPoint.getExtension(getDescription().getView()) //
                .map(ext -> ext.createView(CID_VIS, Model.of(annDoc), editorFactoryId)) //
                .orElseGet(() -> new Label(CID_VIS,
                        "Unsupported view: [" + getDescription().getView() + "]"));
    }

    private ExternalEditorPluginDescripion getDescription()
    {
        ExternalAnnotationEditorFactory factory = (ExternalAnnotationEditorFactory) annotationEditorRegistry
                .getEditorFactory(editorFactoryId);
        return factory.getDescription();
    }

    private String getUrlForPluginAsset(String aAssetPath)
    {
        return getUrlForPluginAsset(context, getDescription(), aAssetPath);
    }

    public static String getUrlForPluginAsset(ServletContext aContext,
            ExternalEditorPluginDescripion aPlugin, String aAssetPath)
    {
        return aContext.getContextPath() + PLUGINS_EDITOR_BASE_URL + aPlugin.getId() + "/"
                + aAssetPath;
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        AnnotationEditorProperties props = new AnnotationEditorProperties();
        ExternalEditorPluginDescripion pluginDesc = getDescription();
        props.setEditorFactory(pluginDesc.getFactory());
        props.setDiamAjaxCallbackUrl(getDiamBehavior().getCallbackUrl().toString());
        props.setDiamWsUrl(constructWsEndpointUrl());
        props.setStylesheetSources(pluginDesc.getStylesheets().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));
        props.setScriptSources(pluginDesc.getScripts().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));

        return props;
    }

    private String constructWsEndpointUrl()
    {
        Url endPointUrl = Url.parse(format("%s%s", context.getContextPath(), WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }
}
