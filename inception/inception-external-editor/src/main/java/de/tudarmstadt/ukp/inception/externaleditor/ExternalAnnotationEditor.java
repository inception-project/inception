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

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;
import static de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorLoader.PLUGINS_EDITOR_BASE_URL;
import static java.util.stream.Collectors.toList;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorPluginDescripion;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.externaleditor.resources.ExternalEditorJavascriptResourceReference;

public class ExternalAnnotationEditor
    extends AnnotationEditorBase
{
    private static final String PLUGIN_SCHEME = "plugin:";

    private static final String MID_VIS = "vis";

    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean ServletContext context;

    private final String editorFactoryId;

    private DiamAjaxBehavior diamBehavior;
    private Component vis;

    public ExternalAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        setOutputMarkupPlaceholderTag(true);

        editorFactoryId = aEditorFactoryId;

        add(diamBehavior = new DiamAjaxBehavior());

        vis = makeView();
        vis.setOutputMarkupPlaceholderTag(true);
        add(vis);
    }

    private Component makeView()
    {
        if (getDescription().getView().startsWith(PLUGIN_SCHEME)) {
            String resPath = StringUtils.substringAfter(getDescription().getView(), PLUGIN_SCHEME);
            return new ExternalAnnotationEditorStaticIFrameView(MID_VIS,
                    getUrlForPluginAsset(resPath));
        }

        AnnotatorState state = getModelObject();

        AnnotationDocument annDoc = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());

        return documentViewExtensionPoint.getExtension(getDescription().getView()) //
                .map(ext -> ext.createView(MID_VIS, Model.of(annDoc), editorFactoryId)) //
                .orElseGet(() -> new Label(MID_VIS,
                        "Unsupported view: [" + getDescription().getView() + "]"));
    }

    private ExternalEditorPluginDescripion getDescription()
    {
        ExternalAnnotationEditorFactory factory = (ExternalAnnotationEditorFactory) annotationEditorRegistry
                .getEditorFactory(editorFactoryId);
        return factory.getDescription();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(JavaScriptHeaderItem.forReference(DiamJavaScriptReference.get()));

        aResponse.render(
                JavaScriptHeaderItem.forReference(ExternalEditorJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null && getProperties() != null) {
            aResponse.render(OnDomReadyHeaderItem.forScript(initScript()));
        }
    }

    @Override
    protected void onRemove()
    {
        getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(destroyScript()));

        super.onRemove();
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

    private String getProperties()
    {
        AnnotationEditorProperties props = new AnnotationEditorProperties();
        ExternalEditorPluginDescripion pluginDesc = getDescription();
        props.setEditorFactory(pluginDesc.getFactory());
        props.setDiamAjaxCallbackUrl(diamBehavior.getCallbackUrl().toString());
        props.setStylesheetSources(pluginDesc.getStylesheets().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));
        props.setScriptSources(pluginDesc.getScripts().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));

        try {
            return JSONUtil.toInterpretableJsonString(props);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private CharSequence destroyScript()
    {
        return wrapInTryCatch(
                "ExternalEditor.destroy(document.getElementById('" + vis.getMarkupId() + "'));");
    }

    private String initScript()
    {
        return wrapInTryCatch("ExternalEditor.getOrInitialize(document.getElementById('"
                + vis.getMarkupId() + "'), Diam.factory(), " + getProperties() + ");");
    }

    private String renderScript()
    {
        return wrapInTryCatch("ExternalEditor.getOrInitialize(document.getElementById('"
                + vis.getMarkupId() + "'), Diam.factory(), " + getProperties()
                + ").then(e => e.loadAnnotations());");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }
}
