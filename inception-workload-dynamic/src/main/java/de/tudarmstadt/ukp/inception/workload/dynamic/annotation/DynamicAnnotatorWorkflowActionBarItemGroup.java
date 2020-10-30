/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.workload.dynamic.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameModifier;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

public class DynamicAnnotatorWorkflowActionBarItemGroup
    extends Panel
{
    private static final long serialVersionUID = -292514874000914541L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;
    private @SpringBean EntityManager entityManager;

    private final AnnotationPageBase page;
    private final WorkloadManagementService workloadManagementService;

    protected final ConfirmationDialog finishDocumentDialog;

    private List<AnnotationDocument> annotationDocumentList;

    public DynamicAnnotatorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage,
            WorkloadManagementService aWorkloadManagementService)
    {
        super(aId);

        // Same as for the default
        page = aPage;
        workloadManagementService = aWorkloadManagementService;

        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));

        LambdaAjaxLink finishDocumentLink;
        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument));
        finishDocumentLink.setOutputMarkupId(true);
        finishDocumentLink.add(enabledWhen(page::isEditable));
        finishDocumentLink.add(new Label("state")
                .add(new CssClassNameModifier(LambdaModel.of(this::getStateClass))));
    }

    protected AnnotationPageBase getAnnotationPage()
    {
        return page;
    }

    public String getStateClass()
    {
        return FontAwesome5IconType.check_circle_r.cssClassName();
    }

    protected void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((_target) -> {
            page.actionValidateDocument(_target, page.getEditorCas());

            AnnotatorState state = page.getModelObject();
            User user = state.getUser();
            Project project = state.getProject();
            SourceDocument document = state.getDocument();

            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(document,
                    user);

            documentService.transitionAnnotationDocumentState(annotationDocument,
                    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            List<AnnotationDocument> inProgressDocuments = workloadManagementService
                    .getAnnotationDocumentsForSpecificState(IN_PROGRESS, project, user);
            if (inProgressDocuments.size() > 0) {
                getAnnotationPage().getModelObject().setDocument(
                        inProgressDocuments.get(0).getDocument(),
                        documentService.listSourceDocuments(project));
                getAnnotationPage().actionLoadDocument(_target);
                return;
            }

            WorkloadManager currentWorkload = workloadManagementService
                    .getOrCreateWorkloadManagerConfiguration(project);

            switch (dynamicWorkloadExtension.readTraits(currentWorkload).getType()) {
            case ("Randomized workflow"):
                // Go through all documents in a random order and check if there
                // is a Annotation document with the state NEW
                List<SourceDocument> randomList = documentService.listSourceDocuments(project);
                Collections.shuffle(randomList);
                Map<SourceDocument, AnnotationDocument> documentsOfCurrentUser = documentService
                        .listAnnotatableDocuments(project, user);
                for (SourceDocument doc : randomList) {
                    if ((workloadManagementService.getAmountOfUsersWorkingOnADocument(doc, project)
                            + 1) <= (dynamicWorkloadExtension.readTraits(currentWorkload)
                                    .getDefaultNumberOfAnnotations())
                            && (documentsOfCurrentUser.get(doc) == null
                                    || NEW.equals(documentsOfCurrentUser.get(doc).getState()))) {
                        getAnnotationPage().getModelObject().setDocument(doc,
                                documentService.listSourceDocuments(project));
                        getAnnotationPage().actionLoadDocument(_target);
                        return;
                    }
                }
                // No documents left, return to homepage and show corressponding message
                redirectUSerToHomePage();
                break;

            default:
                // Default, simply go through the list and return the first document
                for (Map.Entry<SourceDocument, AnnotationDocument> entry : documentService
                        .listAnnotatableDocuments(project, user).entrySet()) {
                    // First check if too many users are already working on the document
                    if (((workloadManagementService
                            .getAmountOfUsersWorkingOnADocument(entry.getKey(), project))
                            + 1) <= dynamicWorkloadExtension.readTraits(currentWorkload)
                                    .getDefaultNumberOfAnnotations()) {
                        // Now check if there either is no annotation document yet created or its
                        // state is NEW
                        if (entry.getValue() == null || NEW.equals(entry.getValue().getState())) {
                            getAnnotationPage().getModelObject().setDocument(entry.getKey(),
                                    documentService.listSourceDocuments(project));
                            getAnnotationPage().actionLoadDocument(_target);
                            return;
                        }
                    }
                }
                // No documents left, return to homepage and show corressponding message
                redirectUSerToHomePage();
                break;
            }
        });
        finishDocumentDialog.show(aTarget);
    }

    public void redirectUSerToHomePage()
    {
        // Nothing left, so returning to homepage and showing hint
        getAnnotationPage().setResponsePage(getAnnotationPage().getApplication().getHomePage());
        getAnnotationPage().getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
    }
}
