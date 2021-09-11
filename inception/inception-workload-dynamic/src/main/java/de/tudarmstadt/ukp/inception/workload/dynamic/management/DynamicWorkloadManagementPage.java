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
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.ASSIGNED;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.DOCUMENT;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.FINISHED;
import static de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys.STATE;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeaderlessColumn;
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
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.utils.RequestCycleUtils;
import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;
import com.googlecode.wicket.kendo.ui.KendoDataSource;
import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDatePicker;
import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import com.googlecode.wicket.kendo.ui.form.multiselect.lazy.MultiSelect;
import com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.AnnotatorColumn;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorStateOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueItem;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.DateSelection;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.WorkloadMetadataDialog;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.WorkflowType;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

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

    private static final String MID_LABEL = "label";

    // Forms
    private Form<Void> searchForm;
    private Form<Collection<SourceDocument>> userAssignDocumentForm;
    private Form<AnnotationDocument> userResetDocumentForm;

    // Current Project
    private final IModel<Project> currentProject = new Model<>();

    // Date provider
    private AnnotationQueueOverviewDataProvider dataProvider;

    // Input Fields
    private TextField<String> userFilterTextField;
    private TextField<String> documentFilterTextField;
    private AjaxDatePicker dateFrom;
    private AjaxDatePicker dateTo;
    private AjaxCheckBox unused;
    private BootstrapRadioChoice<DateSelection> dateChoices;
    private DropDownChoice<User> userSelection;
    private DropDownChoice<AnnotationDocument> resetDocument;
    private DropDownChoice<AnnotationDocumentState> documentState;
    private MultiSelect<SourceDocument> documentsToAdd;
    private WebMarkupContainer stateFilters;

    // Table
    private DataTable<AnnotationQueueItem, AnnotationQueueSortKeys> table;

    // Modal dialog
    private ModalWindow infoDialog;
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

        Project project = getProject();

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
        Label name = new Label("name", currentProject.getObject().getName());
        add(name);

        // Data Provider for the table
        dataProvider = makeAnnotationQueueOverviewDataProvider();

        infoDialog = new ModalWindow("infoDialog");
        add(infoDialog);

        // Columns of the table
        // Each column creates TableMetaData
        List<IColumn<AnnotationQueueItem, AnnotationQueueSortKeys>> columns = new ArrayList<>();
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentState"), STATE,
                item -> item.getState().symbol())
        {
            private static final long serialVersionUID = -2103168638018286379L;

            @Override
            public void populateItem(Item<ICellPopulator<AnnotationQueueItem>> item,
                    String componentId, IModel<AnnotationQueueItem> rowModel)
            {
                item.add(new Label(componentId, getDataModel(rowModel))
                        .setEscapeModelStrings(false));
            }
        });
        columns.add(new LambdaColumn<>(new ResourceModel("Document"), DOCUMENT,
                AnnotationQueueItem::getSourceDocumentName));
        columns.add(new LambdaColumn<>(new ResourceModel("Assigned"), ASSIGNED,
                AnnotationQueueItem::getInProgressCount));
        columns.add(new LambdaColumn<>(new ResourceModel("Finished"), FINISHED,
                AnnotationQueueItem::getFinishedCount));
        columns.add(new AnnotatorColumn(new ResourceModel("Annotators")));
        columns.add(new LambdaColumn<>(new ResourceModel("Updated"),
                AnnotationQueueItem::getLastUpdated));

        columns.add(new HeaderlessColumn<AnnotationQueueItem, AnnotationQueueSortKeys>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AnnotationQueueItem>> aItem,
                    String componentId, IModel<AnnotationQueueItem> rowModel)
            {
                Fragment fragment = new Fragment(componentId, "infoColumn",
                        DynamicWorkloadManagementPage.this);
                fragment.add(new LambdaAjaxLink("showInfoDialog",
                        _target -> actionShowInfoDialog(_target, rowModel)));
                aItem.add(fragment);
            }
        });

        table = new DataTable<>("dataTable", columns, dataProvider, 20);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, dataProvider));

        add(table);

        // Add StateFilters
        stateFilters = new WebMarkupContainer("stateFilters");
        ListView<SourceDocumentState> listview = new ListView<>("stateFilter",
                asList(SourceDocumentState.values()))
        {
            private static final long serialVersionUID = -2292408105823066466L;

            @Override
            protected void populateItem(ListItem<SourceDocumentState> aItem)
            {
                LambdaAjaxLink link = new LambdaAjaxLink("stateFilterLink",
                        (_target -> actionApplyStateFilter(_target, aItem.getModelObject())));

                link.add(new Label(MID_LABEL,
                        aItem.getModel().map(SourceDocumentState::symbol).orElse(""))
                                .setEscapeModelStrings(false));
                link.add(new AttributeAppender("class", () -> dataProvider.getFilterState()
                        .getStates().contains(aItem.getModelObject()) ? "active" : "", " "));
                aItem.add(link);
            }
        };

        stateFilters.add(enabledWhen(() -> !(dataProvider.getFilterState().getSelected())));

        stateFilters = new WebMarkupContainer("stateFilters");
        stateFilters.setOutputMarkupPlaceholderTag(true);
        stateFilters.add(listview);
        add(stateFilters);

        // Add forms of the dropdowns
        add(createSearchForm());
        add(createUserForm());
        add(createSettingsForm());
        add(new LambdaAjaxLink("refresh", this::actionRefresh));

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
        dateFrom = new AjaxDatePicker("from", PropertyModel.of(dataProvider, "filter.from"),
                "MM/dd/yyyy");
        dateFrom.setOutputMarkupId(true);
        dateTo = new AjaxDatePicker("to", PropertyModel.of(dataProvider, "filter.to"),
                "MM/dd/yyyy");
        dateTo.setOutputMarkupId(true);

        searchForm.add(dateFrom);
        searchForm.add(dateTo);

        // Date choices of the radio buttons
        List<DateSelection> dateChoice = Arrays.asList(DateSelection.values());

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
        DynamicWorkloadTraits traits = dynamicWorkloadExtension.readTraits(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(currentProject.getObject()));

        Form<DynamicWorkloadTraits> settingsForm = new Form<>("settingsForm",
                new CompoundPropertyModel<>(traits));
        settingsForm.setOutputMarkupId(true);

        settingsForm.add(new DocLink("workflowHelpLink", "sect_dynamic_workload"));

        settingsForm.add(new NumberTextField<>("defaultNumberOfAnnotations", Integer.class) //
                .setMinimum(1) //
                .setConvertEmptyInputStringToNull(false) //
                .setRequired(true));

        CheckBox abandonationToggle = new CheckBox("abandonationToggle");
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

        Component abandonationTimeout = new NumberTextField<>("abandonationTimeout", Long.class) //
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

        DropDownChoice<AnnotationDocumentState> abandonationState = new DropDownList<>(
                "abandonationState");
        abandonationState.setRequired(true);
        abandonationState.setNullValid(false);
        abandonationState.setChoiceRenderer(new EnumChoiceRenderer<>(abandonationState));
        abandonationState.setChoices(asList(AnnotationDocumentState.NEW,
                AnnotationDocumentState.IGNORE, AnnotationDocumentState.FINISHED));
        abandonationState.add(visibleWhen(abandonationToggle.getModel()));
        settingsForm.add(abandonationState);

        DropDownChoice<String> workflowChoices = new DropDownList<>("workflowType");
        workflowChoices.setChoiceRenderer(
                new LambdaChoiceRenderer<>(id -> workflowExtensionPoint.getExtension(id)
                        .map(WorkflowExtension::getLabel).orElse("<" + id + " not available>")));
        workflowChoices.setRequired(true);
        workflowChoices.setNullValid(false);
        workflowChoices.setChoices(workflowExtensionPoint.getTypes().stream() //
                .sorted(Comparator.comparing(WorkflowType::getUiName))
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
        Form<Void> userForm = new Form<>("userForm");
        userForm.setOutputMarkupId(true);

        // This form is split into three other forms, otherwise a
        // submit of the separate parts would not be possible.
        userForm.add(createUserSelectionForm());
        userForm.add(createUserAssignDocumentForm());
        userForm.add(createUserResetForm());

        return userForm;
    }

    /**
     * Creates the "User selection" part of the "Users" dropdown
     */
    private Form<User> createUserSelectionForm()
    {
        Form<User> userSelectionForm = new Form<>("userSelectionForm");
        userSelectionForm.setOutputMarkupId(true);

        // Dropdown menu with all available users of the project
        userSelection = new DropDownList<>("userSelection");
        userSelection.setChoiceRenderer(new LambdaChoiceRenderer<>(User::getUiName));
        // No default model required, as the input shall be empty by default
        userSelection.setModel(new Model<>());
        userSelection.setNullValid(true);
        userSelection.setChoices(this::getUsersForCurrentProject);
        userSelection.setOutputMarkupId(true);

        // Add AjaxUpdating Behavior to the dropdown
        userSelection.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> _target
                .add(userSelection, userAssignDocumentForm, userResetDocumentForm)));

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
        IModel<Collection<SourceDocument>> documentsToAddModel = new CollectionModel<>(
                new ArrayList<>());
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
            public List<SourceDocument> getChoices()
            {
                final String input = RequestCycleUtils
                        .getQueryParameterValue("filter[filters][0][value]").toString();

                List<SourceDocument> result = new ArrayList<>();

                if (userSelection.getModelObject() != null) {
                    if (input != null) {
                        for (SourceDocument sourceDocument : documentService
                                .listAnnotatableDocuments(currentProject.getObject(),
                                        userSelection.getModelObject())
                                .keySet()) {
                            if (sourceDocument.getName().contains(input)) {
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

    /**
     * Creates the "Reset document" part of the "Users" dropdown
     */
    private Form<AnnotationDocument> createUserResetForm()
    {
        userResetDocumentForm = new Form<>("userResetDocumentForm");

        // Required. We also want this form only to be visible when a
        // User has been selected in the other dropdown
        userResetDocumentForm.setOutputMarkupPlaceholderTag(true);
        userResetDocumentForm.add(visibleWhen(() -> !isNull(userSelection.getModelObject())));

        // Dropdown for all annotation documents for the user that exist in the DB
        resetDocument = new DropDownList<>("resetDocument");
        resetDocument.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationDocument::getName));
        resetDocument.setChoices(this::getCreatedDocumentsForSelectedUser);
        resetDocument.setModel(LoadableDetachableModel.of(this::getSelectedAnnotationDocument));
        resetDocument.setOutputMarkupId(true);
        userResetDocumentForm.add(resetDocument);

        // Dropdown for all states that can be assign to the selected document
        documentState = new DropDownList<>("documentState");
        documentState
                .setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationDocumentState::getName));
        documentState.setChoices(this::getAnnotationDocumentStates);
        documentState
                .setModel(LoadableDetachableModel.of(this::getSelectedAnnotationDocumentState));
        documentState.setOutputMarkupId(true);
        userResetDocumentForm.add(documentState);

        userResetDocumentForm.add(new LambdaAjaxButton<>("set", this::actionSetDocumentStatus));

        return userResetDocumentForm;

    }

    // -------------------- Helper methods --------------------------------//

    private List<User> getUsersForCurrentProject()
    {
        return projectService.listProjectUsersWithPermissions(currentProject.getObject());
    }

    private List<AnnotationDocument> getCreatedDocumentsForSelectedUser()
    {
        if (userSelection.getModelObject() == null) {
            return new ArrayList<>();
        }
        else {
            List<AnnotationDocument> sortedList = new ArrayList<>(
                    documentService.listAnnotationDocuments(currentProject.getObject(),
                            userSelection.getModelObject()));
            sortedList.sort(Comparator.comparing(AnnotationDocument::getName));
            return sortedList;
        }
    }

    private List<AnnotationDocumentState> getAnnotationDocumentStates()
    {
        return Arrays.stream(AnnotationDocumentState.values())
                .filter(s -> s.getName().equals(AnnotationDocumentState.IN_PROGRESS.getName())
                        || s.getName().equals(AnnotationDocumentState.FINISHED.getName()))
                .collect(Collectors.toList());
    }

    private AnnotationDocument getSelectedAnnotationDocument()
    {
        return resetDocument.getModelObject();
    }

    private AnnotationDocumentState getSelectedAnnotationDocumentState()
    {
        return documentState.getModelObject();
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

    private void actionSetDocumentStatus(AjaxRequestTarget aAjaxRequestTarget,
            Form<AnnotationDocumentState> aForm)
        throws IOException
    {
        aAjaxRequestTarget.addChildren(getPage(), IFeedback.class);

        if (resetDocument.getModelObject() == null) {
            error("Please select a document you wish to change its status first!");
            return;
        }

        if (documentState.getModelObject() == null) {
            error("Please select a state you wish to assign to the document");
            return;
        }

        // Either FINSIHED to INPROGRESS
        if (documentState.getModelObject() == AnnotationDocumentState.FINISHED) {
            documentService.setAnnotationDocumentState(resetDocument.getModelObject(),
                    AnnotationDocumentState.IN_PROGRESS);
            success("Document status changed");
        }
        else if (documentState.getModelObject() == AnnotationDocumentState.IN_PROGRESS) {

            // OR INPROGRESS to FINSIHED
            documentService.setAnnotationDocumentState(resetDocument.getModelObject(),
                    AnnotationDocumentState.FINISHED);
            success("Document status changed");
        }
        else {
            error("Can only change the status of documents that are in-progress or finished");
        }

        updateTable(aAjaxRequestTarget);
    }

    private void actionAssignDocument(AjaxRequestTarget aAjaxRequestTarget, Form<?> aForm)
        throws IOException
    {
        aAjaxRequestTarget.addChildren(getPage(), IFeedback.class);

        // First check if there are documents to assign
        Collection<SourceDocument> documentsToAssign = documentsToAdd.getModelObject();
        for (SourceDocument source : documentsToAssign) {
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(source, userSelection.getModelObject());

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

        aAjaxRequestTarget.add(userAssignDocumentForm, userResetDocumentForm);

        updateTable(aAjaxRequestTarget);
    }

    private void actionShowInfoDialog(AjaxRequestTarget aTarget, IModel<AnnotationQueueItem> aDoc)
    {
        // Get the current selected Row
        AnnotationQueueItem doc = aDoc.getObject();

        // Create the modalWindow
        infoDialog.setTitle("Metadata of document: " + doc.getSourceDocument().getName());

        // Set contents of the modalWindow
        List<String> finishedUsersForDocument = workloadManagementService
                .listAnnotationDocumentsForSourceDocumentInState(doc.getSourceDocument(),
                        AnnotationDocumentState.FINISHED)
                .stream().map(AnnotationDocument::getUser).collect(Collectors.toList());
        List<String> inProgressUsersForDocument = workloadManagementService
                .listAnnotationDocumentsForSourceDocumentInState(doc.getSourceDocument(),
                        AnnotationDocumentState.IN_PROGRESS)
                .stream().map(AnnotationDocument::getUser).collect(Collectors.toList());
        infoDialog.setContent(new WorkloadMetadataDialog(infoDialog.getContentId(),
                doc.getSourceDocument(), finishedUsersForDocument, inProgressUsersForDocument));

        // Open the dialog
        infoDialog.show(aTarget);
    }

    /**
     *
     * @param aTarget
     *            Updates the table with the new content, or at least refreshes it
     */
    private void updateTable(AjaxRequestTarget aTarget)
    {
        dataProvider.setAnnotationQueueItems(getQueue());
        aTarget.add(table);
    }

    private void actionApplyStateFilter(AjaxRequestTarget aTarget, SourceDocumentState aState)
    {
        List<SourceDocumentState> selectedStates = dataProvider.getFilterState().getStates();
        if (selectedStates.contains(aState)) {
            selectedStates.remove(aState);
        }
        else {
            selectedStates.add(aState);
        }
        aTarget.add(table, stateFilters);
    }

    private List<AnnotationQueueItem> getQueue()
    {
        Project project = currentProject.getObject();

        dynamicWorkloadExtension.freshenStatus(project);

        Map<SourceDocument, List<AnnotationDocument>> documentMap = new HashMap<>();

        documentService.listSourceDocuments(project)
                .forEach(d -> documentMap.put(d, new ArrayList<>()));

        documentService.listAnnotationDocuments(project).forEach(ad -> documentMap
                .computeIfAbsent(ad.getDocument(), d -> new ArrayList<>()).add(ad));

        DynamicWorkloadTraits traits = dynamicWorkloadExtension.readTraits(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(currentProject.getObject()));

        List<AnnotationQueueItem> queue = new ArrayList<>();
        for (Entry<SourceDocument, List<AnnotationDocument>> e : documentMap.entrySet()) {
            queue.add(new AnnotationQueueItem(e.getKey(), e.getValue(),
                    traits.getDefaultNumberOfAnnotations(), traits.getAbandonationTimeout()));
        }
        return queue;
    }

    private AnnotationQueueOverviewDataProvider makeAnnotationQueueOverviewDataProvider()
    {
        return new AnnotationQueueOverviewDataProvider(getQueue());
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aEvent.getSourceDocument(), aEvent.getUser());

        AnnotationDocumentState targetState = oneClickTransition(annotationDocument);

        documentService.setAnnotationDocumentState(annotationDocument, targetState);

        aEvent.getTarget().addChildren(getPage(), IFeedback.class);

        updateTable(aEvent.getTarget());
    }

    @OnEvent
    public void onAnnotatorStateOpenContextMenuEvent(AnnotatorStateOpenContextMenuEvent aEvent)
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
        items.add(new LambdaMenuItem("Touch",
                _target -> actionTouchAnnotationDocument(_target, document, user)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
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

            updateTable(_target);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionTouchAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            User aUser)
    {
        AnnotationDocument ann = documentService.getAnnotationDocument(aDocument, aUser);
        ann.setTimestamp(new Date());
        documentService.createAnnotationDocument(ann);

        success(format("The timestamp of document [%s] for user [%s] has been updated.",
                aDocument.getName(), aUser.getUiName()));
        aTarget.addChildren(getPage(), IFeedback.class);

        updateTable(aTarget);
    }
}
