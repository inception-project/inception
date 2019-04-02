/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import java.io.IOException;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

@MountPath("/documentDetails.html")
public class DocumentDetailsPage
    extends ApplicationPageBase
{
    public static final String REPOSITORY_ID = "repo";
    public static final String COLLECTION_ID = "col";
    public static final String DOCUMENT_ID = "doc";

    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    public DocumentDetailsPage(PageParameters aParameters)
    {
        StringValue repositoryIdStringValue = aParameters.get(REPOSITORY_ID);
        StringValue collectionIdStringValue = aParameters.get(COLLECTION_ID);
        StringValue documentIdStringValue = aParameters.get(DOCUMENT_ID);

        if (
                repositoryIdStringValue == null || 
                documentIdStringValue == null || 
                collectionIdStringValue == null
        ) {
            abort();
        }
        else {
            DocumentRepository repo = externalSearchService
                    .getRepository(repositoryIdStringValue.toLong());
            
            // Check access to project
            User currentUser = userRepository.getCurrentUser();
            if (!projectService.isAnnotator(repo.getProject(), currentUser)) {
                error("You have no permission to access project [" + repo.getProject().getId()
                        + "]");
                return;
            }
            
            String documentText;
            try {
                documentText = externalSearchService.getDocumentText(repo,
                        collectionIdStringValue.toString(), documentIdStringValue.toString());
            }
            catch (IOException e) {
                documentText = e.getMessage();
            }

            // FIXME: Instead of showing the document ID, we should fetch the document metadata
            // and show the title.
            add(new Label("title", documentIdStringValue.toString()));
            add(new Label("text", documentText));
        }
    }

    private void abort()
    {
        throw new RestartResponseException(getApplication().getHomePage());
    }
}
