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
package de.tudarmstadt.ukp.inception.workload.dynamic.management;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.oneClickTransition;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.ASSIGNED;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.DOCUMENT;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.FINISHED;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.STATE;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.KendoDataSource;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;
import org.wicketstuff.kendo.ui.renderer.ChoiceRenderer;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.comment.AnnotatorCommentDialogPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentFilterStateChanged;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentStateFilterPanel;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.bootstrap.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLambdaColumn;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.AnnotatorColumn;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellShowAnnotatorCommentEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorStateOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueItem;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.DateSelection;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.WorkflowType;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.ui.ResetAnnotationDocumentConfirmationDialogContentPanel;

/**
 * The workload page. It shall give project admins a fast overview on how each annotators progress
 * is. It consists mainly of a huge table with different filter mechanisms. It also grants the
 * ability to switch the way how documents are given to annotators (workflow). Thereupon, if an
 * annotator has mistakenly finished a document he/she did not wanted to, the workload page enables
 * a RESET of each documents state. Also, if specific documents are required to be annotated, a
 * project manager may assign a specific document to an annotator. Finally, to ensure a better
 * distribution of all documents, the project manager may set a "Default number of annotation" for
 * all documents. This number indicates on how often a document at most assigned to annotators.
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/workload")
public class DynamicWorkloadManagementPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 1180618893870240262L;

    public static final String CSS_CLASS_STATE_TOGGLE = "state-toggle";

    // Forms
    private Form<Void> searchForm;
    private Form<Collection<SourceDocument>> userAssignDocumentForm;

    // Current Project
    private final IModel<Project> currentProject = new Model<>();

    // Date provider
    private AnnotationQueueOverviewDataProvider dataProvider;

    // Input Fields
    private TextField<String> userFilterTextField;
    private TextField<String> documentFilterTextField;
    private DateTextField dateFrom;
    private DateTextField dateTo;
    private AjaxCheckBox unused;
    private BootstrapRadioChoice<DateSelection> dateChoices;
    private DropDownChoice<User> userSelection;
    private MultiSelect<SourceDocument> documentsToAdd;
    private WebMarkupContainer stateFilters;

    // Table
    private DataTable<AnnotationQueueItem, AnnotationQueueSortKeys> table;

    // Modal dialog
    private ModalDialog dialog;
    private ChallengeResponseDialog resetDocumentDialog;
    private ContextMenu contextMenu;

    // SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;
    private @SpringBean WorkflowExtensionPoint workflowExtensionPoint;
    private @SpringBean(name = "dynamicWorkloadManagementPageMenuItem") ProjectMenuItem pageMenuItem;

    public DynamicWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        requireProjectRole(userRepository.getCurrentUser(), CURATOR, MANAGER);

        var project = getProject();

        if (!pageMenuItem.applies(project)) {
            getSession().error("The project is not configured for dynamic workload management");
            backToProjectPage();
        }

        currentProject.setObject(project);

        commonInit();
    }

    public void commonInit()
    {
        // Header of the page
        queue(new Label("name", currentProject.getObject().getName()));

        // Data Provider for the table
        dataProvider = new AnnotationQueueOverviewDataProvider(getQueue());

        // Columns of the table
        var columns = new ArrayList<IColumn<AnnotationQueueItem, AnnotationQueueSortKeys>>();
        columns.add(new SymbolLambdaColumn<>(new ResourceModel("DocumentState"), STATE,
                item -> item.getState()));
        columns.add(new LambdaColumn<>(new ResourceModel("Document"), DOCUMENT,
                AnnotationQueueItem::getSourceDocumentName));
        columns.add(new LambdaColumn<>(new ResourceModel("Assigned"), ASSIGNED,
                AnnotationQueueItem::getInProgressCount));
        columns.add(new LambdaColumn<>(new ResourceModel("Finished"), FINISHED,
                AnnotationQueueItem::getFinishedCount));
        columns.add(new AnnotatorColumn(new ResourceModel("Annotators")));
        columns.add(new LambdaColumn<>(new ResourceModel("Updated"),
                AnnotationQueueItem::getLastUpdated));

        table = new DataTable<>("dataTable", columns, dataProvider, 100);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, dataProvider));
        queue(table);

        // Add StateFilters
        stateFilters = new SourceDocumentStateFilterPanel("stateFilters",
                () -> dataProvider.getFilterState().getStates());
        stateFilters.add(enabledWhen(() -> !(dataProvider.getFilterState().getSelected())));
        stateFilters.setOutputMarkupPlaceholderTag(true);
        add(stateFilters);

        // Add forms of the dropdowns
        add(createSearchForm());
        add(createUserForm());
        add(createSettingsForm());
        add(new LambdaAjaxLink("refresh", this::actionRefresh));

        dialog = new BootstrapModalDialog("modalDialog");
        add(dialog);

        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog"));
        add(contextMenu = new ContextMenu("contextMenu"));
    }

    /**
     * @return the "Filter" dropdown menu form
     */
    public Form<Void> createSearchForm()
    {
        searchForm = new Form<>("searchForm");
        searchForm.setOutputMarkupId(true);

        // Filter Textfields and their AJAX events
        userFilterTextField = new TextField<>("userFilter",
                PropertyModel.of(dataProvider, "filter.username"), String.class);
        userFilterTextField.setOutputMarkupId(true);

        documentFilterTextField = new TextField<>("documentFilter",
                PropertyModel.of(dataProvider, "filter.documentName"), String.class);
        documentFilterTextField.setOutputMarkupId(true);

        searchForm.add(userFilterTextField);
        searchForm.add(documentFilterTextField);

        // Input dates
        dateFrom = new DateTextField("from", PropertyModel.of(dataProvider, "filter.from"),
                "yyyy-MM-dd");
        dateFrom.setOutputMarkupId(true);
        dateTo = new DateTextField("to", PropertyModel.of(dataProvider, "filter.to"), "yyyy-MM-dd");
        dateTo.setOutputMarkupId(true);

        searchForm.add(dateFrom);
        searchForm.add(dateTo);

        // Date choices of the radio buttons
        var dateChoice = Arrays.asList(DateSelection.values());

        // Create the radio button group
        dateChoices = new BootstrapRadioChoice<>("date", new Model<>(DateSelection.between),
                dateChoice);
        dateChoices.setOutputMarkupId(true);
        dateChoices.setInline(true);
        dateChoices.setOutputMarkupId(true);

        // Update Behaviour on click, disable according date inputs and reset their values
        dateChoices.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            private static final long serialVersionUID = -7935623563910383563L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                dateTo.setModelObject(null);
                dateFrom.setModelObject(null);

                ajaxRequestTarget.add(dateFrom, dateTo);
            }
        });

        searchForm.add(dateChoices);

        // Checkbox for showing only unused source documents disables other textfields
        unused = new AjaxCheckBox("unused", PropertyModel.of(dataProvider, "filter.selected"))
        {
            private static final long serialVersionUID = 4975472693715689974L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                // Checks if checkbox is selected
                if (dataProvider.getFilterState().getSelected()) {
                    userFilterTextField.setModelObject(null);
                    documentFilterTextField.setModelObject(null);
                    dateFrom.setModelObject(null);
                    dateTo.setModelObject(null);
                }
                ajaxRequestTarget.add(userFilterTextField, documentFilterTextField, dateFrom,
                        dateTo);
            }
        };

        unused.setOutputMarkupId(true);
        searchForm.add(unused);

        // Condition for filter inputs to be enabled or disabled
        dateTo.add(enabledWhen(
                () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(1).name())
                        && !(dataProvider.getFilterState().getSelected())));
        dateFrom.add(enabledWhen(
                () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(0).name())
                        && !(dataProvider.getFilterState().getSelected())));
        dateChoices.add(enabledWhen(() -> !(dataProvider.getFilterState().getSelected())));
        userFilterTextField.add(enabledWhen(() -> !(dataProvider.getFilterState().getSelected())));
        documentFilterTextField
                .add(enabledWhen(() -> !(dataProvider.getFilterState().getSelected())));

        // Reset button
        searchForm.add(new LambdaAjaxButton<>("reset", this::actionReset));

        // Submit button
        searchForm.add(new LambdaAjaxButton<>("search", this::actionSubmit));

        return searchForm;
    }

    /**
     * @return the "Settings" dropdown menu form
     */
    public Form<DynamicWorkloadTraits> createSettingsForm()
    {
        var traits = dynamicWorkloadExtension.readTraits(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(currentProject.getObject()));

        var settingsForm = new Form<>("settingsForm", new CompoundPropertyModel<>(traits));
        settingsForm.setOutputMarkupId(true);

        settingsForm.add(new DocLink("workflowHelpLink", "sect_dynamic_workload"));

        settingsForm.add(new CheckBox("documentResetAllowed"));

        settingsForm.add(new CheckBox("confirmFinishingDocuments"));

        settingsForm.add(new NumberTextField<>("defaultNumberOfAnnotations", Integer.class) //
                .setMinimum(1) //
                .setConvertEmptyInputStringToNull(false) //
                .setRequired(true));

        var abandonationToggle = new CheckBox("abandonationToggle");
        abandonationToggle.setModel(new LambdaModelAdapter<Boolean>(() -> {
            Duration d = settingsForm.getModelObject().getAbandonationTimeout();
            return !(d.isNegative() || d.isZero());
        }, v -> {
            if (!v) {
                settingsForm.getModelObject().setAbandonationTimeout(Duration.ofMillis(0));
            }
            else {
                settingsForm.getModelObject().setAbandonationTimeout(Duration.ofDays(1));
            }
        }));
        abandonationToggle.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> _target.add(settingsForm)));
        settingsForm.add(abandonationToggle);

        var abandonationTimeout = new NumberTextField<>("abandonationTimeout", Long.class) //
                .setMinimum(0l) //
                .setConvertEmptyInputStringToNull(false) //
                .setRequired(true)
                .setModel(new LambdaModelAdapter<Long>(
                        () -> settingsForm.getModelObject().getAbandonationTimeout().toMinutes(),
                        v -> settingsForm.getModelObject().setAbandonationTimeout(ofMinutes(v))))
                .setOutputMarkupId(true);
        abandonationTimeout.add(visibleWhen(abandonationToggle.getModel()));
        settingsForm.add(abandonationTimeout);

        settingsForm.add(new LambdaAjaxButton<>("abandonationOneDay", (_target, _form) -> {
            settingsForm.getModelObject().setAbandonationTimeout(Duration.ofDays(1));
            _target.add(settingsForm);
        }).add(visibleWhen(abandonationToggle.getModel())));

        settingsForm.add(new LambdaAjaxButton<>("abandonationOneWeek", (_target, _form) -> {
            settingsForm.getModelObject().setAbandonationTimeout(Duration.ofDays(7));
            _target.add(settingsForm);
        }).add(visibleWhen(abandonationToggle.getModel())));

        var abandonationState = new DropDownChoice<AnnotationDocumentState>("abandonationState");
        abandonationState.setRequired(true);
        abandonationState.setNullValid(false);
        abandonationState.setChoiceRenderer(new EnumChoiceRenderer<>(abandonationState));
        abandonationState.setChoices(asList(AnnotationDocumentState.NEW,
                AnnotationDocumentState.IGNORE, AnnotationDocumentState.FINISHED));
        abandonationState.add(visibleWhen(abandonationToggle.getModel()));
        settingsForm.add(abandonationState);

        var workflowChoices = new DropDownChoice<String>("workflowType");
        workflowChoices.setChoiceRenderer(
                new LambdaChoiceRenderer<>(id -> workflowExtensionPoint.getExtension(id)
                        .map(WorkflowExtension::getLabel).orElse("<" + id + " not available>")));
        workflowChoices.setRequired(true);
        workflowChoices.setNullValid(false);
        workflowChoices.setChoices(workflowExtensionPoint.getTypes().stream() //
                .sorted(comparing(WorkflowType::getUiName)) //
                .map(WorkflowType::getWorkflowExtensionId) //
                .collect(toList()));
        settingsForm.add(workflowChoices);

        settingsForm.add(new LambdaAjaxButton<>("save", this::actionConfirm));

        return settingsForm;
    }

    /**
     * @return the "Users" dropdown menu form
     */
    public Form<Void> createUserForm()
    {
        var userForm = new Form<Void>("userForm");
        userForm.setOutputMarkupId(true);

        // This form is split into three other forms, otherwise a
        // submit of the separate parts would not be possible.
        userForm.add(createUserSelectionForm());
        userForm.add(createUserAssignDocumentForm());

        return userForm;
    }

    /**
     * Creates the "User selection" part of the "Users" dropdown
     */
    private Form<User> createUserSelectionForm()
    {
        var userSelectionForm = new Form<User>("userSelectionForm");
        userSelectionForm.setOutputMarkupId(true);

        // Dropdown menu with all available users of the project
        userSelection = new DropDownChoice<>("userSelection");
        userSelection.setChoiceRenderer(new LambdaChoiceRenderer<>(User::getUiName));
        // No default model required, as the input shall be empty by default
        userSelection.setModel(new Model<>());
        userSelection.setNullValid(true);
        userSelection.setChoices(this::getUsersForCurrentProject);
        userSelection.setOutputMarkupId(true);

        // Add AjaxUpdating Behavior to the dropdown
        userSelection.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> _target.add(userSelection, userAssignDocumentForm)));

        userSelectionForm.add(userSelection);

        return userSelectionForm;
    }

    /**
     * Creates the "Assign document" part of the "Users" dropdown
     */
    private Form<Collection<SourceDocument>> createUserAssignDocumentForm()
    {
        // Show ALL documents in the project (even those for which the user
        // does not have an annotations document created yet)
        var documentsToAddModel = new CollectionModel<>(new ArrayList<SourceDocument>());
        userAssignDocumentForm = new Form<>("userAssignDocumentForm", documentsToAddModel);

        // Required. We also want this form only to be visible when a
        // User has been selected in the other dropdown
        userAssignDocumentForm.setOutputMarkupPlaceholderTag(true);
        userAssignDocumentForm.add(visibleWhen(() -> !isNull(userSelection.getModelObject())));

        // This ensures that we get the user input in getChoices
        documentsToAdd = new MultiSelect<SourceDocument>("documentsToAdd",
                new ChoiceRenderer<>("name"))
        {
            private static final long serialVersionUID = -6211358256515198208L;

            @Override
            protected void onConfigure(KendoDataSource aDataSource)
            {
                // This ensures that we get the user input in getChoices
                aDataSource.set("serverFiltering", true);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("placeholder",
                        Options.asString(getString("documentsToAssign")));
                aBehavior.setOption("filter", Options.asString("contains"));
                aBehavior.setOption("autoClose", false);
            }

            @Override
            public List<SourceDocument> getChoices(String aInput)
            {
                var result = new ArrayList<SourceDocument>();

                if (userSelection.getModelObject() != null) {
                    if (aInput != null) {
                        for (var sourceDocument : documentService
                                .listAnnotatableDocuments(currentProject.getObject(),
                                        userSelection.getModelObject())
                                .keySet()) {
                            if (sourceDocument.getName().contains(aInput)) {
                                result.add(sourceDocument);
                            }
                        }
                    }
                    else {
                        result.addAll(
                                documentService.listAnnotatableDocuments(currentProject.getObject(),
                                        userSelection.getModelObject()).keySet());
                        result.sort(Comparator.comparing(SourceDocument::getName));
                    }
                }
                return result;
            }
        };

        documentsToAdd.setModel(documentsToAddModel);
        documentsToAdd.setOutputMarkupId(true);
        userAssignDocumentForm.add(documentsToAdd);

        // Add the "Confirm" button
        userAssignDocumentForm.add(new LambdaAjaxButton<>("confirm", this::actionAssignDocument));

        return userAssignDocumentForm;
    }

    // -------------------- Helper methods --------------------------------//

    private List<User> getUsersForCurrentProject()
    {
        return projectService.listUsersWithAnyRoleInProject(currentProject.getObject());
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        updateTable(aTarget);
    }

    private void actionSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        aTarget.add(table, searchForm);
    }

    private void actionReset(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        dateFrom.setModelObject(null);
        dateTo.setModelObject(null);
        unused.setModelObject(false);
        userFilterTextField.setModelObject(null);
        documentFilterTextField.setModelObject(null);
        dataProvider.getFilterState().setState(null);

        aTarget.add(table, searchForm);
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<DynamicWorkloadTraits> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        // Writes the new traits into the DB
        dynamicWorkloadExtension.writeTraits(aForm.getModelObject(), currentProject.getObject());
        success("Changes saved");

        updateTable(aTarget);
    }

    private void actionAssignDocument(AjaxRequestTarget aAjaxRequestTarget, Form<?> aForm)
        throws IOException
    {
        aAjaxRequestTarget.addChildren(getPage(), IFeedback.class);

        // First check if there are documents to assign
        var documentsToAssign = documentsToAdd.getModelObject();
        for (var source : documentsToAssign) {
            var annotationDocument = documentService.createOrGetAnnotationDocument(source,
                    userSelection.getModelObject());

            // Only if the document is in state NEW we can assign it to INPROGRESS
            if (AnnotationDocumentState.NEW == annotationDocument.getState()) {
                documentService.setAnnotationDocumentState(annotationDocument,
                        AnnotationDocumentState.IN_PROGRESS);
                success("Document(s) assigned");
            }
            else {
                error("Document [" + annotationDocument.getName()
                        + "] is either already assigned or even finished.");
            }
        }

        aAjaxRequestTarget.add(userAssignDocumentForm);

        updateTable(aAjaxRequestTarget);
    }

    /**
     * @param aTarget
     *            Updates the table with the new content, or at least refreshes it
     */
    private void updateTable(AjaxRequestTarget aTarget)
    {
        dataProvider.setAnnotationQueueItems(getQueue());
        aTarget.add(table);

    }

    @OnEvent
    public void onSourceDocumentFilterStateChanged(SourceDocumentFilterStateChanged aEvent)
    {
        aEvent.getTarget().add(table, stateFilters);
    }

    private List<AnnotationQueueItem> getQueue()
    {
        var project = currentProject.getObject();

        dynamicWorkloadExtension.freshenStatus(project);

        var documentMap = new HashMap<SourceDocument, List<AnnotationDocument>>();

        documentService.listSourceDocuments(project)
                .forEach(d -> documentMap.put(d, new ArrayList<>()));

        documentService.listAnnotationDocuments(project).forEach(ad -> documentMap
                .computeIfAbsent(ad.getDocument(), d -> new ArrayList<>()).add(ad));

        var traits = dynamicWorkloadExtension.readTraits(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(currentProject.getObject()));

        var queue = new ArrayList<AnnotationQueueItem>();
        for (var e : documentMap.entrySet()) {
            queue.add(new AnnotationQueueItem(e.getKey(), e.getValue(),
                    traits.getDefaultNumberOfAnnotations(), traits.getAbandonationTimeout()));
        }
        return queue;
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        var annotationDocument = documentService
                .createOrGetAnnotationDocument(aEvent.getSourceDocument(), aEvent.getUser());

        var targetState = oneClickTransition(annotationDocument);

        documentService.setAnnotationDocumentState(annotationDocument, targetState);

        aEvent.getTarget().addChildren(getPage(), IFeedback.class);

        updateTable(aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellShowAnnotatorCommentEvent aEvent)
    {
        var annDoc = documentService.getAnnotationDocument(aEvent.getSourceDocument(),
                aEvent.getUser());
        dialog.open(new AnnotatorCommentDialogPanel(ModalDialog.CONTENT_ID, Model.of(annDoc)),
                aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorStateOpenContextMenuEvent(AnnotatorStateOpenContextMenuEvent aEvent)
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
        items.add(new LambdaMenuItem("Touch",
                _target -> actionTouchAnnotationDocument(_target, document, user)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
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

            updateTable(_target);
        });

        dialog.open(dialogContent, aTarget);
    }

    private void actionTouchAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            User aUser)
    {
        var ann = documentService.getAnnotationDocument(aDocument, aUser);
        ann.setTimestamp(new Date());
        documentService.createOrUpdateAnnotationDocument(ann);

        success(format("The timestamp of document [%s] for user [%s] has been updated.",
                aDocument.getName(), aUser.getUiName()));
        aTarget.addChildren(getPage(), IFeedback.class);

        updateTable(aTarget);
    }
}
