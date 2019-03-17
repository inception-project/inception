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
import de.tudarmstadt.ukp.inception.pdfeditor.config.PdfEditorProperties;

public class PdfAnnoPanel
    extends Panel
{
    private static final long serialVersionUID = 4202869513273132875L;

    private @SpringBean DocumentService documentService;

    private @SpringBean PdfEditorProperties pdfEditorProperties;

    private AbstractAjaxBehavior pdfProvider;

    private AbstractAjaxBehavior pdftxtProvider;

    private AbstractAjaxBehavior annoProvider;

    private AbstractAjaxBehavior apiProvider;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public PdfAnnoPanel(String aId, IModel<AnnotatorState> aModel,
                        PdfAnnotationEditor aPdfAnnotationEditor)
    {
        super(aId, aModel);

        add(pdfProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 7715393703216199195L;

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

        add(pdftxtProvider = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = -8676150164372852265L;

            @Override
            public void respond(AjaxRequestTarget aTarget)
            {
                aPdfAnnotationEditor.initialize(aTarget);
                String pdftext = aPdfAnnotationEditor.getPdfExtractFile().getPdftxt();
                getRequestCycle().scheduleRequestHandlerAfterCurrent(
                    new ResourceStreamRequestHandler(
                        new StringResourceStream(pdftext))
                );
            }
        });

        add(annoProvider = new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 8501859992311111560L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                aPdfAnnotationEditor.renderPdfAnnoModel(aTarget);
            }
        });

        add(apiProvider = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 3816087744638629290L;

            @Override
            protected void respond(AjaxRequestTarget aTarget) {
                aPdfAnnotationEditor.handleAPIRequest(aTarget, getRequest().getPostParameters());
            }
        });

        add(new WebMarkupContainer("frame")
        {
            private static final long serialVersionUID = 1421253898149294234L;

            @Override
            protected final void onComponentTag(final ComponentTag aTag)
            {
                checkComponentTag(aTag, "iframe");

                String indexFile = pdfEditorProperties.isDebug() ? "index-debug.html" : "index.html";

                String viewerUrl = RequestCycle.get().getUrlRenderer()
                        .renderFullUrl(Url.parse("resources/pdfanno/" + indexFile));

                String pdfUrl = getPage().getRequestCycle().getUrlRenderer()
                        .renderFullUrl(Url.parse(pdfProvider.getCallbackUrl()));

                String pdftxtUrl = getPage().getRequestCycle().getUrlRenderer()
                    .renderFullUrl(Url.parse(pdftxtProvider.getCallbackUrl()));

                String annoUrl = getPage().getRequestCycle().getUrlRenderer()
                    .renderFullUrl(Url.parse(annoProvider.getCallbackUrl()));

                String apiUrl = getPage().getRequestCycle().getUrlRenderer()
                    .renderFullUrl(Url.parse(apiProvider.getCallbackUrl()));

                viewerUrl += "?pdf=" + pdfUrl + "&pdftxt=" + pdftxtUrl + "&anno=" + annoUrl
                            + "&api=" + apiUrl;

                aTag.put("src", viewerUrl);

                super.onComponentTag(aTag);
            }
        });
    }
}
