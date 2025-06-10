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
package de.tudarmstadt.ukp.inception.recogitojseditor;

import static de.tudarmstadt.ukp.inception.support.wicket.ServletContextUtils.referenceToUrl;
import static java.util.Arrays.asList;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewFactory;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorBase;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsCssResourceReference;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsJavascriptResourceReference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import jakarta.servlet.ServletContext;

public class RecogitoHtmlAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean(name = "recogitoHtmlAnnotationEditorFactory") AnnotationEditorFactory editorFactory;
    private @SpringBean(name = "xHtmlXmlDocumentIFrameViewFactory") DocumentViewFactory viewFactory;
    private @SpringBean DocumentService documentService;
    private @SpringBean ServletContext servletContext;

    public RecogitoHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aEditorFactoryId);
    }

    @Override
    protected Component makeView()
    {
        AnnotatorState state = getModelObject();

        return viewFactory.createView(CID_VIS, Model.of(state.getDocument()),
                editorFactory.getBeanName());
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        var props = super.getProperties();
        // The factory is the JS call. Cf. the "globalName" in build.js and the factory method
        // defined in main.ts
        props.setEditorFactory("RecogitoEditor.factory()");
        props.setStylesheetSources(
                asList(referenceToUrl(servletContext, RecogitoJsCssResourceReference.get())));
        props.setScriptSources(asList(
                referenceToUrl(servletContext, RecogitoJsJavascriptResourceReference.get())));
        return props;
    }
}
