/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfAnnoPanel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfAnnoRenderer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;

public class PdfAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;

    public PdfAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider)
    {
        super(aId, aModel, aActionHandler, aJCasProvider);

        add(new PdfAnnoPanel("vis", aModel, this));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
     
//        if (getModelObject().getDocument() != null) {
//            requestRender(RequestCycle.get().find(AjaxRequestTarget.class));
//        }
    }
    
    @Override
    public void render(AjaxRequestTarget aTarget)
    {
//        renderedContent = aJCas.getDocumentText();
        
//        if (aTarget != null) {
//            aTarget.add(vis);
//        }
    }

    private void handleError(String aMessage, Throwable aCause)
    {
        LOG.error(aMessage, aCause);
        error(aMessage + ExceptionUtils.getRootCauseMessage(aCause));
        return;
    }

    /**
     * Renders the PdfAnnoModel.
     * This includes the anno file and the color map.
     * @param pdftxt Output string of PDFExtract
     */
    public PdfAnnoModel renderPdfAnnoModel(String pdftxt)
    {
        if (getModelObject().getProject() != null)
        {
            JCas jCas;
            try
            {
                jCas = getJCasProvider().get();
            }
            catch (IOException e)
            {
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                return null;
            }
            PdfExtractFile pdfExtractFile = new PdfExtractFile(pdftxt);
            VDocument vdoc = render(jCas, 0, jCas.getDocumentText().length());
            PdfAnnoModel pdfAnnoModel = PdfAnnoRenderer.render(getModelObject(),
                vdoc, jCas.getDocumentText(), annotationService, pdfExtractFile);
            return pdfAnnoModel;
        }
        return null;
    }
}
