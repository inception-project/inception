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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_COLLECTION_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_REPOSITORY_ID;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/search/${" + PAGE_PARAM_REPOSITORY_ID
        + "}/${" + PAGE_PARAM_COLLECTION_ID + "}/${" + PAGE_PARAM_DOCUMENT_ID + "}")
public class DocumentDetailsPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -645134257384090420L;

    public static final String PAGE_PARAM_REPOSITORY_ID = "repo";
    public static final String PAGE_PARAM_COLLECTION_ID = "col";
    public static final String PAGE_PARAM_DOCUMENT_ID = "doc";

    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private DocumentRepository repo;
    private String collectionId;
    private String documentId;

    public DocumentDetailsPage(PageParameters aParameters)
    {
        super(aParameters);

        User user = userRepository.getCurrentUser();
        requireAnyProjectRole(user);

        StringValue repositoryIdStringValue = aParameters.get(PAGE_PARAM_REPOSITORY_ID);
        StringValue collectionIdStringValue = aParameters.get(PAGE_PARAM_COLLECTION_ID);
        StringValue documentIdStringValue = aParameters.get(PAGE_PARAM_DOCUMENT_ID);

        if (repositoryIdStringValue.isEmpty() || documentIdStringValue.isEmpty()
                || collectionIdStringValue.isEmpty()) {
            backToProjectPage();
        }

        repo = externalSearchService.getRepository(repositoryIdStringValue.toLong());
        collectionId = collectionIdStringValue.toString();
        documentId = documentIdStringValue.toString();
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add(new Label("title", LoadableDetachableModel.of(this::getDocumentResult).map(
                r -> r.getDocumentTitle() != null ? r.getDocumentTitle() : r.getDocumentId())));
        add(new Label("text", LoadableDetachableModel.of(this::getDocumentText)));
    }

    private String getDocumentText()
    {
        try {
            return externalSearchService.getDocumentText(repo, collectionId, documentId);
        }
        catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private ExternalSearchResult getDocumentResult()
    {
        try {
            return externalSearchService.getDocumentResult(repo, collectionId, documentId);
        }
        catch (Exception e) {
            return new ExternalSearchResult(repo, collectionId, documentId);
        }
    }
}
