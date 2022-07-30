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
package de.tudarmstadt.ukp.inception.pdfeditor2;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.ServletContextUtils.referenceToUrl;
import static java.util.Arrays.asList;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewFactory;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorBase;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.pdfeditor2.format.PdfFormatSupport;
import de.tudarmstadt.ukp.inception.pdfeditor2.resources.PdfAnnotationEditorCssResourceReference;
import de.tudarmstadt.ukp.inception.pdfeditor2.resources.PdfAnnotationEditorJavascriptResourceReference;
import de.tudarmstadt.ukp.inception.pdfeditor2.view.PdfDocumentIFrameView;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class PdfAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private static final String VIS = "vis";

    private PdfDocumentIFrameView view;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ColoringService coloringService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean(name = "pdfDocument2IFrameViewFactory") DocumentViewFactory viewFactory;
    private @SpringBean ServletContext servletContext;

    public PdfAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    protected Component makeView()
    {
        AnnotatorState state = getModelObject();
        String format = state.getDocument().getFormat();
        if (!format.equals(PdfFormatSupport.ID)) {
            return new WrongFileFormatPanel(VIS, format);
        }

        AnnotationDocument annDoc = documentService.getAnnotationDocument(state.getDocument(),
                state.getUser());
        view = (PdfDocumentIFrameView) viewFactory.createView(VIS, Model.of(annDoc),
                viewFactory.getId());
        return view;
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        AnnotationEditorProperties props = new AnnotationEditorProperties();
        // The factory is the JS call. Cf. the "globalName" in build.js and the factory method
        // defined in main.ts
        props.setEditorFactory("PdfAnnotationEditor.factory()");
        props.setDiamAjaxCallbackUrl(getDiamBehavior().getCallbackUrl().toString());
        props.setStylesheetSources(asList(
                referenceToUrl(servletContext, PdfAnnotationEditorCssResourceReference.get())));
        props.setScriptSources(asList(referenceToUrl(servletContext,
                PdfAnnotationEditorJavascriptResourceReference.get())));
        return props;
    }
}
