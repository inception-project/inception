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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.workload.workflow.types.RandomizedWorkflowExtension.RANDOMIZED_WORKFLOW;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

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

/**
 * This is only enabled for annotators of a project with the dynamic workload enabled. An annotator
 * is no more able to switch between documents before finishing the current one. This increases the
 * usablility of the data, as all documents are finished and not only "started". Depending on the
 * workflow strategy selected by the project manager, the document distribution varies.
 */
public class DynamicAnnotatorWorkflowActionBarItemGroup
    extends Panel
    implements Serializable
{
    private static final long serialVersionUID = 1731811997451414806L;

    private final AnnotationPageBase page;

    protected final ConfirmationDialog finishDocumentDialog;

    // SpringBeans
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;
    private @SpringBean EntityManager entityManager;
    private @SpringBean WorkloadManagementService workloadManagementService;

    /**
     * Constructor of the ActionBar
     */
    public DynamicAnnotatorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

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

    /**
     * This method represents the opening dialog upon clicking "Finish" for the current document.
     */
    protected void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((_target) -> {
            page.actionValidateDocument(_target, page.getEditorCas());

            // Needed often, therefore assigned in the beginning of the method
            AnnotatorState state = page.getModelObject();
            User user = state.getUser();
            Project project = state.getProject();
            SourceDocument document = state.getDocument();

            // On finishing, the current AnnotationDocument is put to the new state FINSIHED
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(document,
                    user);
            documentService.transitionAnnotationDocumentState(annotationDocument,
                    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            List<AnnotationDocument> inProgressDocuments = workloadManagementService
                    .getAnnotationDocumentListForUserWithState(project, user, IN_PROGRESS);

            // Assign a new document with actionLoadDocument

            // First, check if there are other documents which have been in the state INPROGRESS
            // Load the first one found
            if (!inProgressDocuments.isEmpty()) {
                getAnnotationPage().getModelObject().setDocument(
                        inProgressDocuments.get(0).getDocument(),
                        documentService.listSourceDocuments(project));
                getAnnotationPage().actionLoadDocument(aTarget);
                return;
            }

            // No annotation documents in the state INPROGRESS, now select a new one
            // depending on the workload strategy selected
            WorkloadManager currentWorkload = workloadManagementService
                    .loadOrCreateWorkloadManagerConfiguration(project);

            // Get all documents for which the state is NEW, or which have not been created yet.
            List<SourceDocument> sourceDocuments = workloadManagementService
                    .getAnnotationDocumentListForUser(project, user);

            // Switch for all workflow types which are available. If a new one is created.
            // simply add a new case here.
            switch (dynamicWorkloadExtension.readTraits(currentWorkload).getWorkflowType()) {

            // Go through all documents in a random order
            case (RANDOMIZED_WORKFLOW):
                // Shuffle the List then call loadNewDocument
                Collections.shuffle(sourceDocuments);
                loadNewDocument(sourceDocuments, project, currentWorkload, getAnnotationPage(),
                        _target);
                break;

            // Default workflow selected, nothing to change in the list
            default:
                loadNewDocument(sourceDocuments, project, currentWorkload, getAnnotationPage(),
                        _target);
                break;
            }
        });
        finishDocumentDialog.show(aTarget);
    }

    private void loadNewDocument(List<SourceDocument> aSourceDocuments, Project aProject,
            WorkloadManager aCurrentWorkload, AnnotationPageBase aPage, AjaxRequestTarget aTarget)
    {
        // Go through all documents of the list
        for (SourceDocument doc : aSourceDocuments) {
            // Check if there are less annotators working on the selected document than
            // the default number of annotation set by the project manager
            if ((workloadManagementService.getAmountOfUsersWorkingOnADocument(doc, aProject)
                    + 1) <= (dynamicWorkloadExtension.readTraits(aCurrentWorkload)
                            .getDefaultNumberOfAnnotations())) {
                // This was the case, so load the document and return
                aPage.getModelObject().setDocument(doc,
                        documentService.listSourceDocuments(aProject));
                aPage.actionLoadDocument(aTarget);
                return;
            }
        }
        // No documents left, return to homepage and show corresponding message
        redirectUSerToHomePage();

    }

    private void redirectUSerToHomePage()
    {
        // Nothing left, so returning to homepage and showing hint
        getAnnotationPage().setResponsePage(getAnnotationPage().getApplication().getHomePage());
        getAnnotationPage().getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
    }
}
