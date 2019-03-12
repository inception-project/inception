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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValueConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfAnnoPanel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.DocumentModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoRenderer;
import paperai.pdfextract.PDFExtractor;

public class PdfAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

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
    }

    @Override
    public void render(AjaxRequestTarget aTarget)
    {
        try {
            Selection selection = getModelObject().getSelection();
            File pdfFile = documentService.getSourceDocumentFile(getModelObject().getDocument());
            String pdftext = PDFExtractor.processFileToString(pdfFile, false);
            renderPdfAnnoModel(aTarget, pdftext);
            if (selection.getAnnotation() != null) {
                aTarget.appendJavaScript("var anno = pdfanno.contentWindow.annoPage.findAnnotationById("
                    + selection.getAnnotation().toString() + ");"
                    + "anno && anno.select();");
            }
        }
        catch (IOException e)
        {
            handleError("Could not load data", e, aTarget);
        }
    }

    private void handleError(String aMessage, Throwable aCause, AjaxRequestTarget aTarget)
    {
        LOG.error(aMessage, aCause);
        handleError(aMessage + ": " + ExceptionUtils.getRootCauseMessage(aCause), aTarget);
    }

    private void handleError(String aMessage, AjaxRequestTarget aTarget)
    {
        error(aMessage);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    /**
     * Renders the PdfAnnoModel.
     * This includes the anno file and the color map.
     */
    public void renderPdfAnnoModel(AjaxRequestTarget aTarget, PdfExtractFile aPdfExtractFile)
    {
        if (getModelObject().getProject() != null)
        {
            try
            {
                JCas jCas = getJCasProvider().get();
                VDocument vdoc = render(jCas, 0, jCas.getDocumentText().length());
                PdfAnnoModel pdfAnnoModel = PdfAnnoRenderer.render(getModelObject(),
                    vdoc, jCas.getDocumentText(), annotationService, aPdfExtractFile);
                // show unmatched spans to user
                if (pdfAnnoModel.getUnmatchedSpans().size() > 0) {
                    String annotations = pdfAnnoModel.getUnmatchedSpans().stream()
                        .map(span -> "(id: " + span.getId() + ", text: \"" + span.getText() + "\")")
                        .collect(Collectors.joining(", "));
                    handleError("Could not find a match for the following annotations: "
                        + annotations, aTarget);
                }
                String script = getAnnotationsJS(pdfAnnoModel);
                aTarget.appendJavaScript(script);
            }
            catch (IOException e)
            {
                handleError("Unable to load data", e, aTarget);
            }
        }
    }

    /**
     * Renders the PdfAnnoModel.
     * This includes the anno file and the color map.
     */
    public void renderPdfAnnoModel(AjaxRequestTarget aTarget, String aPdftxt)
    {
        renderPdfAnnoModel(aTarget, new PdfExtractFile(aPdftxt));
    }

    public void createSpanAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                     JCas aJCas, PdfExtractFile aPdfExtractFile)
    {
        try
        {
            DocumentModel documentModel = new DocumentModel(aJCas.getDocumentText());
            Offset offset = new Offset(aParams);
            Offset docOffset =
                PdfAnnoRenderer.convertToDocumentOffset(offset, documentModel, aPdfExtractFile);
            if (docOffset.getBegin() > -1 && docOffset.getEnd() > -1) {
                getModelObject().getSelection()
                    .selectSpan(aJCas, docOffset.getBegin(), docOffset.getEnd());
                getActionHandler().actionCreateOrUpdate(aTarget, aJCas);
                renderPdfAnnoModel(aTarget, aPdfExtractFile.getPdftxt());
            } else {
                handleError("Unable to create span annotation: No match was found", aTarget);
            }
        }
        catch (IOException | AnnotationException e)
        {
            handleError("Unable to create span annotation", e, aTarget);
        }
    }

    private void selectSpanAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                      JCas aJCas, PdfExtractFile aPdfExtractFile)
    {
        try
        {
            DocumentModel documentModel = new DocumentModel(aJCas.getDocumentText());
            VID paramId = VID.parseOptional(aParams.getParameterValue("id").toString());
            Offset offset = new Offset(aParams);
            Offset docOffset =
                PdfAnnoRenderer.convertToDocumentOffset(offset, documentModel, aPdfExtractFile);
            if (docOffset.getBegin() > -1 && docOffset.getEnd() > -1) {
                if (paramId.isSynthetic()) {
                    extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                        aTarget, aJCas, paramId, "spanOpenDialog", docOffset.getBegin(),
                        docOffset.getEnd());
                } else {
                    getModelObject().getSelection().selectSpan(paramId, aJCas.getCas(),
                            docOffset.getBegin(), docOffset.getEnd());
                    getActionHandler().actionSelect(aTarget, aJCas);
                }
            } else {
                handleError("Unable to select span annotation: No match was found", aTarget);
            }
        }
        catch (AnnotationException | IOException e)
        {
            handleError("Unable to select span annotation", e, aTarget);
        }
    }

    private void createRelationAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                          JCas aJCas, PdfExtractFile aPdfExtractFile)
        throws IOException
    {
        try {
            AnnotationFS originFs = selectByAddr(aJCas,
                aParams.getParameterValue("origin").toInt());
            int target = aParams.getParameterValue("target").toInt();
            if (target == -1) {
                // if -1 return, relation drawing was not stopped over a target
                return;
            }
            AnnotationFS targetFs = selectByAddr(aJCas,
                target);

            AnnotatorState state = getModelObject();
            Selection selection = state.getSelection();
            selection.selectArc(VID.NONE_ID, originFs, targetFs);

            if (selection.getAnnotation().isNotSet()) {
                getActionHandler().actionCreateOrUpdate(aTarget, aJCas);
            }
        }
        catch (AnnotationException | CASRuntimeException e)
        {
            handleError("Unable to create relation annotation", e, aTarget);
        }
        catch (StringValueConversionException e)
        {
            handleError("Unable to create relations on recommendations", aTarget);
        }
        finally
        {
            // workaround to enable further creation of relations in PDFAnno
            // if existing annotations are not rerendered after an attempt to create a relation.
            // it can happen that mouse will hang when leaving annotation knob while dragging
            renderPdfAnnoModel(aTarget, aPdfExtractFile.getPdftxt());
        }
    }

    private void selectRelationAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                          JCas aJCas)
        throws IOException
    {
        try {
            AnnotationFS originFs = selectByAddr(aJCas,
                aParams.getParameterValue("origin").toInt());
            AnnotationFS targetFs = selectByAddr(aJCas,
                aParams.getParameterValue("target").toInt());

            AnnotatorState state = getModelObject();
            Selection selection = state.getSelection();
            selection.selectArc(VID.parseOptional(aParams.getParameterValue("id").toString()),
                originFs, targetFs);

            if (selection.getAnnotation().isSet()) {
                getActionHandler().actionSelect(aTarget, aJCas);
            }
        }
        catch (AnnotationException e)
        {
            handleError("Unable to select relation annotation", e, aTarget);
        }
    }

    private void deleteRecommendation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                      JCas aJCas, PdfExtractFile aPdfExtractFile)
    {
        try {
            VID paramId = VID.parseOptional(aParams.getParameterValue("id").toString());
            if (paramId.isSynthetic()) {
                DocumentModel documentModel = new DocumentModel(aJCas.getDocumentText());
                Offset offset = new Offset(aParams);
                Offset docOffset =
                    PdfAnnoRenderer.convertToDocumentOffset(offset, documentModel, aPdfExtractFile);
                if (docOffset.getBegin() > -1 && docOffset.getEnd() > -1) {
                    extensionRegistry.fireAction(getActionHandler(), getModelObject(), aTarget,
                        aJCas, paramId, "doAction", docOffset.getBegin(), docOffset.getEnd());
                } else {
                    handleError("Unable to delete recommendation: No match was found", aTarget);
                }
            }
        }
        catch (AnnotationException | IOException e)
        {
            handleError("Unable to delete recommendation", e, aTarget);
        }
    }

    public void handleAPIRequest(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                 String aPdftxt)
    {
        try
        {
            JCas jCas = getJCasProvider().get();
            PdfExtractFile pdfExtractFile = new PdfExtractFile(aPdftxt);
            String action = aParams.getParameterValue("action").toString();

            switch (action)
            {
            case "createSpan": createSpanAnnotation(aTarget, aParams, jCas, pdfExtractFile);
                break;
            case "selectSpan": selectSpanAnnotation(aTarget, aParams, jCas, pdfExtractFile);
                break;
            case "createRelation": createRelationAnnotation(aTarget, aParams, jCas, pdfExtractFile);
                break;
            case "selectRelation": selectRelationAnnotation(aTarget, aParams, jCas);
                break;
            case "deleteRecommendation":
                deleteRecommendation(aTarget, aParams, jCas, pdfExtractFile);
                break;
            default: handleError("Unkown action: " + action, aTarget);
            }
        }
        catch (IOException e)
        {
            handleError("Unable to load data", e, aTarget);
        }
    }

    /**
     * Returns JavaScript code that imports annotation data in PDFAnno
     */
    private String getAnnotationsJS(PdfAnnoModel aPdfAnnoModel)
    {
        return String.join("",
            "var annoFile = `\n",
            aPdfAnnoModel.getAnnoFileContent(),
            "`;",
            "pdfanno.contentWindow.annoPage.importAnnotation({",
            "'primary': true,",
            "'colorMap': {},",
            "'annotations':[annoFile]}, true);"
        );
    }
}
