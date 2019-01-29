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

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
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
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfAnnoPanel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfAnnoRenderer;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
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
            // if something is selected care for existing selections, else throw them away
            if (selection.getBegin() != -1 && selection.getEnd() != -1) {
                // save existing selections before destroying them for rerendering
                String saveSelections = String.join("",
                    "var selectedAnnotations = [];",
                    "pdfanno.contentWindow.annoPage.getAllAnnotations().forEach(function(a) {",
                    "  if(a.selected) {",
                    "    selectedAnnotations.push(a.uuid);",
                    "  }",
                    "});",
                    "pdfanno.contentWindow.selectedAnnotations = selectedAnnotations;"
                );
                aTarget.appendJavaScript(saveSelections);
                // rerender existing annotations
                renderPdfAnnoModel(aTarget, pdftext);
                // restore selections
                String restoreSelections = String.join("",
                    "pdfanno.contentWindow.selectedAnnotations.forEach(function(uuid) {",
                    "  var annotation = pdfanno.contentWindow.annoPage.findAnnotationById(uuid);",
                    "  if (annotation !== null) {",
                    "    annotation.toggleSelect();",
                    "  }",
                    "});"
                );
                aTarget.appendJavaScript(restoreSelections);
            } else {
                renderPdfAnnoModel(aTarget, pdftext);
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
                String script = getAnnotationsJS(pdfAnnoModel, aTarget);
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

    public void createSpanAnnotation(AjaxRequestTarget aTarget, JCas aJCas,
                                        PdfExtractFile aPdfExtractFile, Offset aOffset)
    {
        try
        {
            Offset docOffset = PdfAnnoRenderer
                .convertToDocumentOffset(aJCas.getDocumentText(), aPdfExtractFile, aOffset);
            if (docOffset != null) {
                getModelObject().getSelection()
                    .selectSpan(aJCas, docOffset.getBegin(), docOffset.getEnd());
                getActionHandler().actionCreateOrUpdate(aTarget, aJCas);
                renderPdfAnnoModel(aTarget, aPdfExtractFile.getPdftxt());
                // select the annotation where annotation offset overlaps with selection offset
                String selectAnnotation = String.join("",
                    "pdfanno.contentWindow.annoPage.getAllAnnotations().forEach(function(a) {",
                    "  if (a.textRange && a.textRange[0] <= " + aOffset.getEnd(),
                    " && " + aOffset.getBegin() + " <= a.textRange[1]) {",
                    "    a.toggleSelect();",
                    "  }",
                    "});"
                );
                aTarget.appendJavaScript(selectAnnotation);
            } else {
                handleError("Unable to create annotation: No match was found", aTarget);
            }
        }
        catch (IOException | AnnotationException e)
        {
            handleError("Unable to create annotation", e, aTarget);
        }
    }

    private void selectSpanAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                      JCas aJCas, PdfExtractFile aPdfExtractFile, Offset aOffset)
    {
        try
        {
            VID paramId = VID.parseOptional(aParams.getParameterValue("id").toString());
            Offset docOffset = PdfAnnoRenderer
                .convertToDocumentOffset(aJCas.getDocumentText(), aPdfExtractFile, aOffset);
            if (docOffset != null) {
                if (paramId.isSynthetic()) {
                    extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                        aTarget, aJCas, paramId, "spanOpenDialog", docOffset.getBegin(),
                        docOffset.getEnd());
                } else {
                    getModelObject().getSelection()
                        .selectSpan(paramId, aJCas, docOffset.getBegin(), docOffset.getEnd());
                    getActionHandler().actionSelect(aTarget, aJCas);
                }
            } else {
                handleError("Unable to select annotation: No match was found", aTarget);
            }
        }
        catch (AnnotationException | IOException e)
        {
            handleError("Unable to select annotation", e, aTarget);
        }
    }

    public void handleAPIRequest(AjaxRequestTarget aTarget, IRequestParameters aParams,
                                 String aPdftxt)
    {
        try
        {
            JCas jCas = getJCasProvider().get();
            final Offset offset = new Offset(aParams);
            PdfExtractFile pdfExtractFile = new PdfExtractFile(aPdftxt);
            String action = aParams.getParameterValue("action").toString();

            switch (action)
            {
            case "createSpan": createSpanAnnotation(aTarget, jCas, pdfExtractFile, offset);
                               break;
            case "selectSpan": selectSpanAnnotation(aTarget, aParams, jCas, pdfExtractFile, offset);
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
    public String getAnnotationsJS(PdfAnnoModel aPdfAnnoModel, AjaxRequestTarget aTarget)
    {
        try {
            return String.join("",
                "var annoFile = `\n",
                aPdfAnnoModel.getAnnoFileContent(),
                "`;",
                "pdfanno.contentWindow.annoPage.importAnnotation({",
                "'primary': true,",
                "'colorMap': ", JSONUtil.toJsonString(aPdfAnnoModel.getColorMap()), ",",
                "'annotations':[annoFile]}, true);"
            );
        } catch (IOException e) {
            handleError("Could not map PDFAnno ColorMap to JSON String", e, aTarget);
        }
        return "";
    }
}
