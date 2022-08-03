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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno;

import static de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile.getSubstitutionTable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.pdfeditor.PdfAnnotationEditor;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfextract.PDFExtractor;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

/**
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
public class PdfDocumentIFrameView
    extends WebMarkupContainer
{
    private static final Logger LOG = LoggerFactory.getLogger(PdfAnnotationEditor.class);

    private static final long serialVersionUID = 4202869513273132875L;

    private @SpringBean DocumentService documentService;

    private AbstractAjaxBehavior pdfProvider;
    private AbstractAjaxBehavior pdftxtProvider;
    private AbstractAjaxBehavior apiProvider;

    private PdfExtractFile pdfExtractFile;

    public PdfDocumentIFrameView(String aId, IModel<AnnotationDocument> aDoc,
            String aEditorFactoryId)
    {
        super(aId, aDoc);

        // FIXME: "pdfanno" below is a hard-coded probably somewhere in the JS code
        setMarkupId("pdfanno");

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

        add(pdftxtProvider = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = -8676150164372852265L;

            @Override
            public void respond(AjaxRequestTarget aTarget)
            {
                initialize(aTarget);
                String pdftext = pdfExtractFile.getPdftxt();
                SourceDocument doc = aDoc.getObject().getDocument();

                StringResourceStream resource = new StringResourceStream(pdftext, "text/plain");

                ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
                handler.setFileName(doc.getName() + ".txt");
                handler.setCacheDuration(Duration.ofSeconds(1));
                handler.setContentDisposition(ContentDisposition.INLINE);

                getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
            }
        });

        add(apiProvider = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 3816087744638629290L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                PdfAnnotationEditor editor = PdfDocumentIFrameView.this
                        .findParent(PdfAnnotationEditor.class);
                editor.handleAPIRequest(aTarget, getRequest().getPostParameters());
            }
        });
    }

    @Override
    protected void onComponentTag(ComponentTag aTag)
    {
        String indexFile = "viewer.html";

        UrlRenderer urlRenderer = RequestCycle.get().getUrlRenderer();

        String viewerUrl = urlRenderer.renderContextRelativeUrl("resources/pdfanno/" + indexFile);

        String pdfUrl = urlRenderer.renderFullUrl(Url.parse(pdfProvider.getCallbackUrl()));
        String pdftxtUrl = urlRenderer.renderFullUrl(Url.parse(pdftxtProvider.getCallbackUrl()));
        String apiUrl = urlRenderer.renderFullUrl(Url.parse(apiProvider.getCallbackUrl()));

        viewerUrl += "?pdf=" + pdfUrl + "&pdftxt=" + pdftxtUrl + "&api=" + apiUrl;

        aTag.setName("iframe");
        aTag.put("src", viewerUrl);
        aTag.put("class", "flex-content");
        aTag.put("frameborder", "0");
        aTag.put("scrolling", "no");

        super.onComponentTag(aTag);
    }

    public PdfExtractFile getPdfExtractFile()
    {
        return pdfExtractFile;
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotationDocument> getModel()
    {
        return (IModel<AnnotationDocument>) getDefaultModel();
    }

    private void initialize(AjaxRequestTarget aTarget)
    {
        File pdfFile = documentService.getSourceDocumentFile(getModel().getObject().getDocument());

        try {
            String pdfText = PDFExtractor.processFileToString(pdfFile);
            pdfExtractFile = new PdfExtractFile(pdfText, getSubstitutionTable());
        }
        catch (IOException | SAXException | ParserConfigurationException e) {
            handleError("Unable to create PdfExtractFile for [" + pdfFile.getName() + "]"
                    + "with PDFExtractor.", e, aTarget);
        }
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
