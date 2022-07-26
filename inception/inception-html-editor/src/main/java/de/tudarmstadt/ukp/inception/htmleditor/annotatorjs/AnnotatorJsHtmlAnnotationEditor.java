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
package de.tudarmstadt.ukp.inception.htmleditor.annotatorjs;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.ServletContextUtils.referenceToUrl;
import static java.util.Arrays.asList;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewFactory;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorBase;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsCssResourceReference;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsJavascriptResourceReference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class AnnotatorJsHtmlAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean(name = "htmlAnnotationEditorFactory") AnnotationEditorFactory editorFactory;
    private @SpringBean(name = "xHtmlXmlDocumentIFrameViewFactory") DocumentViewFactory viewFactory;
    private @SpringBean DocumentService documentService;
    private @SpringBean ServletContext servletContext;

    public AnnotatorJsHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    protected Component makeView()
    {
        AnnotatorState state = getModelObject();

        AnnotationDocument annDoc = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());

        return viewFactory.createView(CID_VIS, Model.of(annDoc), editorFactory.getBeanName());
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        AnnotationEditorProperties props = new AnnotationEditorProperties();
        // The factory is the JS call. Cf. the "globalName" in build.js and the factory method
        // defined in main.ts
        props.setEditorFactory("AnnotatorJsEditor.factory()");
        props.setDiamAjaxCallbackUrl(getDiamBehavior().getCallbackUrl().toString());
        props.setStylesheetSources(
                asList(referenceToUrl(servletContext, AnnotatorJsCssResourceReference.get())));
        props.setScriptSources(asList(
                referenceToUrl(servletContext, AnnotatorJsJavascriptResourceReference.get())));
        return props;
    }
}
