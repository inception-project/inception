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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.oneClickTransition;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.SetModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.comment.AnnotatorCommentDialogPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellShowAnnotatorCommentEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.DocumentRowSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.FilterStateChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.AnnotatorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.CuratorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixDataProvider;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixRow;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.Filter;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.SourceDocumentNameColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.SourceDocumentSelectColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.SourceDocumentStateColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.UserSelectToolbar;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/monitoring")
public class MatrixWorkloadManagementPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    public static final String CSS_CLASS_STATE_TOGGLE = "state-toggle";
    public static final String CSS_CLASS_SELECTED = "s";

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean CurationDocumentService curationService;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean MatrixWorkloadExtension matrixWorkloadExtension;
    private @SpringBean(name = "matrixWorkloadManagementPageMenuItem") ProjectMenuItem pageMenuItem;

    private DataTable<DocumentMatrixRow, DocumentMatrixSortKey> documentMatrix;
    private LambdaAjaxLink toggleBulkChange;
    private WebMarkupContainer actionContainer;
    private WebMarkupContainer bulkActionDropdown;
    private WebMarkupContainer bulkActionDropdownButton;
    private ChallengeResponseDialog resetDocumentDialog;
    private ModalDialog modalDialog;
    private ContextMenu contextMenu;

    private boolean bulkChangeMode = false;
    private IModel<Set<String>> selectedUsers = new SetModel<>(new HashSet<>());
    private IModel<Filter> filter;

    public MatrixWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        User user = userRepository.getCurrentUser();

        Project project = getProject();

        requireProjectRole(user, CURATOR, MANAGER);

        if (!pageMenuItem.applies(project)) {
            getSession().error("The project is not configured for static workload management");
            backToProjectPage();
        }

        modalDialog = new BootstrapModalDialog("modalDialog");
        add(modalDialog);

        add(new Label("name", project.getName()));

        add(new DocLink("documentStatusHelpLink", "_annotation_state_management"));

        filter = Model.of(new Filter());
        add(new MatrixWorkloadFilterPanel("filterPanel", filter));

        add(documentMatrix = createDocumentMatrix("documentMatrix", bulkChangeMode));

        add(createSettingsForm());

        add(new LambdaAjaxLink("refresh", this::actionRefresh));

        actionContainer = new WebMarkupContainer("actionContainer");
        actionContainer.setOutputMarkupPlaceholderTag(true);
        add(actionContainer);

        bulkActionDropdown = new WebMarkupContainer("bulkActionDropdown");
        bulkActionDropdown.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        actionContainer.add(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer("bulkActionDropdownButton");
        bulkActionDropdownButton.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        actionContainer.add(bulkActionDropdownButton);

        toggleBulkChange = new LambdaAjaxLink("toggleBulkChange", this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        actionContainer.add(toggleBulkChange);

        bulkActionDropdown.add(new LambdaAjaxLink("bulkStartProgress", this::actionBulkStart)
                .add(visibleWhen(() -> DEVELOPMENT == getApplication().getConfigurationType())));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkLock", this::actionBulkLock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkUnlock", this::actionBulkUnlock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkFinish", this::actionBulkFinish));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkResume", this::actionBulkResume));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkOpen", this::actionBulkOpen));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkClose", this::actionBulkClose));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkReset", this::actionBulkResetDocument));

        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog"));
        add(contextMenu = new ContextMenu("contextMenu"));
    }

    /**
     * @return the "Settings" dropdown menu form
     */
    public Form<MatrixWorkloadTraits> createSettingsForm()
    {
        MatrixWorkloadTraits traits = matrixWorkloadExtension.readTraits(
                workloadManagementService.loadOrCreateWorkloadManagerConfiguration(getProject()));

        Form<MatrixWorkloadTraits> settingsForm = new Form<>("settingsForm",
                new CompoundPropertyModel<>(traits));
        settingsForm.setOutputMarkupId(true);

        settingsForm.add(new CheckBox("reopenableByAnnotator"));

        settingsForm.add(new LambdaAjaxButton<>("save", this::actionConfirm));

        return settingsForm;
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<MatrixWorkloadTraits> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        // Writes the new traits into the DB
        matrixWorkloadExtension.writeTraits(aForm.getModelObject(), getProject());
        success("Changes saved");
    }

    private DataTable<DocumentMatrixRow, DocumentMatrixSortKey> createDocumentMatrix(
            String aComponentId, boolean aBulkChangeMode)
    {
        DocumentMatrixDataProvider dataProvider = new DocumentMatrixDataProvider(getMatrixData());

        // Copy sorting state from previous matrix
        if (documentMatrix != null) {
            DocumentMatrixDataProvider oldDataProvider = ((DocumentMatrixDataProvider) documentMatrix
                    .getDataProvider());
            dataProvider.setSort(oldDataProvider.getSort());
        }
        dataProvider.setFilterState(filter.getObject());

        List<IColumn<DocumentMatrixRow, DocumentMatrixSortKey>> columns = new ArrayList<>();
        SourceDocumentSelectColumn sourceDocumentSelectColumn = new SourceDocumentSelectColumn();
        sourceDocumentSelectColumn.setVisible(bulkChangeMode);
        columns.add(sourceDocumentSelectColumn);
        columns.add(new SourceDocumentStateColumn());
        columns.add(new SourceDocumentNameColumn());
        columns.add(new CuratorColumn());

        List<User> annotators = projectService.listProjectUsersWithPermissions(getProject(),
                ANNOTATOR);

        if (StringUtils.isNotBlank(filter.getObject().getUserName())) {
            if (filter.getObject().isMatchUserNameAsRegex()) {
                Predicate<String> p = Pattern
                        .compile(".*(" + filter.getObject().getUserName() + ").*")
                        .asMatchPredicate().negate();
                annotators.removeIf(u -> p.test(u.getUiName()));
            }
            else {
                annotators.removeIf(u -> !u.getUiName().contains(filter.getObject().getUserName()));
            }
        }

        for (User annotator : annotators) {
            columns.add(new AnnotatorColumn(annotator, selectedUsers));
        }

        DataTable<DocumentMatrixRow, DocumentMatrixSortKey> table = new DefaultDataTable<>(
                aComponentId, columns, dataProvider, 50);
        table.setOutputMarkupId(true);

        if (aBulkChangeMode) {
            table.addTopToolbar(new UserSelectToolbar(selectedUsers, table));
        }

        return table;
    }

    private void actionToggleBulkChange(AjaxRequestTarget aTarget)
    {
        bulkChangeMode = !bulkChangeMode;
        actionRefresh(aTarget);
    }

    private void actionBulkResetDocument(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS || annDoc.getState() == FINISHED)
                .collect(Collectors.toList());

        IModel<String> projectNameModel = Model.of(getProject().getName());
        resetDocumentDialog
                .setTitleModel(new StringResourceModel("BulkResetDocumentDialog.title", this));
        resetDocumentDialog
                .setChallengeModel(new StringResourceModel("BulkResetDocumentDialog.text", this)
                        .setParameters(selectedDocuments.size(), projectNameModel));
        resetDocumentDialog.setResponseModel(projectNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            Map<String, User> userCache = new HashMap<>();
            for (AnnotationDocument document : selectedDocuments) {
                User user = userCache.computeIfAbsent(document.getUser(),
                        username -> userRepository.get(username));
                documentService.resetAnnotationCas(document.getDocument(), user);
            }

            success(format("The %s document(s) have been set reset.", selectedDocuments.size()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionResetAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            User aUser)
    {
        IModel<String> documentNameModel = Model.of(aDocument.getName());
        resetDocumentDialog
                .setTitleModel(new StringResourceModel("ResetDocumentDialog.title", this));
        resetDocumentDialog
                .setChallengeModel(new StringResourceModel("ResetDocumentDialog.text", this)
                        .setParameters(documentNameModel, aUser.getUiName()));
        resetDocumentDialog.setResponseModel(documentNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            documentService.resetAnnotationCas(aDocument, aUser);

            success(format("The annotations of document [%s] for user [%s] have been set reset.",
                    aDocument.getName(), aUser.getUiName()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionResetCurationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        IModel<String> documentNameModel = Model.of(aDocument.getName());
        resetDocumentDialog.setTitleModel( //
                new StringResourceModel("ResetCurationDocumentDialog.title", this));
        resetDocumentDialog.setChallengeModel( //
                new StringResourceModel("ResetCurationDocumentDialog.text", this)
                        .setParameters(documentNameModel));
        resetDocumentDialog.setResponseModel(documentNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            curationService.deleteCurationCas(aDocument);
            documentService.setSourceDocumentState(aDocument, ANNOTATION_IN_PROGRESS);

            success(format("The curation of document [%s] has been set reset.",
                    aDocument.getName()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionBulkOpen(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments();

        var lockedDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == IGNORE) //
                .collect(toList());
        documentService.bulkSetAnnotationDocumentState(lockedDocuments, NEW);

        var finishedDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == FINISHED) //
                .collect(toList());
        documentService.bulkSetAnnotationDocumentState(finishedDocuments, IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", lockedDocuments.size(),
                NEW));
        success(format("The state of %d document(s) has been set to [%s]", finishedDocuments.size(),
                IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkClose(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments();

        var newDocuments = selectedDocuments.stream().filter(annDoc -> annDoc.getState() == NEW) //
                .collect(toList());
        documentService.bulkSetAnnotationDocumentState(newDocuments, IGNORE);

        var inProgressDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS) //
                .collect(toList());
        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, FINISHED);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", newDocuments.size(),
                IGNORE));
        success(format("The state of %d document(s) has been set to [%s]",
                inProgressDocuments.size(), FINISHED));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkStart(AjaxRequestTarget aTarget)
    {
        var newDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == NEW) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", newDocuments.size(),
                IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkLock(AjaxRequestTarget aTarget)
    {
        var newDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == NEW) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, IGNORE);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", newDocuments.size(),
                IGNORE));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkUnlock(AjaxRequestTarget aTarget)
    {
        var lockedDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IGNORE) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(lockedDocuments, NEW);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", lockedDocuments.size(),
                NEW));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkFinish(AjaxRequestTarget aTarget)
    {
        var inProgressDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, FINISHED);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]",
                inProgressDocuments.size(), FINISHED));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkResume(AjaxRequestTarget aTarget)
    {
        var finishedDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == FINISHED) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(finishedDocuments, IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", finishedDocuments.size(),
                IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        selectedUsers.getObject().clear();

        var newMatrix = createDocumentMatrix("documentMatrix", bulkChangeMode);
        documentMatrix.replaceWith(newMatrix);
        documentMatrix = newMatrix;

        aTarget.add(documentMatrix, toggleBulkChange, actionContainer);
    }

    private Collection<AnnotationDocument> selectedAnnotationDocuments()
    {
        List<User> annotators = documentMatrix.getColumns().stream() //
                .filter(col -> col instanceof AnnotatorColumn) //
                .map(col -> (AnnotatorColumn) col) //
                .map(AnnotatorColumn::getUser) //
                .collect(toList());

        Map<String, User> annotatorIndex = new HashMap<>();
        annotators.forEach(annotator -> annotatorIndex.put(annotator.getUiName(), annotator));

        Set<User> selectedUserObjects = new HashSet<>();
        selectedUsers.getObject().forEach(username -> {
            User u = annotatorIndex.get(username);
            if (u != null) {
                selectedUserObjects.add(u);
            }
        });

        Set<AnnotationDocument> annotationDocumentsToChange = new HashSet<>();

        List<DocumentMatrixRow> rows = ((DocumentMatrixDataProvider) documentMatrix
                .getDataProvider()).getMatrixData();

        // Collect annotation documents by row
        for (DocumentMatrixRow row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            List<User> usersWithoutAnnDocs = new ArrayList<>();
            for (User annotator : annotators) {
                AnnotationDocument annDoc = row.getAnnotationDocument(annotator.getUsername());
                if (annDoc == null) {
                    usersWithoutAnnDocs.add(annotator);
                }
                else {
                    annotationDocumentsToChange.add(annDoc);
                }
            }

            annotationDocumentsToChange.addAll(documentService
                    .createOrGetAnnotationDocuments(row.getSourceDocument(), usersWithoutAnnDocs));
        }

        // Collect annotation documents by column
        for (User anotator : selectedUserObjects) {
            List<SourceDocument> sourceDocsWithoutAnnDocs = new ArrayList<>();
            for (DocumentMatrixRow row : rows) {
                AnnotationDocument annDoc = row.getAnnotationDocument(anotator.getUsername());
                if (annDoc == null) {
                    sourceDocsWithoutAnnDocs.add(row.getSourceDocument());
                }
                else {
                    annotationDocumentsToChange.add(annDoc);
                }
            }

            annotationDocumentsToChange.addAll(documentService
                    .createOrGetAnnotationDocuments(sourceDocsWithoutAnnDocs, anotator));
        }

        return annotationDocumentsToChange;
    }

    @OnEvent
    public void onAnnotatorColumnSelectionChangedEvent(AnnotatorColumnSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onDocumentRowSelectionChangedEvent(DocumentRowSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onFilterStateChangedEvent(FilterStateChangedEvent aEvent)
    {
        reloadMatrixData();
        actionRefresh(aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellShowAnnotatorCommentEvent aEvent)
    {
        AnnotationDocument annDoc = documentService
                .getAnnotationDocument(aEvent.getSourceDocument(), aEvent.getUser());
        modalDialog.open(new AnnotatorCommentDialogPanel(ModalDialog.CONTENT_ID, Model.of(annDoc)),
                aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aEvent.getSourceDocument(), aEvent.getUser());

        AnnotationDocumentState targetState = oneClickTransition(annotationDocument);

        documentService.setAnnotationDocumentState(annotationDocument, targetState);
        success(format("The state of document [%s] for user [%s] has been set to [%s]",
                aEvent.getSourceDocument().getName(), aEvent.getUser().getUiName(),
                annotationDocument.getState()));

        aEvent.getTarget().addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onCuratorColumnCellClickEvent(CuratorColumnCellClickEvent aEvent)
    {
        switch (aEvent.getSourceDocument().getState()) {
        case CURATION_IN_PROGRESS:
            documentService.transitionSourceDocumentState(aEvent.getSourceDocument(),
                    CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
            break;
        case CURATION_FINISHED:
            documentService.transitionSourceDocumentState(aEvent.getSourceDocument(),
                    CURATION_FINISHED_TO_CURATION_IN_PROGRESS);
            break;
        default:
            info("Curation state can only be changed once curation has started.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        success(format("The curation state of document [%s] has been set to [%s]",
                aEvent.getSourceDocument().getName(), aEvent.getSourceDocument().getState()));
        aEvent.getTarget().addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onAnnotatorColumnCellOpenContextMenuEvent(
            AnnotatorColumnCellOpenContextMenuEvent aEvent)
    {
        if (aEvent.getState() == NEW) {
            info("Documents on which work has not yet been started cannot be reset.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        List<IMenuItem> items = contextMenu.getItemList();
        items.clear();

        // The AnnotatorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        SourceDocument document = aEvent.getSourceDocument();
        User user = aEvent.getUser();
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetAnnotationDocument(_target, document, user)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    @OnEvent
    public void onCuratorColumnCellOpenContextMenuEvent(
            CuratorColumnCellOpenContextMenuEvent aEvent)
    {
        SourceDocumentState state = aEvent.getSourceDocument().getState();

        if (state != CURATION_IN_PROGRESS && state != CURATION_FINISHED) {
            info("Documents on which curation has not yet been started cannot be reset.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        List<IMenuItem> items = contextMenu.getItemList();
        items.clear();

        // The CuratorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        SourceDocument document = aEvent.getSourceDocument();
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetCurationDocument(_target, document)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    private void reloadMatrixData()
    {
        ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .setMatrixData(getMatrixData());
    }

    private List<DocumentMatrixRow> getMatrixData()
    {
        Set<String> annotators = projectService
                .listProjectUsersWithPermissions(getProject(), ANNOTATOR).stream()
                .map(User::getUsername) //
                .collect(toSet());

        Map<SourceDocument, DocumentMatrixRow> documentMatrixRows = new LinkedHashMap<>();
        for (SourceDocument srcDoc : documentService.listSourceDocuments(getProject())) {
            documentMatrixRows.put(srcDoc, new DocumentMatrixRow(srcDoc, annotators));
        }

        for (AnnotationDocument annDoc : documentService.listAnnotationDocuments(getProject())) {
            documentMatrixRows.get(annDoc.getDocument()).add(annDoc);
        }

        return new ArrayList<>(documentMatrixRows.values());
    }
}
