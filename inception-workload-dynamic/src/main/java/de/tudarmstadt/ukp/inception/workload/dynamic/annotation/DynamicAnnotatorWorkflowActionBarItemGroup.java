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
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
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
{
    private static final long serialVersionUID = 9215276761731631710L;

    private AnnotationPageBase page;

    protected ConfirmationDialog finishDocumentDialog;

    private final AnnotatorState annotatorState;
    private final LambdaAjaxLink finishDocumentLink;


    // SpringBeans
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;
    private @SpringBean EntityManager entityManager;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkflowExtensionPoint workflowExtensionPoint;

    /**
     * Constructor of the ActionBar
     */
    public DynamicAnnotatorWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;
        annotatorState = aPage.getModelObject();

        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));

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
            User user = annotatorState.getUser();
            Project project = annotatorState.getProject();
            SourceDocument document = annotatorState.getDocument();

            // On finishing, the current AnnotationDocument is put to the new state FINISHED
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(document,
                    user);
            documentService.transitionAnnotationDocumentState(annotationDocument,
                    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);

            _target.add(page);

            List<AnnotationDocument> inProgressDocuments = workloadManagementService
                    .getAnnotationDocumentListForUserWithState(project, user, IN_PROGRESS);

            // Assign a new document with actionLoadDocument

            // First, check if there are other documents which have been in the state INPROGRESS
            // Load the first one found
            if (!inProgressDocuments.isEmpty()) {
                annotatorState.setDocument(inProgressDocuments.get(0).getDocument(),
                        documentService.listSourceDocuments(project));
                getAnnotationPage().actionLoadDocument(_target);
                return;
            }

            // No annotation documents in the state INPROGRESS, now select a new one
            // depending on the workload strategy selected
            WorkloadManager currentWorkload = workloadManagementService
                    .loadOrCreateWorkloadManagerConfiguration(project);

            // Get all documents for which the state is NEW, or which have not been created yet.
            List<SourceDocument> sourceDocuments = workloadManagementService
                    .getAnnotationDocumentListForUser(project, user);

            // If there are no traits set yet, use the DefaultWorkflowExtension
            // otherwise select the current one
            WorkflowExtension currentWorkflowExtension = new DefaultWorkflowExtension();
            for (WorkflowExtension extension : workflowExtensionPoint.getExtensions()) {
                if (extension.getId().equals(
                        dynamicWorkloadExtension.readTraits(currentWorkload).getWorkflowType())) {
                    currentWorkflowExtension = extension;
                    break;
                }
            }
            // Rearrange list of documents according to current workflow
            sourceDocuments = currentWorkflowExtension.getNextDocument(sourceDocuments);

            // Load the new document, if loadNextDocument() returns false, redirect the user to the
            // homepage
            if (!currentWorkflowExtension.loadNextDocument(sourceDocuments, project,
                    currentWorkload, getAnnotationPage(), _target, workloadManagementService,
                    dynamicWorkloadExtension, documentService)) {
                redirectUserToHomePage();
            }
        });
        finishDocumentDialog.show(aTarget);
    }

    private void redirectUserToHomePage()
    {
        // Nothing left, so returning to homepage and showing hint
        getAnnotationPage().getSession().info(
                "There are no more documents to annotate available for you. Please contact your project supervisor.");
        getAnnotationPage().setResponsePage(getAnnotationPage().getApplication().getHomePage());
    }
}
