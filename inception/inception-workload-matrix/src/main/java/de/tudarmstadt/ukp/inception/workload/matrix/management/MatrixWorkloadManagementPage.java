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
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVPrinter;
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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.SetModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.springframework.http.MediaType;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.comment.AnnotatorCommentDialogPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentFilterStateChanged;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentStateFilterPanel;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox.IconToggleBoxChangedEvent;
import de.tudarmstadt.ukp.inception.bootstrap.PopoverBehavior;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellShowAnnotatorCommentEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.DocumentRowSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.FilterStateChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixAnnotatorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixCuratorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixDataProvider;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixFilterState;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixNameColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixRow;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSelectColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixStateColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.UserSelectToolbar;
import de.tudarmstadt.ukp.inception.workload.matrix.service.MatrixWorkloadService;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.ui.ResetAnnotationDocumentConfirmationDialogContentPanel;
import de.tudarmstadt.ukp.inception.workload.ui.ResetCurationConfirmationDialogContentPanel;

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
    private @SpringBean MatrixWorkloadService matrixWorkloadService;

    private DataTable<DocumentMatrixRow, DocumentMatrixSortKey> documentMatrix;
    private LambdaAjaxLink toggleBulkChange;
    private WebMarkupContainer actionContainer;
    private WebMarkupContainer bulkActionDropdown;
    private WebMarkupContainer bulkActionDropdownButton;
    private ModalDialog modalDialog;
    private ContextMenu contextMenu;
    private DocumentMatrixDataProvider dataProvider;
    private IconToggleBox matchDocumentNameAsRegex;
    private IconToggleBox matchUserNameAsRegex;

    private boolean bulkChangeMode = false;
    private IModel<Set<String>> selectedUsers = new SetModel<>(new HashSet<>());
    private IModel<DocumentMatrixFilterState> filter;

    public MatrixWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var user = userRepository.getCurrentUser();

        var project = getProject();

        requireProjectRole(user, CURATOR, MANAGER);

        if (!pageMenuItem.applies(project)) {
            getSession().error("The project is not configured for static workload management");
            backToProjectPage();
        }

        modalDialog = new BootstrapModalDialog("modalDialog");
        add(modalDialog);

        add(new Label("name", project.getName()));

        add(new DocLink("documentStatusHelpLink", "_annotation_state_management"));

        filter = Model.of(new DocumentMatrixFilterState());

        add(documentMatrix = createDocumentMatrix("documentMatrix", bulkChangeMode));

        var documentNameFilter = new TextField<>("documentNameFilter",
                PropertyModel.of(dataProvider.getFilterState(), "documentName"), String.class);
        documentNameFilter.setOutputMarkupPlaceholderTag(true);
        documentNameFilter.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        queue(documentNameFilter);
        queue(matchDocumentNameAsRegex = new IconToggleBox("matchDocumentNameAsRegex",
                PropertyModel.of(dataProvider.getFilterState(), "matchDocumentNameAsRegex"))
                        .setPostLabelText(Model.of("(.*)")));

        var userNameFilter = new TextField<>("userNameFilter",
                PropertyModel.of(dataProvider.getFilterState(), "userName"), String.class);
        userNameFilter.setOutputMarkupPlaceholderTag(true);
        userNameFilter
                .add(new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionRefresh)
                        .withDebounce(ofMillis(200)));
        queue(userNameFilter);
        queue(matchUserNameAsRegex = new IconToggleBox("matchUserNameAsRegex",
                PropertyModel.of(dataProvider.getFilterState(), "matchUserNameAsRegex"))
                        .setPostLabelText(Model.of("(.*)")));

        queue(new SourceDocumentStateFilterPanel("stateFilters",
                () -> dataProvider.getFilterState().getStates()));

        add(createSettingsForm());

        var config = new PopoverConfig().withPlacement(Placement.left).withHtml(true);
        var legend = new WebMarkupContainer("legend");
        legend.add(new PopoverBehavior(new ResourceModel("legend"),
                new StringResourceModel("legend.content", legend), config));
        queue(legend);

        add(new LambdaAjaxLink("refresh", this::actionRefresh));

        add(new LambdaAjaxLink("assignWork", this::actionAssignWork));

        actionContainer = new WebMarkupContainer("actionContainer");
        actionContainer.setOutputMarkupPlaceholderTag(true);
        add(actionContainer);

        bulkActionDropdown = new WebMarkupContainer("bulkActionDropdown");
        bulkActionDropdown.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkStartProgress", this::actionBulkStart) //
                .add(visibleWhen(() -> DEVELOPMENT == getApplication().getConfigurationType())));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkLock", this::actionBulkLock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkUnlock", this::actionBulkUnlock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkFinish", this::actionBulkFinish));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkResume", this::actionBulkResume));
        bulkActionDropdown
                .add(new LambdaAjaxLink("bulkResumeCuration", this::actionBulkResumeCuration));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkOpen", this::actionBulkOpen));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkClose", this::actionBulkClose));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkReset", this::actionBulkResetDocument));
        bulkActionDropdown
                .add(new LambdaAjaxLink("bulkResetCuration", this::actionBulkResetCuration));
        actionContainer.add(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer("bulkActionDropdownButton");
        bulkActionDropdownButton.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        actionContainer.add(bulkActionDropdownButton);

        toggleBulkChange = new LambdaAjaxLink("toggleBulkChange", this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        actionContainer.add(toggleBulkChange);

        add(contextMenu = new ContextMenu("contextMenu"));

        var exportButton = new AjaxDownloadLink("export", () -> "workload.csv",
                this::exportWorkload);
        exportButton.add(visibleWhen(() -> documentMatrix.getItemCount() > 0));
        queue(exportButton);
    }

    private IResourceStream exportWorkload()
    {
        var annotators = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR).stream() //
                .map(User::getUsername) //
                .sorted() //
                .toList();

        return new PipedStreamResource(os -> {
            try (var aOut = new CSVPrinter(new OutputStreamWriter(os, UTF_8), EXCEL)) {
                var headers = new ArrayList<String>();
                headers.add("document name");
                headers.add("document state");
                headers.add("curation state");
                headers.addAll(annotators);

                aOut.printRecord(headers);

                var provider = documentMatrix.getDataProvider();
                var i = provider.iterator(0, provider.size());
                while (i.hasNext()) {
                    var rowIn = i.next();
                    var rowOut = new ArrayList<String>(headers.size());
                    rowOut.add(rowIn.getSourceDocument().getName());
                    rowOut.add(rowIn.getState().getId());
                    rowOut.add(rowIn.getCurationState().getId());
                    for (var annotator : annotators) {
                        var annDoc = rowIn.getAnnotationDocument(annotator);
                        if (annDoc != null) {
                            rowOut.add(annDoc.getState().getId());
                        }
                        else {
                            rowOut.add(AnnotationDocumentState.NEW.getId());
                        }
                    }
                    aOut.printRecord(rowOut);
                }
            }
        }, MediaType.valueOf("text/csv"));
    }

    @OnEvent
    public void onSourceDocumentFilterStateChanged(SourceDocumentFilterStateChanged aEvent)
    {
        actionApplyFilter(aEvent.getTarget());
    }

    @OnEvent
    public void onIconToggleBoxChanged(IconToggleBoxChangedEvent aEvent)
    {
        if (aEvent.getSource() == matchUserNameAsRegex) {
            actionRefresh(aEvent.getTarget());
        }

        if (aEvent.getSource() == matchDocumentNameAsRegex) {
            actionApplyFilter(aEvent.getTarget());
        }
    }

    /**
     * @return the "Settings" dropdown menu form
     */
    public Form<MatrixWorkloadTraits> createSettingsForm()
    {
        var traits = matrixWorkloadExtension.readTraits(
                workloadManagementService.loadOrCreateWorkloadManagerConfiguration(getProject()));

        var settingsForm = new Form<>("settingsForm", new CompoundPropertyModel<>(traits));
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
        dataProvider = new DocumentMatrixDataProvider(getMatrixData());

        // Copy sorting state from previous matrix
        if (documentMatrix != null) {
            var oldDataProvider = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider());
            dataProvider.setSort(oldDataProvider.getSort());
        }
        dataProvider.setFilterState(filter.getObject());

        var columns = new ArrayList<IColumn<DocumentMatrixRow, DocumentMatrixSortKey>>();
        var sourceDocumentSelectColumn = new DocumentMatrixSelectColumn();
        sourceDocumentSelectColumn.setVisible(bulkChangeMode);
        columns.add(sourceDocumentSelectColumn);
        columns.add(new DocumentMatrixStateColumn());
        columns.add(new DocumentMatrixNameColumn());
        columns.add(new DocumentMatrixCuratorColumn());

        var annotators = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR);

        if (StringUtils.isNotBlank(filter.getObject().getUserName())) {
            if (filter.getObject().isMatchUserNameAsRegex()) {
                var p = Pattern
                        .compile(".*(" + filter.getObject().getUserName() + ").*", CASE_INSENSITIVE)
                        .asMatchPredicate().negate();
                annotators.removeIf(u -> p.test(u.getUiName()));
            }
            else {

                annotators.removeIf(
                        u -> !containsIgnoreCase(u.getUiName(), filter.getObject().getUserName()));
            }
        }

        for (var annotator : annotators) {
            columns.add(new DocumentMatrixAnnotatorColumn(annotator, selectedUsers));
        }

        var table = new DefaultDataTable<DocumentMatrixRow, DocumentMatrixSortKey>(aComponentId,
                columns, dataProvider, 50);
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
        var selectedDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS || annDoc.getState() == FINISHED)
                .toList();

        if (selectedDocuments.isEmpty()) {
            info("No documents have been selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var dialogContent = new ResetAnnotationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID, Model.of(selectedDocuments));

        if (selectedDocuments.size() == 1) {
            var annDoc = selectedDocuments.get(0);
            var user = userRepository.get(annDoc.getUser());
            dialogContent.setExpectedResponseModel(
                    Model.of(user.getUiName() + " / " + annDoc.getName()));
        }
        else {
            dialogContent.setExpectedResponseModel(Model.of(getProject().getName()));
        }

        dialogContent.setConfirmAction(_target -> {
            var userCache = new HashMap<String, User>();
            for (var document : selectedDocuments) {
                var user = userCache.computeIfAbsent(document.getUser(),
                        username -> userRepository.get(username));
                documentService.resetAnnotationCas(document.getDocument(), user);
            }

            success(format("The %s document(s) have been set reset.", selectedDocuments.size()));
            _target.addChildren(getPage(), IFeedback.class);

            matrixWorkloadExtension.recalculate(getProject());

            reloadMatrixData();
            _target.add(documentMatrix);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionBulkResetCuration(AjaxRequestTarget aTarget)
    {
        var selectedDocuments = new ArrayList<>(selectedSourceDocuments());

        if (selectedDocuments.isEmpty()) {
            info("No documents have been selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var dialogContent = new ResetCurationConfirmationDialogContentPanel(ModalDialog.CONTENT_ID,
                Model.ofList(selectedDocuments));

        if (selectedDocuments.size() == 1) {
            var doc = selectedDocuments.get(0);
            dialogContent.setExpectedResponseModel(Model.of(doc.getName()));
        }
        else {
            dialogContent.setExpectedResponseModel(Model.of(getProject().getName()));
        }

        var statesToReset = asList(CURATION_IN_PROGRESS, CURATION_FINISHED);
        dialogContent.setConfirmAction(_target -> {
            var documentsToReset = selectedDocuments.stream() //
                    .filter(d -> statesToReset.contains(d.getState())) //
                    .toList();

            documentService.bulkSetSourceDocumentState(documentsToReset, ANNOTATION_IN_PROGRESS);

            for (var doc : documentsToReset) {
                curationService.deleteCurationCas(doc);
            }

            success(format("The curation state of %s document(s) has been set reset.",
                    documentsToReset.size()));
            _target.addChildren(getPage(), IFeedback.class);

            matrixWorkloadExtension.recalculate(getProject());

            reloadMatrixData();
            _target.add(documentMatrix);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionResetAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            User aUser)
    {
        var dialogContent = new ResetAnnotationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID);

        dialogContent.setExpectedResponseModel(
                Model.of(aUser.getUiName() + " / " + aDocument.getName()));
        dialogContent.setConfirmAction(_target -> {
            documentService.resetAnnotationCas(aDocument, aUser);

            success(format("The annotations of document [%s] for user [%s] have been set reset.",
                    aDocument.getName(), aUser.getUiName()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionResetCurationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        var dialogContent = new ResetCurationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID);

        dialogContent.setExpectedResponseModel(Model.of(aDocument.getName()));
        dialogContent.setConfirmAction(_target -> {
            documentService.setSourceDocumentState(aDocument, ANNOTATION_IN_PROGRESS);

            curationService.deleteCurationCas(aDocument);

            success(format("The curation of document [%s] has been set reset.",
                    aDocument.getName()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionBulkOpen(AjaxRequestTarget aTarget)
    {
        var selectedDocuments = selectedAnnotationDocuments();

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
        var selectedDocuments = selectedAnnotationDocuments();

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

    private void actionBulkResumeCuration(AjaxRequestTarget aTarget)
    {
        var finishedDocuments = selectedSourceDocuments().stream()
                .filter(doc -> doc.getState() == CURATION_FINISHED) //
                .toList();

        documentService.bulkSetSourceDocumentState(finishedDocuments, CURATION_IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The curation state of %d document(s) has been set to [%s]",
                finishedDocuments.size(), CURATION_IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionApplyFilter(AjaxRequestTarget aTarget)
    {
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

    private void actionAssignWork(AjaxRequestTarget aTarget)
    {
        var dialogContent = new AssignWorkDialogContentPanel(ModalDialog.CONTENT_ID,
                this::actionAssign);
        modalDialog.open(dialogContent, aTarget);
    }

    private void actionAssign(AjaxRequestTarget aTarget,
            Form<AssignWorkDialogContentPanel.AssignWorkRequest> aForm)
    {
        var request = aForm.getModelObject();

        matrixWorkloadService.assignWorkload(getProject(), request.getAnnotatorsPerDocument(),
                request.isOverride());
        modalDialog.close(aTarget);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private Collection<AnnotationDocument> selectedAnnotationDocuments()
    {
        var annotators = documentMatrix.getColumns().stream() //
                .filter(col -> col instanceof DocumentMatrixAnnotatorColumn) //
                .map(col -> (DocumentMatrixAnnotatorColumn) col) //
                .map(DocumentMatrixAnnotatorColumn::getUser) //
                .toList();

        var annotatorIndex = new HashMap<String, User>();
        annotators.forEach(annotator -> annotatorIndex.put(annotator.getUiName(), annotator));

        var selectedUserObjects = new HashSet<User>();
        selectedUsers.getObject().forEach(username -> {
            if (CURATION_USER.equals(username)) {
                return;
            }

            var u = annotatorIndex.get(username);
            if (u != null) {
                selectedUserObjects.add(u);
            }
        });

        var annotationDocumentsToChange = new HashSet<AnnotationDocument>();

        var rows = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider()).getMatrixData();

        // Collect annotation documents by row
        for (var row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            var usersWithoutAnnDocs = new ArrayList<User>();
            for (var annotator : annotators) {
                var annDoc = row.getAnnotationDocument(annotator.getUsername());
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
        for (var anotator : selectedUserObjects) {
            var sourceDocsWithoutAnnDocs = new ArrayList<SourceDocument>();
            for (var row : rows) {
                var annDoc = row.getAnnotationDocument(anotator.getUsername());
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

    private Collection<SourceDocument> selectedSourceDocuments()
    {
        var sourceDocumentsToChange = new HashSet<SourceDocument>();

        var rows = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider()).getMatrixData();

        // Collect annotation documents by row
        for (var row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            sourceDocumentsToChange.add(row.getSourceDocument());
        }

        // Collect annotation documents by column
        if (selectedUsers.map(users -> users.contains(CURATION_USER)).getObject()) {
            for (var row : rows) {
                sourceDocumentsToChange.add(row.getSourceDocument());
            }
        }

        return sourceDocumentsToChange;
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
        var annDoc = documentService.getAnnotationDocument(aEvent.getSourceDocument(),
                aEvent.getUser());
        modalDialog.open(new AnnotatorCommentDialogPanel(ModalDialog.CONTENT_ID, Model.of(annDoc)),
                aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        var annotationDocument = documentService
                .createOrGetAnnotationDocument(aEvent.getSourceDocument(), aEvent.getUser());

        var targetState = oneClickTransition(annotationDocument);

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

        var items = contextMenu.getItemList();
        items.clear();

        // The AnnotatorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        var document = aEvent.getSourceDocument();
        var user = aEvent.getUser();
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetAnnotationDocument(_target, document, user)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    @OnEvent
    public void onCuratorColumnCellOpenContextMenuEvent(
            CuratorColumnCellOpenContextMenuEvent aEvent)
    {
        var state = aEvent.getSourceDocument().getState();

        if (state != CURATION_IN_PROGRESS && state != CURATION_FINISHED) {
            info("Documents on which curation has not yet been started cannot be reset.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        var items = contextMenu.getItemList();
        items.clear();

        // The CuratorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        var document = aEvent.getSourceDocument();
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
        var annotators = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR).stream()
                .map(User::getUsername) //
                .collect(toSet());

        var documentMatrixRows = new LinkedHashMap<SourceDocument, DocumentMatrixRow>();
        for (var srcDoc : documentService.listSourceDocuments(getProject())) {
            documentMatrixRows.put(srcDoc, new DocumentMatrixRow(srcDoc, annotators));
        }

        for (var annDoc : documentService.listAnnotationDocuments(getProject())) {
            documentMatrixRows.get(annDoc.getDocument()).add(annDoc);
        }

        return new ArrayList<>(documentMatrixRows.values());
    }
}
