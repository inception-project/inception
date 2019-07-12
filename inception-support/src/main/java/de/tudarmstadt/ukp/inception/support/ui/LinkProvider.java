/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.support.ui;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class LinkProvider
{

    /**
     * Create an external link to a page which opens a document, codes the url as
     * {@code aPageClass?params#!p=projectId&d=docId}.
     */
    public static ExternalLink createDocumentPageLink(DocumentService aDocService, Project aProject,
            String aDocId, String aId, String aLinkLabel, Class<? extends WebPage> aPageClass)
    {
        long docId = -1;
        if (aDocService.existsSourceDocument(aProject, aDocId)) {
            docId = aDocService.getSourceDocument(aProject, aDocId).getId();
        }
        
        return createDocumentPageLink(aProject, docId, aId, aLinkLabel, aPageClass);
    }
    
    public static ExternalLink createDocumentPageLink(Project aProject, long aDocId, String aId,
            String aLinkLabel, Class<? extends WebPage> aPageClass)
    {
        String url = "";
        if (aDocId >= 0) {
            url = String.format("%s#!p=%d&d=%d",
                    RequestCycle.get().urlFor(aPageClass, new PageParameters()), aProject.getId(),
                    aDocId);
        }
        if (aLinkLabel == null) {
            new ExternalLink(aId, url);
        }
        
        return new ExternalLink(aId, url, aLinkLabel);
        
    }

    public static ExternalLink createDocumentPageLink(DocumentService aDocService, Project aProject,
            String aDocumentId, String aId, Class<? extends WebPage> aClass)
    {
        return createDocumentPageLink(aDocService, aProject, aDocumentId, aId, null, aClass);
    }
    
    public static ExternalLink createDocumentPageLink(Project aProject,
            long aDocumentId, String aId, Class<? extends WebPage> aClass)
    {
        return createDocumentPageLink(aProject, aDocumentId, aId, null, aClass);
    }

}
