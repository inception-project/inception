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

import java.util.ArrayList;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@MountPath("/documentDetails.html")
public class DocumentDetailsPage extends ApplicationPageBase
{
    public static final String DOCUMENT_TITLE = "title";

    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;

    private final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");

    private DocumentRepository currentRepository;
    private User currentUser;
    private Project project;

    public DocumentDetailsPage(PageParameters aParameters)
    {
        project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project == null) {
            abort();
        }

        currentUser = userRepository.getCurrentUser();

        ArrayList<DocumentRepository> repositories;

        repositories = (ArrayList<DocumentRepository>) externalSearchService
            .listDocumentRepositories(project);

        if (repositories.size() > 0) {
            currentRepository = repositories.get(0);
        }
        else {
            currentRepository = null;
        }

        renderDocument(aParameters);

        add(mainContainer);
    }

    protected void renderDocument(PageParameters aParameters)
    {
        StringValue documentTitleStringValue = aParameters.get(DOCUMENT_TITLE);

        if (documentTitleStringValue == null) {
            abort();
        }
        else {
            String documentTitle = documentTitleStringValue.toString();
            mainContainer.add(new Label("title", documentTitle));

            String documentText = externalSearchService.getDocumentById(currentUser,
                currentRepository, documentTitle).getText();
            Label textElement = new Label("text", documentText);
            textElement.setOutputMarkupId(true);
            textElement.setEscapeModelStrings(false);
            mainContainer.add(textElement);
        }
    }

    private void abort() {
        throw new RestartResponseException(getApplication().getHomePage());
    }
}
