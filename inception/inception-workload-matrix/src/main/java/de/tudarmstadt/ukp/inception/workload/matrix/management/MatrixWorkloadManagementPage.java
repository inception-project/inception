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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
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
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Strings.CI;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.comment.AnnotatorCommentDialogPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
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
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.ui.core.config.DefaultMdcSetup;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellShowAnnotatorCommentEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.DocumentRowSelectionChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.FilterStateChangedEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.AnnotationSetSelectionToolbar;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixAnnotatorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixCuratorColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixDataProvider;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixFilterState;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixNameColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixRow;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSelectColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey;
import de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixStateColumn;
import de.tudarmstadt.ukp.inception.workload.matrix.service.MatrixWorkloadService;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.task.RecalculateProjectStateTask;
import de.tudarmstadt.ukp.inception.workload.ui.ProjectDocumentStatsPanel;
import de.tudarmstadt.ukp.inception.workload.ui.ProjectProgressDialogContentPanel;
import de.tudarmstadt.ukp.inception.workload.ui.ResetAnnotationDocumentConfirmationDialogContentPanel;
import de.tudarmstadt.ukp.inception.workload.ui.ResetCurationConfirmationDialogContentPanel;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/monitoring")
public class MatrixWorkloadManagementPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    private @SpringBean DocumentImportExportService documentImportExportService;
    private @SpringBean RepositoryProperties repositoryProperties;
    private @SpringBean SchedulingService schedulingService;

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
    private IModel<Set<AnnotationSet>> selectedUsers = new SetModel<>(new HashSet<>());
    private IModel<DocumentMatrixFilterState> filter;

    private AjaxDownloadBehavior downloadBehavior;

    private ProjectDocumentStatsPanel projectDocumentStatsPanel;

    private SymbolLabel stateIcon;

    private LoadableDetachableModel<SourceDocumentStateStats> stats;

    private static final Set<AnnotationDocumentState> EXPORTABLE_ANNOTATION_STATES = Set.of(
            AnnotationDocumentState.NEW, AnnotationDocumentState.IN_PROGRESS,
            AnnotationDocumentState.FINISHED);

    private static final Set<SourceDocumentState> EXPORTABLE_DOCUMENT_STATES = Set
            .of(SourceDocumentState.CURATION_FINISHED, SourceDocumentState.CURATION_IN_PROGRESS);

    private LoadableDetachableModel<Boolean> curationSelectedModel;

    private LoadableDetachableModel<Boolean> annotationSelectedModel;

    public MatrixWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var sessionOwner = userRepository.getCurrentUser();
        requireProjectRole(sessionOwner, CURATOR, MANAGER);

        var project = getProject();
        if (!pageMenuItem.applies(project)) {
            getSession().error("The project is not configured for static workload management");
            backToProjectPage();
        }

        downloadBehavior = new AjaxDownloadBehavior();
        add(downloadBehavior);

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

        curationSelectedModel = LoadableDetachableModel.of(() -> !selectedCuratorCells().isEmpty());
        annotationSelectedModel = LoadableDetachableModel
                .of(() -> !selectedAnnotatorCells().isEmpty());
        bulkActionDropdown = new WebMarkupContainer("bulkActionDropdown");
        bulkActionDropdown.setOutputMarkupId(true);
        bulkActionDropdown.add(visibleWhen(() -> bulkChangeMode));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkStartProgress", this::actionBulkStart) //
                .add(enabledWhen((this::onlyAnnotationCellsSelected))) //
                .add(visibleWhen(() -> DEVELOPMENT == getApplication().getConfigurationType())));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkLock", this::actionBulkLock)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkUnlock", this::actionBulkUnlock)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkFinish", this::actionBulkFinish)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkResume", this::actionBulkResume)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown
                .add(new LambdaAjaxLink("bulkResumeCuration", this::actionBulkResumeCuration)
                        .add(enabledWhen((this::onlyCurationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkOpen", this::actionBulkOpen)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkClose", this::actionBulkClose)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkReset", this::actionBulkResetDocument)
                .add(enabledWhen((this::onlyAnnotationCellsSelected))));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkExport", this::actionBulkExportDocument));
        bulkActionDropdown
                .add(new LambdaAjaxLink("bulkResetCuration", this::actionBulkResetCuration)
                        .add(enabledWhen((this::onlyCurationCellsSelected))));
        actionContainer.add(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer("bulkActionDropdownButton");
        bulkActionDropdownButton.add(visibleWhen(() -> bulkChangeMode));
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

        stats = LoadableDetachableModel.of(() -> dataProvider.getStats());
        projectDocumentStatsPanel = new ProjectDocumentStatsPanel("stats", stats);
        projectDocumentStatsPanel.setOutputMarkupId(true);
        add(projectDocumentStatsPanel);

        stateIcon = new SymbolLabel("stateIcon",
                stats.map(SourceDocumentStateStats::getProjectState));
        stateIcon.setOutputMarkupId(true);
        add(stateIcon);

        add(new LambdaAjaxLink("openProgress", this::actionOpenProgress));
    }

    private boolean onlyCurationCellsSelected()
    {
        return curationSelectedModel.getObject() == true
                && annotationSelectedModel.getObject() == false;
    }

    private boolean onlyAnnotationCellsSelected()
    {
        return curationSelectedModel.getObject() == false
                && annotationSelectedModel.getObject() == true;
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();
        curationSelectedModel.detach();
        annotationSelectedModel.detach();
    }

    private IResourceStream exportWorkload()
    {
        var users = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR);
        var annotators = buildAnnotatorList(users);

        return new PipedStreamResource(os -> {
            try (var aOut = new CSVPrinter(new OutputStreamWriter(os, UTF_8), EXCEL)) {
                var provider = documentMatrix.getDataProvider();
                var documentRows = provider.iterator(0, provider.size());
                exportWorkloadToCsv(aOut, annotators, documentRows);
            }
        }, MediaType.valueOf("text/csv"));
    }

    /**
     * Builds a sorted list of AnnotationSet objects from a list of users.
     * 
     * @param users
     *            the list of users to convert to AnnotationSets
     * @return a list of AnnotationSets sorted by display name
     */
    static List<AnnotationSet> buildAnnotatorList(List<User> users)
    {
        return users.stream() //
                .map(AnnotationSet::forUser) //
                .sorted(comparing(AnnotationSet::displayName)) //
                .toList();
    }

    /**
     * Exports workload data to CSV format.
     * 
     * @param csvPrinter
     *            the CSV printer to write to
     * @param annotators
     *            the list of annotators (as AnnotationSets) to include in the export
     * @param documentRows
     *            iterator over the document matrix rows to export
     * @throws IOException
     *             if an error occurs during CSV writing
     */
    static void exportWorkloadToCsv(CSVPrinter csvPrinter, List<AnnotationSet> annotators,
            Iterator<? extends DocumentMatrixRow> documentRows)
        throws IOException
    {
        // Write headers
        var headers = new ArrayList<String>();
        headers.add("document name");
        headers.add("document state");
        headers.add("curation state");
        annotators.forEach(s -> headers.add(s.displayName()));
        csvPrinter.printRecord(headers);

        // Write data rows
        while (documentRows.hasNext()) {
            var row = documentRows.next();
            var csvRow = new ArrayList<String>(headers.size());
            csvRow.add(row.getSourceDocument().getName());
            csvRow.add(row.getState().getId());
            csvRow.add(row.getCurationState().getId());
            for (var annotator : annotators) {
                var annDoc = row.getAnnotationDocument(annotator);
                if (annDoc != null) {
                    csvRow.add(annDoc.getState().getId());
                }
                else {
                    csvRow.add(AnnotationDocumentState.NEW.getId());
                }
            }
            csvPrinter.printRecord(csvRow);
        }
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

        var annotators = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR).stream() //
                .map(AnnotationSet::forUser) //
                .collect(toCollection(ArrayList::new));

        if (isNotBlank(filter.getObject().getUserName())) {
            if (filter.getObject().isMatchUserNameAsRegex()) {
                var p = Pattern
                        .compile(".*(" + filter.getObject().getUserName() + ").*", CASE_INSENSITIVE)
                        .asMatchPredicate().negate();
                annotators.removeIf(u -> p.test(u.displayName()));
            }
            else {
                annotators.removeIf(
                        u -> !CI.contains(u.displayName(), filter.getObject().getUserName()));
            }
        }

        columns.add(new DocumentMatrixCuratorColumn(selectedUsers));
        for (var annotator : annotators) {
            columns.add(new DocumentMatrixAnnotatorColumn(annotator, selectedUsers));
        }

        var table = new DefaultDataTable<DocumentMatrixRow, DocumentMatrixSortKey>(aComponentId,
                columns, dataProvider, 50);
        table.setOutputMarkupId(true);

        if (aBulkChangeMode) {
            table.addTopToolbar(new AnnotationSetSelectionToolbar(selectedUsers, table));
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
        var selectedCells = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS || annDoc.getState() == FINISHED)
                .toList();

        if (selectedCells.isEmpty()) {
            info("No documents have been selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var dialogContent = new ResetAnnotationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID, Model.of(selectedCells));

        if (selectedCells.size() == 1) {
            var annDoc = selectedCells.get(0);
            var user = userRepository.get(annDoc.getUser());
            dialogContent.setExpectedResponseModel(
                    Model.of(user.getUiName() + " / " + annDoc.getName()));
        }
        else {
            dialogContent.setExpectedResponseModel(Model.of(getProject().getName()));
        }

        dialogContent.setConfirmAction(_target -> {
            var userCache = new HashMap<String, User>();
            for (var document : selectedCells) {
                var user = userCache.computeIfAbsent(document.getUser(),
                        username -> userRepository.get(username));
                documentService.resetAnnotationCas(document.getDocument(), user);
            }

            success(format("The %s document(s) have been set reset.", selectedCells.size()));
            _target.addChildren(getPage(), IFeedback.class);

            matrixWorkloadExtension.recalculate(getProject());

            reloadMatrixData();
            _target.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
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
            _target.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionExportAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner)
        throws IOException
    {
        var dialogPanel = new FormatSelectionDialogContentPanel(ModalDialog.CONTENT_ID,
                (target, format) -> confirmedExportAnnotationDocument(target, format, aDocument,
                        aDataOwner));

        modalDialog.open(dialogPanel, aTarget);

    }

    private void confirmedExportAnnotationDocument(AjaxRequestTarget aTarget, String aFormat,
            SourceDocument aDocument, AnnotationSet aDataOwner)
        throws IOException
    {
        var formatSupport = documentImportExportService.getFormatById(aFormat).get();

        try {
            File file;
            file = exportAnnotationDocument(aDocument, aDataOwner, formatSupport, null);

            var docName = aDocument.getName();
            var baseName = removeExtension(docName);
            var fileExt = getExtension(file.getName());
            var filename = aDataOwner.id() + "/" + baseName + "." + fileExt;

            var exportResource = new PipedStreamResource(
                    os -> performExportAnnotationDocument(os, file));

            downloadBehavior.initiate(aTarget, filename, exportResource);
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }
    }

    private File exportAnnotationDocument(SourceDocument aDocument, AnnotationSet aDataOwner,
            FormatSupport formatSupport, Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws UIMAException, IOException
    {
        File file;
        if (CURATION_SET.equals(aDataOwner)
                && EXPORTABLE_DOCUMENT_STATES.contains(aDocument.getState())) {
            file = documentImportExportService.exportAnnotationDocument(aDocument,
                    CURATION_SET.id(), formatSupport, CURATION_SET.id(), CURATION, false,
                    aBulkOperationContext);
        }
        else {
            var annDoc = documentService.getAnnotationDocument(aDocument, aDataOwner);
            if (annDoc == null || annDoc.getState() == AnnotationDocumentState.NEW) {
                file = documentImportExportService.exportAnnotationDocument(aDocument,
                        aDataOwner.id(), formatSupport, INITIAL_SET.id(), ANNOTATION, false,
                        aBulkOperationContext);
            }
            else {
                file = documentImportExportService.exportAnnotationDocument(aDocument,
                        aDataOwner.id(), formatSupport, aDataOwner.id(), ANNOTATION, false,
                        aBulkOperationContext);
            }
        }

        return file;
    }

    private void performExportAnnotationDocument(OutputStream aOS, File aFile) throws IOException
    {
        try (var in = new FileInputStream(aFile)) {
            in.transferTo(aOS);
        }
        finally {
            if (aFile.exists() && !aFile.delete()) {
                aFile.deleteOnExit();
            }
        }
    }

    private void actionResetAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aUser)
    {
        var dialogContent = new ResetAnnotationDocumentConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID);

        var user = userRepository.get(aUser.id());
        dialogContent
                .setExpectedResponseModel(Model.of(user.getUiName() + " / " + aDocument.getName()));
        dialogContent.setConfirmAction(_target -> {
            documentService.resetAnnotationCas(aDocument, user);

            success(format("The annotations of document [%s] for user [%s] have been set reset.",
                    aDocument.getName(), user.getUiName()));
            _target.addChildren(getPage(), IFeedback.class);

            reloadMatrixData();
            _target.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
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
            _target.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
        });

        modalDialog.open(dialogContent, aTarget);
    }

    private void actionBulkOpen(AjaxRequestTarget aTarget)
    {
        var selectedDocuments = selectedAnnotatorCells();

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
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkClose(AjaxRequestTarget aTarget)
    {
        var selectedDocuments = selectedAnnotatorCells();

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
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkStart(AjaxRequestTarget aTarget)
    {
        var newDocuments = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == NEW) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", newDocuments.size(),
                IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkLock(AjaxRequestTarget aTarget)
    {
        var newDocuments = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == NEW) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, IGNORE);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", newDocuments.size(),
                IGNORE));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkUnlock(AjaxRequestTarget aTarget)
    {
        var lockedDocuments = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == IGNORE) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(lockedDocuments, NEW);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", lockedDocuments.size(),
                NEW));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkFinish(AjaxRequestTarget aTarget)
    {
        var inProgressDocuments = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, FINISHED);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]",
                inProgressDocuments.size(), FINISHED));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private void actionBulkResume(AjaxRequestTarget aTarget)
    {
        var finishedDocuments = selectedAnnotatorCells().stream()
                .filter(annDoc -> annDoc.getState() == FINISHED) //
                .collect(toList());

        documentService.bulkSetAnnotationDocumentState(finishedDocuments, IN_PROGRESS);

        matrixWorkloadExtension.recalculate(getProject());

        success(format("The state of %d document(s) has been set to [%s]", finishedDocuments.size(),
                IN_PROGRESS));
        aTarget.addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
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
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    record Cell(SourceDocument doc, AnnotationSet set) {};

    private void actionBulkExportDocument(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUser();
        var selectedDocuments = new ArrayList<Cell>();
        selectedAnnotatorCells().stream() //
                .filter(ad -> EXPORTABLE_ANNOTATION_STATES.contains(ad.getState())) //
                .map(ad -> new Cell(ad.getDocument(), ad.getAnnotationSet())) //
                .forEach(selectedDocuments::add);
        selectedCuratorCells().stream() //
                .filter(d -> EXPORTABLE_DOCUMENT_STATES.contains(d.getState())) //
                .map(d -> new Cell(d, CURATION_SET)) //
                .forEach(selectedDocuments::add);

        if (selectedDocuments.isEmpty()) {
            info("No documents have been selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var dialogPanel = new FormatSelectionDialogContentPanel(ModalDialog.CONTENT_ID,
                (target, format) -> confirmedBulkExportDocument(target, format, sessionOwner,
                        selectedDocuments));

        modalDialog.open(dialogPanel, aTarget);
    }

    private void confirmedBulkExportDocument(AjaxRequestTarget aTarget, String aFormat,
            User aSessionOwner, Collection<Cell> aSelectedDocuments)
        throws IOException
    {
        var filename = "export.zip";
        var mediaType = new MediaType("application", "zip");
        var exportResource = new PipedStreamResource(os -> performBulkExportDocument(os, aFormat,
                aSessionOwner, getProject(), aSelectedDocuments), mediaType);

        downloadBehavior.initiate(aTarget, filename, exportResource);
    }

    private void performBulkExportDocument(OutputStream aOs, String aFormat, User aSessionOwner,
            Project aProject, Collection<Cell> aSelectedDocuments)
        throws IOException
    {
        var bulkOperationContext = new HashMap<Pair<Project, String>, Object>();

        var formatSupport = documentImportExportService.getFormatById(aFormat).get();

        try (var ctx = new DefaultMdcSetup(repositoryProperties, aProject, aSessionOwner)) {
            try (var zipOS = new ZipOutputStream(aOs)) {
                for (var annDoc : aSelectedDocuments) {
                    var dataOwner = annDoc.set();
                    File file = null;

                    try {
                        file = exportAnnotationDocument(annDoc.doc(), annDoc.set(), formatSupport,
                                bulkOperationContext);

                        var docName = annDoc.doc().getName();
                        var baseName = removeExtension(docName);
                        var fileExt = getExtension(file.getName());

                        zipOS.putNextEntry(
                                new ZipEntry(dataOwner + "/" + baseName + "." + fileExt));
                        try (var in = new FileInputStream(file)) {
                            in.transferTo(zipOS);
                        }
                        zipOS.closeEntry();
                    }
                    catch (UIMAException e) {
                        throw new IOException(e);
                    }
                    finally {
                        if (file != null && file.exists() && !file.delete()) {
                            file.deleteOnExit();
                        }
                    }
                }
            }
        }
    }

    private void actionOpenProgress(AjaxRequestTarget aTarget)
    {
        var dialogContent = new ProjectProgressDialogContentPanel(ModalDialog.CONTENT_ID,
                getProjectModel());
        modalDialog.open(dialogContent, aTarget);
    }

    private void actionApplyFilter(AjaxRequestTarget aTarget)
    {
        aTarget.add(documentMatrix);
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        selectedUsers.getObject().clear();

        schedulingService.executeSync(RecalculateProjectStateTask.builder() //
                .withProject(getProject()) //
                .withTrigger("Workload configuration changed") //
                .build());

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
        aTarget.add(documentMatrix, stateIcon, projectDocumentStatsPanel);
    }

    private Collection<SourceDocument> selectedCuratorCells()
    {
        var selectedSets = documentMatrix.getColumns().stream()
                .filter(DocumentMatrixCuratorColumn.class::isInstance)
                .map(DocumentMatrixCuratorColumn.class::cast)
                .map(DocumentMatrixCuratorColumn::getAnnotationSet)
                .filter(selectedUsers.getObject()::contains).collect(Collectors.toSet());

        var selectedDocs = new HashSet<SourceDocument>();

        var rows = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .getFilteredMatrixData();

        // Collect annotation documents by row
        for (var row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            selectedDocs.add(row.getSourceDocument());
        }

        // Collect annotation documents by column
        if (selectedSets.contains(CURATION_SET)) {
            for (var row : rows) {
                selectedDocs.add(row.getSourceDocument());
            }
        }
        else {
            rows.stream() //
                    .filter(DocumentMatrixRow::isSelected) //
                    .map(DocumentMatrixRow::getSourceDocument) //
                    .forEach(selectedDocs::add);
        }

        return selectedDocs;
    }

    private Collection<AnnotationDocument> selectedAnnotatorCells()
    {
        var selectedSets = documentMatrix.getColumns().stream()
                .filter(DocumentMatrixAnnotatorColumn.class::isInstance)
                .map(DocumentMatrixAnnotatorColumn.class::cast)
                .map(DocumentMatrixAnnotatorColumn::getAnnotationSet)
                .filter(selectedUsers.getObject()::contains).collect(Collectors.toSet());

        var selectedAnnDocs = new HashSet<AnnotationDocument>();

        var rows = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .getFilteredMatrixData();

        // Collect annotation documents by row
        for (var row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            var setsWithoutAnnDocs = new ArrayList<AnnotationSet>();
            for (var annSet : selectedSets) {
                var annDoc = row.getAnnotationDocument(annSet);
                if (annDoc == null) {
                    setsWithoutAnnDocs.add(annSet);
                }
                else {
                    selectedAnnDocs.add(annDoc);
                }
            }

            selectedAnnDocs.addAll(documentService
                    .createOrGetAnnotationDocuments(row.getSourceDocument(), setsWithoutAnnDocs));
        }

        // Collect annotation documents by column
        for (var annSet : selectedSets) {
            var sourceDocsWithoutAnnDocs = new ArrayList<SourceDocument>();
            for (var row : rows) {
                var annDoc = row.getAnnotationDocument(annSet);
                if (annDoc == null) {
                    sourceDocsWithoutAnnDocs.add(row.getSourceDocument());
                }
                else {
                    selectedAnnDocs.add(annDoc);
                }
            }

            selectedAnnDocs.addAll(documentService
                    .createOrGetAnnotationDocuments(sourceDocsWithoutAnnDocs, annSet));
        }

        return selectedAnnDocs;
    }

    private Collection<SourceDocument> selectedSourceDocuments()
    {
        var sourceDocumentsToChange = new HashSet<SourceDocument>();

        var rows = ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .getFilteredMatrixData();

        // Collect annotation documents by row
        for (var row : rows) {
            if (!row.isSelected()) {
                continue;
            }

            sourceDocumentsToChange.add(row.getSourceDocument());
        }

        // Collect annotation documents by column
        if (selectedUsers.map(users -> users.contains(CURATION_SET)).getObject()) {
            for (var row : rows) {
                sourceDocumentsToChange.add(row.getSourceDocument());
            }
        }

        return sourceDocumentsToChange;
    }

    @OnEvent
    public void onAnnotatorColumnSelectionChangedEvent(AnnotatorColumnSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix, actionContainer);
    }

    @OnEvent
    public void onDocumentRowSelectionChangedEvent(DocumentRowSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix, actionContainer);
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
                aEvent.getAnnotationSet());
        modalDialog.open(new AnnotatorCommentDialogPanel(ModalDialog.CONTENT_ID, Model.of(annDoc)),
                aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(
                aEvent.getSourceDocument(), aEvent.getAnnotationSet());

        var targetState = oneClickTransition(annotationDocument);

        documentService.setAnnotationDocumentState(annotationDocument, targetState);
        success(format("The state of document [%s] for user [%s] has been set to [%s]",
                aEvent.getSourceDocument().getName(), aEvent.getAnnotationSet().displayName(),
                annotationDocument.getState()));

        aEvent.getTarget().addChildren(getPage(), IFeedback.class);

        reloadMatrixData();
        aEvent.getTarget().add(documentMatrix, stateIcon, projectDocumentStatsPanel);
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
        aEvent.getTarget().add(documentMatrix, stateIcon, projectDocumentStatsPanel);
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
        var user = aEvent.getAnnotationSet();
        items.add(new LambdaMenuItem("Export",
                _target -> actionExportAnnotationDocument(_target, document, user)));
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
        items.add(new LambdaMenuItem("Export",
                _target -> actionExportAnnotationDocument(_target, document, CURATION_SET)));
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetCurationDocument(_target, document)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    private void reloadMatrixData()
    {
        stats.detach();
        ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .setMatrixData(getMatrixData());
    }

    private List<DocumentMatrixRow> getMatrixData()
    {
        var annotators = projectService.listUsersWithRoleInProject(getProject(), ANNOTATOR).stream()
                .map(AnnotationSet::forUser) //
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
