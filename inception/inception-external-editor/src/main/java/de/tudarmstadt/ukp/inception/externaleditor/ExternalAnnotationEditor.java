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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.externaleditor.config.EditorPluginDescripion;

public class ExternalAnnotationEditor
    extends AnnotationEditorBase
{
    private static final String MID_VIS = "vis";

    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;

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
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        AnnotatorState state = getModelObject();

        AnnotationDocument annDoc = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());

        vis = documentViewExtensionPoint.getExtension(getDescription().getView()) //
                .map(ext -> ext.createView(MID_VIS, Model.of(annDoc))) //
                .orElseGet(() -> new Label(MID_VIS,
                        "Unsupported view: [" + getDescription().getView() + "]"));
        vis.setOutputMarkupPlaceholderTag(true);
        add(vis);

        add(diamBehavior = new DiamAjaxBehavior());
    }

    private EditorPluginDescripion getDescription()
    {
        ExternalAnnotationEditorFactory factory = (ExternalAnnotationEditorFactory) annotationEditorRegistry
                .getEditorFactory(editorFactoryId);
        return factory.getDescription();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        EditorPluginDescripion description = getDescription();

        aResponse.render(JavaScriptHeaderItem.forReference(DiamJavaScriptReference.get()));

        Path jsPath = description.getBasePath().resolve(description.getJs());
        FileSystemResourceReference jsReference = new FileSystemResourceReference(
                description.getFactory() + ".js", jsPath);
        aResponse.render(JavaScriptHeaderItem.forReference(jsReference));

        Path cssPath = description.getBasePath().resolve(description.getCss());
        if (Files.isRegularFile(cssPath)) {
            FileSystemResourceReference cssReference = new FileSystemResourceReference(
                    description.getFactory() + ".css", cssPath);
            aResponse.render(CssHeaderItem.forReference(cssReference));
        }

        if (getModelObject().getDocument() != null) {
            aResponse.render(OnDomReadyHeaderItem.forScript(initScript()));
        }
    }

    @Override
    protected void onRemove()
    {
        super.onRemove();

        getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(destroyScript()));
    }

    private CharSequence destroyScript()
    {
        return wrapInTryCatch(
                getDescription().getFactory() + ".destroy('" + vis.getMarkupId() + "');");
    }

    private String initScript()
    {
        String callbackUrl = diamBehavior.getCallbackUrl().toString();
        return wrapInTryCatch(getDescription().getFactory() + ".getOrInitialize('"
                + vis.getMarkupId() + "', Diam.factory(), '" + callbackUrl + "');");
    }

    private String renderScript()
    {
        String callbackUrl = diamBehavior.getCallbackUrl().toString();
        return wrapInTryCatch(
                getDescription().getFactory() + ".getOrInitialize('" + vis.getMarkupId()
                        + "', Diam.factory(), '" + callbackUrl + "').loadAnnotations();");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }
}
