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
package de.tudarmstadt.ukp.inception.workload.dynamic.management;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeaderlessColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDatePicker;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManagementService;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManager;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.WorkloadMetadataDialog;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowManagerType;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;

@MountPath("/workload.html")
public class DynamicWorkloadManagementPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1180618893870240262L;

    private IModel<Project> currentProject = new Model<>();

    private NumberTextField<Integer> defaultNumberDocumentsTextField;

    private DataTable<SourceDocument, String> table;
    private ModalWindow infoDialog;

    private AjaxDatePicker dateFrom;
    private AjaxDatePicker dateTo;
    private AjaxCheckBox unused;

    private TextField<String> userFilterTextField;
    private TextField<String> documentFilterTextField;

    private BootstrapRadioChoice<String> dateChoices;

    private BootstrapSelect<WorkflowManagerType> workflowChoices;
    private BootstrapSelect<String> userSelection;
    private BootstrapSelect<String> resetDocument;
    private BootstrapSelect<String> assignDocument;
    private BootstrapSelect<String> documentState;

    private Form<Void> searchForm;
    private Form<Void> settingsForm;
    private Form<Void> userSelectionForm;
    private Form<Void> userForm;
    private Form<Void> userDetailForm;

    private User selectedUser;

    // SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;
    private @SpringBean DynamicWorkflowManagementService dynamicWorkflowManagementService;
    private @SpringBean WorkflowManagerExtensionPoint workflowManagerExtensionPoint;

    public DynamicWorkloadManagementPage()
    {
        super();
        // Error, user is returned to home page, nothing else to do
        // getSession required to show the message at the homepage
        setResponsePage(getApplication().getHomePage());
        getSession().error("No project selected.");
    }

    public DynamicWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        // Get current Project
        currentProject.setObject(
                getProjectFromParameters(aPageParameters.get(PAGE_PARAM_PROJECT_ID)).get());

        commonInit();
    }

    public void commonInit()
    {
        // Header of the page
        Label name = new Label("name", currentProject.getObject().getName());
        add(name);

        // Headers of the table
        List<String> headers = new ArrayList<>();
        headers.add(getString("Document"));
        headers.add(getString("Finished"));
        headers.add(getString("Processed"));
        headers.add(getString("Users"));
        headers.add(getString("Updated"));
        headers.add(getString("actions"));

        // 13/05/2020 15:16:57 // Christoph

        // Data Provider for the table
        AnnotationQueueOverviewDataProvider dataProvider = new AnnotationQueueOverviewDataProvider(
                documentService.listSourceDocuments(currentProject.getObject()), headers,
                documentService.listAnnotationDocuments(currentProject.getObject()));

        infoDialog = new ModalWindow("infoDialog");
        add(infoDialog);

        // Columns of the table
        // Each column creates TableMetaData
        List<IColumn<SourceDocument, String>> columns = new ArrayList<>();
        columns.add(new LambdaColumn<>(new ResourceModel("Document"), getString("Document"),
            SourceDocument::getName));
        columns.add(new LambdaColumn<>(new ResourceModel("Finished"), getString("Finished"),
            _doc -> dataProvider.getFinishedAmountForDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel("Processed"), getString("Processed"),
            _doc -> dataProvider.getInProgressAmountForDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel("Users"), getString("Users"),
            _doc -> dataProvider.getUsersWorkingOnTheDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel("Updated"),
            _doc -> dataProvider.lastAccessTimeForDocument((SourceDocument) _doc)));

        // Own column type, contains only a clickable image (AJAX event),
        // creates a small panel dialog containing metadata
        columns.add(new HeaderlessColumn<SourceDocument, String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SourceDocument>> aItem, String componentId,
                    IModel<SourceDocument> rowModel)
            {
                Fragment fragment = new Fragment(componentId, "infoColumn",
                    DynamicWorkloadManagementPage.this);
                fragment.add(new LambdaAjaxLink("showInfoDialog",
                    _target -> actionShowInfoDialog(_target, rowModel)));
                aItem.add(fragment);
            };
        });

        table = new DataTable<>("dataTable", columns, dataProvider, 20);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, dataProvider));

        add(table);

        add(createSearchForm(dataProvider));
        add(createSettingsForm());
        add(createUserForm());
    }

    private void actionShowInfoDialog(AjaxRequestTarget aTarget, IModel<SourceDocument> aDoc)
    {
        // Get the current selected Row
        SourceDocument doc = aDoc.getObject();

        // Create the modalWindow
        infoDialog.setTitle("Metadata of document: " + doc.getName());

        // Set contents of the modalWindow
        infoDialog.setContent(new WorkloadMetadataDialog(infoDialog.getContentId(), doc,
                listUsersFinishedForDocument(doc), listUsersInProgressForDocument(doc)));

        // Open the dialog
        infoDialog.show(aTarget);
    }

    private Optional<Project> getProjectFromParameters(StringValue aProjectParam)
    {
        if (aProjectParam == null || aProjectParam.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(projectService.getProject(aProjectParam.toLong()));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<String> listUsersFinishedForDocument(SourceDocument aDocument)
    {
        List<String> result = new ArrayList<>();
        for (AnnotationDocument anno : documentService
                .listAnnotationDocuments(currentProject.getObject())) {
            if (anno.getState().equals(AnnotationDocumentState.FINISHED)
                    && anno.getName().equals(aDocument.getName())) {
                result.add(anno.getUser());
            }
        }
        return result;
    }

    public List<String> listUsersInProgressForDocument(SourceDocument aDocument)
    {
        List<String> result = new ArrayList<>();
        for (AnnotationDocument anno : documentService
                .listAnnotationDocuments(currentProject.getObject())) {
            if (anno.getState().equals(AnnotationDocumentState.IN_PROGRESS)
                    && anno.getDocument().equals(aDocument)) {
                result.add(anno.getUser());
            }
        }
        return result;
    }

    public Form<Void> createSearchForm(AnnotationQueueOverviewDataProvider aProv)
    {
        searchForm = new Form<>("searchForm");

        searchForm.setOutputMarkupId(true);

        // Filter Textfields and their AJAX events
        userFilterTextField = new TextField<>("userFilter",
                PropertyModel.of(aProv, "filter.username"), String.class);

        userFilterTextField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        documentFilterTextField = new TextField<>("documentFilter",
                PropertyModel.of(aProv, "filter.documentName"), String.class);

        documentFilterTextField.setOutputMarkupId(true);

        searchForm.add(userFilterTextField);
        searchForm.add(documentFilterTextField);

        // Input dates
        dateFrom = new AjaxDatePicker("from", PropertyModel.of(aProv, "filter.from"), "MM/dd/yyyy");
        dateTo = new AjaxDatePicker("to", PropertyModel.of(aProv, "filter.to"), "MM/dd/yyyy");

        searchForm.add(dateFrom);
        searchForm.add(dateTo);

        // Date choices
        // FIXME: This should use an Enum value as model - and I18N should be used to map the
        // enum's values to the display values
        List<String> dateChoice = new ArrayList<>();
        dateChoice.add(getString("from"));
        dateChoice.add(getString("until"));
        dateChoice.add(getString("between"));

        // Create the radio button group
        dateChoices = new BootstrapRadioChoice<>("date", new Model<>(getString("between")),
                dateChoice);
        dateChoices.setInline(true);
        dateChoices.setOutputMarkupId(true);

        // Update Behaviour on click, disable according date inputs and reset their values
        dateChoices.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                if (getDefaultModelObjectAsString().equals(dateChoice.get(0))) {
                    dateTo.setModelObject(null);
                }
                else if (getDefaultModelObjectAsString().equals(dateChoice.get(1))) {
                    dateFrom.setModelObject(null);
                }

                ajaxRequestTarget.add(dateFrom);
                ajaxRequestTarget.add(dateTo);
            }
        });

        // add them to the form
        searchForm.add(dateChoices);

        // Checkbox for showing only unused source documents, disables other textfields
        unused = new AjaxCheckBox("unused", PropertyModel.of(aProv, "filter.selected"))
        {

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                if (getDefaultModelObjectAsString().equals("true")) {
                    userFilterTextField.setModelObject(null);
                    dateFrom.setModelObject(null);
                    dateTo.setModelObject(null);
                }
                ajaxRequestTarget.add(searchForm);
            }
        };

        unused.setOutputMarkupId(true);
        searchForm.add(unused);

        // Reset button
        Button reset = new LambdaAjaxButton("reset", this::actionReset);

        searchForm.add(reset);

        // Submit button
        Button search = new LambdaAjaxButton("search", this::actionSubmit);

        searchForm.add(search);

        // Condition for filter inputs to be enabled
        dateTo.add(enabledWhen(
            () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(0))
                && unused.getValue().equals("false")));
        dateFrom.add(enabledWhen(
            () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(1))
                && unused.getValue().equals("false")));
        dateChoices.add(enabledWhen(() -> unused.getValue().equals("false")));
        userFilterTextField.add(enabledWhen(() -> unused.getValue().equals("false")));
        documentFilterTextField.add(enabledWhen(() -> unused.getValue().equals("false")));

        return searchForm;
    }

    public Form<Void> createSettingsForm()
    {
        settingsForm = new Form<>("settingsForm");
        settingsForm.setOutputMarkupId(true);

        // Init defaultDocumentsNumberTextField
        defaultNumberDocumentsTextField = new NumberTextField<>("defaultDocumentsNumberTextField",
                new Model<Integer>(), Integer.class);

        // Set value for input and additional features
        defaultNumberDocumentsTextField
                .setDefaultModel(new CompoundPropertyModel<>(dynamicWorkflowManagementService
                        .getOrCreateWorkflowEntry(currentProject.getObject())
                        .getDefaultAnnotations()));
        defaultNumberDocumentsTextField.setMinimum(1);
        defaultNumberDocumentsTextField.setRequired(true);
        defaultNumberDocumentsTextField.setConvertEmptyInputStringToNull(false);

        // Finally, add the confirm button at the end
        Button save = new LambdaAjaxButton("save", this::actionConfirm);

        settingsForm.add(defaultNumberDocumentsTextField);
        settingsForm.add(save);

        workflowChoices = new BootstrapSelect("workloadStrategy",
                LoadableDetachableModel.of(this::getWorkflowManager));
        workflowChoices
                .setChoiceRenderer(new LambdaChoiceRenderer<>(WorkflowManagerType::getUiName));
        workflowChoices.setRequired(true);
        workflowChoices.setNullValid(false);
        workflowChoices.setChoices(workflowManagerExtensionPoint.getTypes());

        // add them to the form
        settingsForm.add(workflowChoices);

        return settingsForm;
    }

    public Form<Void> createUserForm()
    {
        userForm = new Form("userForm");
        userForm.setOutputMarkupId(true);

        userForm.add(createUserSelectionForm());
        userForm.add(createUserDetailForm());

        return userForm;
    }

    private Form<Void> createUserSelectionForm()
    {
        userSelectionForm = new Form<>("userSelectionForm");
        userSelectionForm.setOutputMarkupId(true);

        // Show all available users
        userSelection = new BootstrapSelect("userSelection", new Model<>(new ArrayList<String>()));
        userSelection.setMarkupId("Select a user");
        userSelection.setChoices(Model
                .ofList(projectService.listProjectUsersWithPermissions(currentProject.getObject())
                        .stream().map(User::getUsername).sorted().collect(Collectors.toList())));

        userSelection.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {
            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                System.out.println("-------------------");
                System.out.println("CLICKED");
                selectedUser = userRepository.get(userSelection.getDefaultModelObjectAsString());
                resetDocument
                        .setChoices(
                                Model.ofList(documentService
                                        .listAnnotationDocuments(currentProject.getObject(),
                                                userRepository.get(userSelection
                                                        .getDefaultModelObjectAsString()))
                                        .stream().map(AnnotationDocument::getName).sorted()
                                        .collect(Collectors.toList())));

                assignDocument.setChoices(Model.ofList(documentService
                        .listAnnotationDocuments(currentProject.getObject(), selectedUser).stream()
                        .map(AnnotationDocument::getName).sorted().collect(Collectors.toList())));

                aTarget.add(userDetailForm);
                aTarget.add(userForm);
            }

        });

        userSelectionForm.add(userSelection);
        return userSelectionForm;
    }

    private WorkflowManagerType getWorkflowManager()
    {
        DynamicWorkflowManager manager = dynamicWorkflowManagementService
                .getOrCreateWorkflowEntry(currentProject.getObject());
        WorkloadManagerExtension extension = workflowManagerExtensionPoint
                .getExtension(manager.getWorkflow());
        return new WorkflowManagerType(extension.getId(), extension.getLabel());
    }

    private Form<Void> createUserDetailForm()
    {
        userDetailForm = new Form<>("userDetailForm");
        userDetailForm.setOutputMarkupId(true);

        // Show ALL documents in the project (even those for which the user
        // does not have an annotations document created yet (due to lazy creation)
        assignDocument = new BootstrapSelect("assignDocument",
                new Model<>(new ArrayList<String>()));
        assignDocument.setMarkupId("Select a document");
        userDetailForm.add(assignDocument);

        Button assign = new LambdaAjaxButton<>("assign", this::actionAssignDocument);
        userDetailForm.add(assign);

        // Shows all annotation documents for the user that exist in the DB
        resetDocument = new BootstrapSelect("resetDocument", new Model<>(new ArrayList<String>()));
        resetDocument.setMarkupId("Select the new State");
        userDetailForm.add(resetDocument);

        documentState = new BootstrapSelect("documentState", new Model<>(new ArrayList<String>()));
        documentState.setMarkupId("Select the new State");
        List<String> documentstates = new ArrayList<>();
        documentstates.add(AnnotationDocumentState.IN_PROGRESS.getName());
        documentstates.add(AnnotationDocumentState.FINISHED.getName());
        documentState.setChoices(documentstates);
        userDetailForm.add(documentState);

        Button set = new LambdaAjaxButton<>("set", this::actionSetDocumentStatus);
        userDetailForm.add(set);

        return userDetailForm;
    }

    private void actionSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        aTarget.add(table);
    }

    private void actionReset(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        dateFrom.setModelObject(null);
        dateTo.setModelObject(null);
        unused.setModelObject(false);
        userFilterTextField.setModelObject(null);
        documentFilterTextField.setModelObject(null);

        aTarget.add(searchForm);
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        dynamicWorkflowManagementService.setDefaultAnnotations(
                Integer.parseInt(defaultNumberDocumentsTextField.getInput()),
                currentProject.getObject());

        dynamicWorkflowManagementService.setWorkflow(workflowChoices.getModelObject().getUiName(),
                currentProject.getObject());

        success("Changes saved");
    }

    private void actionSetDocumentStatus(AjaxRequestTarget ajaxRequestTarget, Form<?> aForm)
    {

    }

    private void actionAssignDocument(AjaxRequestTarget ajaxRequestTarget, Form<?> aForm)
    {

    }
}
