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
package de.tudarmstadt.ukp.inception.pdfeditor2.view;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.UrlRenderer;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.dkpro.core.api.pdf.type.PdfPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.pdfeditor2.PdfAnnotationEditor;
import de.tudarmstadt.ukp.inception.pdfeditor2.format.VisualPdfReader;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.VisualPDFTextStripper;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class PdfDocumentIFrameView
    extends WebMarkupContainer
{
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private static final long serialVersionUID = 4202869513273132875L;

    private @SpringBean DocumentService documentService;

    private AbstractAjaxBehavior pdfProvider;
    private AbstractAjaxBehavior vModelProvider;

    public PdfDocumentIFrameView(String aId, IModel<AnnotationDocument> aDoc,
            String aEditorFactoryId)
    {
        super(aId, aDoc);

        add(pdfProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 7715393703216199195L;

            @Override
            public void onRequest()
            {
                SourceDocument doc = aDoc.getObject().getDocument();

                File pdfFile = documentService.getSourceDocumentFile(doc);

                FileResourceStream resource = new FileResourceStream(pdfFile)
                {
                    private static final long serialVersionUID = 5985138568430773008L;

                    @Override
                    public String getContentType()
                    {
                        return "application/pdf";
                    }
                };

                ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
                handler.setFileName(doc.getName());
                handler.setCacheDuration(Duration.ZERO);
                handler.setContentDisposition(ContentDisposition.INLINE);

                getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
            }
        });

        add(vModelProvider = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = -8676150164372852265L;

            @Override
            public void respond(AjaxRequestTarget aTarget)
            {
                sendVModel(aDoc, aTarget);
            }
        });
    }

    private void sendVModel(IModel<AnnotationDocument> aDoc, AjaxRequestTarget aTarget)
    {
        try {
            CAS cas = documentService.readAnnotationCas(aDoc.getObject());

            VModel vModel;
            var pdfPages = cas.select(PdfPage.class).asList();
            if (!pdfPages.isEmpty()) {
                vModel = VisualPdfReader.visualModelFromCas(cas, pdfPages);
            }
            else {
                vModel = visualModelFromPdfSource();
            }

            String json = JSONUtil.toJsonString(vModel);

            StringResourceStream resource = new StringResourceStream(json, "application/json");

            ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
            handler.setFileName("vmodel.json");
            handler.setCacheDuration(Duration.ofSeconds(1));
            handler.setContentDisposition(ContentDisposition.INLINE);

            getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
        }
        catch (IOException e) {
            handleError("Unable to create visual model for ["
                    + aDoc.getObject().getDocument().getName() + "]", e, aTarget);
        }
    }

    private VModel visualModelFromPdfSource() throws IOException
    {
        LOG.info("Loading visual model from source");
        VModel vModel;
        File file = documentService.getSourceDocumentFile(getModel().getObject().getDocument());
        try (PDDocument doc = PDDocument.load(file)) {
            var extractor = new VisualPDFTextStripper();
            extractor.writeText(doc, new StringWriter());
            vModel = extractor.getVisualModel();
        }
        return vModel;
    }

    @Override
    protected void onComponentTag(ComponentTag aTag)
    {
        UrlRenderer urlRenderer = RequestCycle.get().getUrlRenderer();

        String viewerUrl = urlRenderer.renderContextRelativeUrl("resources/pdfanno2/viewer.html");
        String pdfUrl = urlRenderer.renderFullUrl(Url.parse(pdfProvider.getCallbackUrl()));
        String vModelUrl = urlRenderer.renderFullUrl(Url.parse(vModelProvider.getCallbackUrl()));

        aTag.setName("iframe");
        aTag.put("src", viewerUrl + "?pdf=" + pdfUrl + "&vmodel=" + vModelUrl);
        aTag.put("class", "flex-content");
        aTag.put("frameborder", "0");
        aTag.put("scrolling", "no");

        super.onComponentTag(aTag);
    }

    public IModel<AnnotationDocument> getModel()
    {
        return (IModel<AnnotationDocument>) getDefaultModel();
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
}
