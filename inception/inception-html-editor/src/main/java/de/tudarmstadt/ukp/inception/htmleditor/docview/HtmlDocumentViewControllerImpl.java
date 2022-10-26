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
package de.tudarmstadt.ukp.inception.htmleditor.docview;

import java.security.Principal;

import javax.servlet.ServletContext;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlXmlDocumentIFrameViewFactory;

/**
 * @deprecated Use {@link XHtmlXmlDocumentIFrameViewFactory} instead
 */
@Deprecated
@ConditionalOnWebApplication
@RestController
@RequestMapping(HtmlDocumentViewController.BASE_URL)
public class HtmlDocumentViewControllerImpl
    implements HtmlDocumentViewController
{
    private static final String GET_DOCUMENT_PATH = "/p/{projectId}/raw/{documentId}";

    private final DocumentService documentService;
    private final ServletContext servletContext;

    @Autowired
    public HtmlDocumentViewControllerImpl(DocumentService aDocumentService,
            ServletContext aServletContext)
    {
        documentService = aDocumentService;
        servletContext = aServletContext;
    }

    @Override
    public String getDocumentUrl(SourceDocument aDoc)
    {
        return servletContext.getContextPath() + BASE_URL
                + GET_DOCUMENT_PATH
                        .replace("{projectId}", String.valueOf(aDoc.getProject().getId()))
                        .replace("{documentId}", String.valueOf(aDoc.getId()));
    }

    @PreAuthorize("@documentAccess.canViewAnnotationDocument(#aProjectId, #aDocumentId, #principal.name)")
    @Override
    @GetMapping(path = GET_DOCUMENT_PATH)
    public ResponseEntity<String> getDocument(@PathVariable("projectId") long aProjectId,
            @PathVariable("documentId") long aDocumentId, Principal principal)
        throws Exception
    {
        SourceDocument doc = documentService.getSourceDocument(aProjectId, aDocumentId);

        CAS cas = documentService.createOrReadInitialCas(doc);

        if (cas.select(XmlDocument.class).isEmpty()) {
            return ResponseEntity.ok() //
                    .contentType(MediaType.TEXT_PLAIN) //
                    .body(cas.getDocumentText());
        }

        HtmlDocumentRenderer renderer = new HtmlDocumentRenderer();
        renderer.setRenderOnlyBody(false);
        String xml = renderer.render(cas);
        return ResponseEntity.ok() //
                .contentType(MediaType.TEXT_HTML) //
                .body(xml);
    }
}
