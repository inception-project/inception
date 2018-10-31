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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class PdfAnnoPanel
    extends Panel
{
    private static final long serialVersionUID = 4202869513273132875L;

    private @SpringBean DocumentService documentService;

    private AbstractAjaxBehavior pdfProvider;
    
    public PdfAnnoPanel(String aId, IModel<AnnotatorState> aModel)
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

                viewerUrl += "?pdf=" + pdfUrl;

                tag.put("src", viewerUrl);

                super.onComponentTag(tag);
            }
        });
    }
}
