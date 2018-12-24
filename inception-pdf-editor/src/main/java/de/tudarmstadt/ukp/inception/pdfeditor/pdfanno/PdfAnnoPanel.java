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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno;

import java.io.File;
import java.io.IOException;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.pdfeditor.PdfAnnotationEditor;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.AnnoFile;
import paperai.pdfextract.PDFExtractor;

public class PdfAnnoPanel
    extends Panel
{
    private static final long serialVersionUID = 4202869513273132875L;

    private @SpringBean DocumentService documentService;

    private AbstractAjaxBehavior pdfProvider;

    private AbstractAjaxBehavior pdftxtProvider;

    private AbstractAjaxBehavior annoProvider;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public PdfAnnoPanel(String aId, IModel<AnnotatorState> aModel,
                        PdfAnnotationEditor pdfAnnotationEditor)
    {
        super(aId, aModel);

        add(pdfProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onRequest()
            {
                SourceDocument doc = aModel.getObject().getDocument();

                File pdfFile = documentService.getSourceDocumentFile(doc);

                getRequestCycle().scheduleRequestHandlerAfterCurrent(
                        new ResourceStreamRequestHandler(new FileResourceStream(pdfFile),
                                doc.getName()));
            }
        });

        add(pdftxtProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 2L;

            @Override
            public void onRequest()
            {
                SourceDocument doc = aModel.getObject().getDocument();

                File pdfFile = documentService.getSourceDocumentFile(doc);

                try
                {
                    String pdftext = PDFExtractor.processFileToString(pdfFile, false);
                    getRequestCycle().scheduleRequestHandlerAfterCurrent(
                            new ResourceStreamRequestHandler(
                                    new StringResourceStream(pdftext))
                    );
                }
                catch (IOException e)
                {
                    log.error("Unable to get PDF text for " + pdfFile.getName()
                        + "with PDFExtractor.", e);
                }
            }
        });

        add(annoProvider = new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 3L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                SourceDocument doc = aModel.getObject().getDocument();

                File pdfFile = documentService.getSourceDocumentFile(doc);

                try
                {
                    String pdftext = PDFExtractor.processFileToString(pdfFile, false);
                    AnnoFile annoFile = pdfAnnotationEditor.renderAnnoFile(pdftext);
                    String script = "setTimeout(function() { " +
                        "var annoFile = `\n" +
                        annoFile +
                        "`;\n" +
                        "pdfanno.contentWindow.annoPage.importAnnotation({" +
                        "'primary': true," +
                        "'colorMap': " + annoFile.getColorMap().toString() + "," +
                        "'annotations':[annoFile]}, true);" +
                        "}, 10);";

                    aTarget .appendJavaScript(script);
                }
                catch (IOException e)
                {
                    log.error("Unable to get PDF text for " + pdfFile.getName()
                        + "with PDFExtractor.", e);
                }
            }
        });

        add(new WebMarkupContainer("frame")
        {
            private static final long serialVersionUID = 1421253898149294234L;

            @Override
            protected final void onComponentTag(final ComponentTag tag)
            {
                checkComponentTag(tag, "iframe");

                String viewerUrl = RequestCycle.get().getUrlRenderer()
                        .renderFullUrl(Url.parse("resources/pdfanno/index.html"));

                String pdfUrl = getPage().getRequestCycle().getUrlRenderer()
                        .renderFullUrl(Url.parse(pdfProvider.getCallbackUrl()));

                String pdftxtUrl = getPage().getRequestCycle().getUrlRenderer()
                    .renderFullUrl(Url.parse(pdftxtProvider.getCallbackUrl()));

                String annoUrl = getPage().getRequestCycle().getUrlRenderer()
                    .renderFullUrl(Url.parse(annoProvider.getCallbackUrl()));

                viewerUrl += "?pdf=" + pdfUrl + "&pdftxt=" + pdftxtUrl + "&anno=" + annoUrl;

                tag.put("src", viewerUrl);

                super.onComponentTag(tag);
            }
        });
    }
}
