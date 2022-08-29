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
package de.tudarmstadt.ukp.inception.pdfeditor;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.ServletContextUtils.referenceToUrl;
import static de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoSerializer.convertToDocumentOffset;
import static de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoSerializer.convertToDocumentOffsets;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VID.NONE_ID;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
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
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfDocumentIFrameView;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.DocumentModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfAnnoModel;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render.PdfAnnoSerializer;
import de.tudarmstadt.ukp.inception.pdfeditor.resources.PdfAnnotationEditorCssResourceReference;
import de.tudarmstadt.ukp.inception.pdfeditor.resources.PdfAnnotationEditorJavascriptResourceReference;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

/**
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
public class PdfAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private static final String CREATE_SPAN = "createSpan";
    private static final String GET_ANNOTATIONS = "getAnnotations";

    private static final String VIS = "vis";

    private DocumentModel documentModel;
    private Map<Integer, Offset> pageOffsetCache;
    private int page;
    private PdfDocumentIFrameView view;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ColoringService coloringService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean(name = "pdfDocumentIFrameViewFactory") DocumentViewFactory viewFactory;
    private @SpringBean ServletContext servletContext;

    public PdfAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        try {
            documentModel = new DocumentModel(getCasProvider().get().getDocumentText());
        }
        catch (IOException e) {
            handleError("Unable to load data", e);
        }

        pageOffsetCache = new HashMap<>();
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

    private void handleError(String aMessage, Throwable aCause, AjaxRequestTarget aTarget)
    {
        if (aCause instanceof AnnotationException) {
            LOG.debug(aMessage, aCause);
            handleError(aCause.getMessage(), aTarget);
        }
        else {
            LOG.error(aMessage, aCause);
            handleError(aMessage + ": " + ExceptionUtils.getRootCauseMessage(aCause), aTarget);
        }
    }

    private void handleError(String aMessage, AjaxRequestTarget aTarget)
    {
        error(aMessage);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    /**
     * Renders the PdfAnnoModel. This includes the anno file and the color map.
     */
    @SuppressWarnings("javadoc")
    public void renderPdfAnnoModel(AjaxRequestTarget aTarget)
    {
        if (getModelObject().getProject() == null) {
            return;
        }

        Offset pageOffset = pageOffsetCache.computeIfAbsent(page, this::calculatePageOffsets);

        if (pageOffset == null) {
            return;
        }

        try {
            CAS cas = getCasProvider().get();

            // get sentences in which page begin and end offsets are and use those to compute
            // the new page begin and end offsets. required because annotation rendering will
            // sometimes fail if offset in middle of a sentence
            AnnotationFS beginSent = selectSentenceAt(cas, pageOffset.getBegin());
            int begin = (beginSent != null) ? beginSent.getBegin() : pageOffset.getBegin();
            AnnotationFS endSent = selectSentenceAt(cas, pageOffset.getEnd());
            int end = (endSent != null) ? endSent.getEnd() : pageOffset.getEnd();

            PdfAnnoModel pdfAnnoModel = render(cas, begin, end,
                    new PdfAnnoSerializer(view.getPdfExtractFile(), begin));

            // show unmatched spans to user
            if (pdfAnnoModel.getUnmatchedSpans().size() > 0) {
                String annotations = pdfAnnoModel.getUnmatchedSpans().stream()
                        .map(span -> "(id: " + span.getId() + ", text: \"" + span.getText() + "\")")
                        .collect(Collectors.joining(", "));
                handleError("Could not find a match for the following annotations: " + annotations,
                        aTarget);
            }

            aTarget.appendJavaScript(getAnnotationsJS(pdfAnnoModel));
        }
        catch (IOException e) {
            handleError("Unable to load data", e, aTarget);
        }
    }

    public void createSpanAnnotation(AjaxRequestTarget aTarget, IRequestParameters aParams,
            CAS aCas)
    {
        if (view.getPdfExtractFile() == null && documentModel == null) {
            // in this case the user probably changed the document and accidentally
            // marked text in the old document. so do not create any annotation here.
            handleError(
                    "Unable to create span annotation: "
                            + "Did you accidentally marked text when switching documents?",
                    aTarget);
            return;
        }

        try {
            Offset offset = new Offset(aParams);
            Offset docOffset = convertToDocumentOffset(offset, documentModel,
                    view.getPdfExtractFile());
            AnnotatorState state = getModelObject();
            if (docOffset.getBegin() > -1 && docOffset.getEnd() > -1) {
                if (state.isSlotArmed()) {
                    // When filling a slot, the current selection is *NOT* changed. The
                    // Span annotation which owns the slot that is being filled remains
                    // selected!
                    getActionHandler().actionFillSlot(aTarget, aCas, docOffset.getBegin(),
                            docOffset.getEnd(), NONE_ID);
                }
                else {
                    state.getSelection().selectSpan(aCas, docOffset.getBegin(), docOffset.getEnd());
                    getActionHandler().actionCreateOrUpdate(aTarget, aCas);
                }
            }
            else {
                handleError("Unable to create span annotation: No match was found", aTarget);
            }
        }
        catch (IOException | AnnotationException e) {
            handleError("Unable to create span annotation", e, aTarget);
        }
    }

    private void getAnnotations(AjaxRequestTarget aTarget, IRequestParameters aParams)
    {
        page = aParams.getParameterValue("page").toInt(1);
        renderPdfAnnoModel(aTarget);
    }

    private Offset calculatePageOffsets(int aPage)
    {
        // get page offsets, if possible for the from previous to next page
        int maxPageNumber = view.getPdfExtractFile().getMaxPageNumber();

        int beginPage = Math.min(Math.max(1, aPage - 1), maxPageNumber);
        int endPage = Math.min(Math.max(1, aPage + 1), maxPageNumber);

        int begin = view.getPdfExtractFile().getBeginPageOffset(beginPage);
        int end = view.getPdfExtractFile().getEndPageOffset(endPage);

        List<Offset> offsets = new ArrayList<>();
        offsets.add(new Offset(begin, begin));
        offsets.add(new Offset(end, end));
        offsets = convertToDocumentOffsets(offsets, documentModel, view.getPdfExtractFile());

        int newBegin = offsets.stream().mapToInt(Offset::getBegin).min().getAsInt();
        int newEnd = offsets.stream().mapToInt(Offset::getEnd).max().getAsInt();

        return new Offset(newBegin, newEnd);
    }

    public void handleAPIRequest(AjaxRequestTarget aTarget, IRequestParameters aParams)
    {
        try {
            CAS cas = getCasProvider().get();
            String action = aParams.getParameterValue("action").toString();

            // // Doing anything but selecting or creating a span annotation when a
            // // slot is armed will unarm it
            // if (getModelObject().isSlotArmed() && !(action.equals(CREATE_SPAN))) {
            // getModelObject().clearArmedSlot();
            // }

            switch (action) {
            case CREATE_SPAN:
                createSpanAnnotation(aTarget, aParams, cas);
                break;
            case GET_ANNOTATIONS:
                getAnnotations(aTarget, aParams);
                break;
            default:
                handleError("Unknown action: " + action, aTarget);
            }
        }
        catch (IOException e) {
            handleError("Unable to load data", e, aTarget);
        }
    }

    /**
     * Returns JavaScript code that imports annotation data in PDFAnno
     */
    private String getAnnotationsJS(PdfAnnoModel aPdfAnnoModel)
    {
        return join("", //
                "var annoFile = `\n", aPdfAnnoModel.getAnnoFileContent(), "`;",
                // FIXME: "pdfanno" below is a hard-coded HTML element ID from PdfAnnoPanel.html!
                // This should be replaced by a HTML element ID generated by Wicket for the
                // PdfAnnoPanel instance used by this editor.
                "pdfanno.contentWindow.annoPage.importAnnotation({", //
                "'primary': true,", //
                "'colorMap': {},", //
                "'annotations':[annoFile]}, true);");
    }
}
