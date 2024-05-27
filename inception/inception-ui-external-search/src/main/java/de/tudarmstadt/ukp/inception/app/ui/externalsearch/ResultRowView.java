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

import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_COLLECTION_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_REPOSITORY_ID;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporter;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.HighlightLabel;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class ResultRowView
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(SearchPage.class);

    private static final long serialVersionUID = -6708211343231617251L;

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentImporter documentImporter;
    private @SpringBean UserDao userRepository;

    private final IModel<Project> project;

    public ResultRowView(String aId, long aRowNumber, IModel<Project> aProject,
            IModel<ExternalSearchResult> aModel)
    {
        super(aId, aModel);

        project = aProject;

        ExternalSearchResult result = aModel.getObject();

        boolean existsSourceDocument = documentService.existsSourceDocument(project.getObject(),
                result.getDocumentId());
        queue(new Icon("icon",
                LoadableDetachableModel.of(() -> existsSourceDocument ? FontAwesome5IconType.file_r
                        : FontAwesome5IconType.file_download_s)));

        queue(new ListView<ExternalSearchHighlight>("highlights", result.getHighlights())
        {
            private static final long serialVersionUID = 7281297180627354855L;

            @Override
            protected void populateItem(ListItem<ExternalSearchHighlight> aItem)
            {
                aItem.add(new HighlightLabel("highlight", aItem.getModelObject().getHighlight()));
            }
        });

        PageParameters titleLinkPageParameters = new PageParameters();
        ProjectPageBase.setProjectPageParameter(titleLinkPageParameters, project.getObject());
        titleLinkPageParameters.set(PAGE_PARAM_REPOSITORY_ID, result.getRepository().getId());
        titleLinkPageParameters.set(PAGE_PARAM_COLLECTION_ID, result.getCollectionId());
        titleLinkPageParameters.set(PAGE_PARAM_DOCUMENT_ID, result.getDocumentId());
        Link<Void> link = new BookmarkablePageLink<>("titleLink", DocumentDetailsPage.class,
                titleLinkPageParameters);
        queue(new Label("title",
                defaultIfBlank(result.getDocumentTitle(), defaultIfBlank(result.getDocumentId(),
                        defaultIfBlank(result.getOriginalUri(), "<no title>")))));
        queue(link);

        queue(new Label("score", result.getScore()));
        queue(new LambdaAjaxLink("importLink", _target -> actionImportDocument(_target, result))
                .add(visibleWhen(() -> !existsSourceDocument)));

        PageParameters openLinkPageParameters = new PageParameters();
        ProjectPageBase.setProjectPageParameter(openLinkPageParameters, project.getObject());
        openLinkPageParameters.set(AnnotationPage.PAGE_PARAM_DOCUMENT, result.getDocumentId());
        Link<Void> openLink = new BookmarkablePageLink<>("openLink", AnnotationPage.class,
                openLinkPageParameters);
        openLink.add(visibleWhen(() -> existsSourceDocument));
        queue(openLink);

        // add(LinkProvider
        // .createDocumentPageLink(documentService, getProject(), result.getDocumentId(),
        // "openLink", AnnotationPage.class)
        // .add(visibleWhen(() -> existsSourceDocument)));
    }

    void actionImportDocument(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        try {
            documentImporter.importDocumentFromDocumentRepository(userRepository.getCurrentUser(),
                    project.getObject(), aResult.getCollectionId(), aResult.getDocumentId(),
                    aResult.getRepository());

            send(this, BUBBLE, new ExternalDocumentImportedEvent(aTarget, aResult));
        }
        catch (IOException e) {
            LOG.error(e.getMessage(), e);
            error(e.getMessage() + " - " + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }
}
