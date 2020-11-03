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
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import de.tudarmstadt.ukp.inception.workload.dynamic.support.DateSelection;
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
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.utils.RequestCycleUtils;
import com.googlecode.wicket.kendo.ui.KendoDataSource;
import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDatePicker;
import com.googlecode.wicket.kendo.ui.form.multiselect.lazy.MultiSelect;
import com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
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
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadTrait;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueOverviewDataProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.WorkloadMetadataDialog;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.workflow.WorkflowExtensionPoint;

@MountPath("/workload.html")
public class DynamicWorkloadManagementPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1180618893870240262L;

    private Form<Void> searchForm;
    private Form<Collection<SourceDocument>> userAssignDocumentForm;
    private Form<AnnotationDocument> userResetDocumentForm;

    private WorkflowExtensionPoint workflowExtensionPoint;

    private final IModel<Project> currentProject = new Model<>();

    private NumberTextField<Integer> defaultNumberDocumentsTextField;

    private DataTable<SourceDocument, String> table;
    private ModalWindow infoDialog;

    private AjaxDatePicker dateFrom;
    private AjaxDatePicker dateTo;
    private AjaxCheckBox unused;

    private TextField<String> userFilterTextField;
    private TextField<String> documentFilterTextField;

    private BootstrapRadioChoice<DateSelection> dateChoices;

    private BootstrapSelect<String> workflowChoices;
    private BootstrapSelect<User> userSelection;
    private BootstrapSelect<AnnotationDocument> resetDocument;
    private BootstrapSelect<AnnotationDocumentState> documentState;

    private DateSelection dateSelection;

    // SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean DynamicWorkloadExtension dynamicWorkloadExtension;

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
        headers.add(getString("Annotators"));
        headers.add(getString("Updated"));
        headers.add(getString("actions"));

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
                dataProvider::getFinishedAmountForDocument));
        columns.add(new LambdaColumn<>(new ResourceModel("Processed"), getString("Processed"),
                dataProvider::getInProgressAmountForDocument));
        columns.add(new LambdaColumn<>(new ResourceModel("Annotators"), getString("Annotators"),
                dataProvider::getUsersWorkingOnTheDocument));
        columns.add(new LambdaColumn<>(new ResourceModel("Updated"),
                dataProvider::lastAccessTimeForDocument));

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
            }
        });

        table = new DataTable<>("dataTable", columns, dataProvider, 20);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, dataProvider));

        add(table);

        add(createSearchForm(dataProvider));
        add(createUserForm());
        add(createSettingsForm());
    }

    private void actionShowInfoDialog(AjaxRequestTarget aTarget, IModel<SourceDocument> aDoc)
    {
        // Get the current selected Row
        SourceDocument doc = aDoc.getObject();

        // Create the modalWindow
        infoDialog.setTitle("Metadata of document: " + doc.getName());

        // Set contents of the modalWindow
        infoDialog.setContent(new WorkloadMetadataDialog(infoDialog.getContentId(), doc,
                workloadManagementService.getUsersForSpecificDocumentAndState(
                        AnnotationDocumentState.FINISHED, doc, currentProject.getObject()),
                workloadManagementService.getUsersForSpecificDocumentAndState(
                        AnnotationDocumentState.IN_PROGRESS, doc, currentProject.getObject())));

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
        List<DateSelection> dateChoice = Arrays.asList(dateSelection.getClass().getEnumConstants());
        // Create the radio button group
        dateChoices = new BootstrapRadioChoice<>
            ("date", new Model<>(DateSelection.between), dateChoice);
        dateChoices.setInline(true);
        dateChoices.setOutputMarkupId(true);

        // Update Behaviour on click, disable according date inputs and reset their values
        dateChoices.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                dateTo.setModelObject(null);
                dateFrom.setModelObject(null);

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
                if (getModelObject()) {
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
        Button reset = new LambdaAjaxButton<>("reset", this::actionReset);

        searchForm.add(reset);

        // Submit button
        Button search = new LambdaAjaxButton<>("search", this::actionSubmit);

        searchForm.add(search);

        // Condition for filter inputs to be enabled
        dateTo.add(enabledWhen(
            () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(0).name())
                && !unused.getModelObject()));
        dateFrom.add(enabledWhen(
            () -> !dateChoices.getDefaultModelObjectAsString().equals(dateChoice.get(1).name())
                && !unused.getModelObject()));
        dateChoices.add(enabledWhen(() -> !unused.getModelObject()));
        userFilterTextField.add(enabledWhen(() -> !unused.getModelObject()));
        documentFilterTextField.add(enabledWhen(() -> !unused.getModelObject()));

        return searchForm;
    }

    public Form<Void> createSettingsForm()
    {
        Form<Void> settingsForm = new Form<>("settingsForm");
        settingsForm.setOutputMarkupId(true);

        // Init defaultDocumentsNumberTextField
        defaultNumberDocumentsTextField = new NumberTextField<>("defaultDocumentsNumberTextField",
                new Model<>(), Integer.class);

        DynamicWorkloadTrait traits = dynamicWorkloadExtension.readTraits(workloadManagementService
                .getOrCreateWorkloadManagerConfiguration(currentProject.getObject()));

        defaultNumberDocumentsTextField.setDefaultModel(
                new CompoundPropertyModel<>((traits.getDefaultNumberOfAnnotations())));
        defaultNumberDocumentsTextField.setMinimum(1);
        defaultNumberDocumentsTextField.setRequired(true);
        defaultNumberDocumentsTextField.setConvertEmptyInputStringToNull(false);

        // Finally, add the confirm button at the end
        Button save = new LambdaAjaxButton<>("save", this::actionConfirm);

        settingsForm.add(defaultNumberDocumentsTextField);
        settingsForm.add(save);

        workflowChoices = new BootstrapSelect<>("workloadStrategy");
        workflowChoices.setDefaultModel(Model.of(traits.getType()));
        List<String> choices = workflowExtensionPoint.getExtensions().stream()
            .map(WorkflowExtension::getLabel)
            .collect(Collectors.toList());
        workflowChoices.setChoices(choices);
        workflowChoices.setRequired(true);
        workflowChoices.setNullValid(false);

        // add them to the form
        settingsForm.add(workflowChoices);

        return settingsForm;
    }

    public Form<Void> createUserForm()
    {
        Form<Void> userForm = new Form<>("userForm");
        userForm.setOutputMarkupId(true);

        userForm.add(createUserSelectionForm());
        userForm.add(createUserAssignDocumentForm());
        userForm.add(createUserResetForm());

        return userForm;
    }

    private Form<User> createUserSelectionForm()
    {
        Form<User> userSelectionForm = new Form<>("userSelectionForm");
        userSelectionForm.setOutputMarkupId(true);

        // Show all available users

        userSelection = new BootstrapSelect<>("userSelection");
        userSelection.setChoiceRenderer(new LambdaChoiceRenderer<>(User::getUsername));
        userSelection.setModel(new Model<>());
        userSelection.setNullValid(true);
        userSelection.setChoices(this::getUsersForCurrentProject);

        userSelection.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(userSelection);
                aTarget.add(userAssignDocumentForm);
                aTarget.add(userResetDocumentForm);
            }
        });

        userSelection.setOutputMarkupId(true);
        userSelectionForm.add(userSelection);

        return userSelectionForm;
    }

    private Form<Collection<SourceDocument>> createUserAssignDocumentForm()
    {
        // Show ALL documents in the project (even those for which the user
        // does not have an annotations document created yet
        IModel<Collection<SourceDocument>> documentsToAddModel = new CollectionModel<>(
                new ArrayList<>());
        userAssignDocumentForm = new Form<>("userAssignDocumentForm", documentsToAddModel);
        // This ensures that we get the user input in getChoices
        MultiSelect<SourceDocument> documentsToAdd = new MultiSelect<SourceDocument>(
                "documentsToAdd", new ChoiceRenderer<>("name"))
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
                    }
                }
                return result;
            }
        };

        documentsToAdd.setModel(documentsToAddModel);
        documentsToAdd.setOutputMarkupId(true);

        userAssignDocumentForm.setOutputMarkupPlaceholderTag(true);
        userAssignDocumentForm.add(visibleWhen(() -> !isNull(userSelection.getModelObject())));
        userAssignDocumentForm.add(documentsToAdd);

        userAssignDocumentForm.add(new LambdaAjaxButton<>("assign", this::actionAssignDocument));
        return userAssignDocumentForm;
    }

    private Form<AnnotationDocument> createUserResetForm()
    {
        userResetDocumentForm = new Form<>("userResetDocumentForm");
        // Shows all annotation documents for the user that exist in the DB
        resetDocument = new BootstrapSelect<>("resetDocument");
        resetDocument.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationDocument::getName));
        resetDocument.setChoices(this::getCreatedDocumentsForSelectedUser);
        resetDocument.setModel(LoadableDetachableModel.of(this::getSelectedAnnotationDocument));
        resetDocument.setOutputMarkupId(true);
        userResetDocumentForm.add(resetDocument);

        documentState = new BootstrapSelect<>("documentState");
        documentState
                .setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationDocumentState::getName));
        documentState.setChoices(this::getAnnotationDocumentStates);

        documentState
                .setModel(LoadableDetachableModel.of(this::getSelectedAnnotationDocumentState));
        documentState.setOutputMarkupId(true);
        userResetDocumentForm.add(documentState);

        userResetDocumentForm.setOutputMarkupPlaceholderTag(true);
        userResetDocumentForm.add(visibleWhen(() -> !isNull(userSelection.getModelObject())));

        userResetDocumentForm.add(new LambdaAjaxButton<>("set", this::actionSetDocumentStatus));

        return userResetDocumentForm;

    }

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
            return new ArrayList<>(documentService.listAnnotationDocuments(
                    currentProject.getObject(), userSelection.getModelObject()));
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

        dynamicWorkloadExtension.writeTraits(workloadManagementService,
            new DynamicWorkloadTrait(workflowChoices.getModelValue(),
                Integer.parseInt(defaultNumberDocumentsTextField.getInput())),
            currentProject.getObject());

        if (workflowChoices.getDefaultModelObjectAsString().equals(getString("randomized")))

        success("Changes saved");
    }

    private void actionSetDocumentStatus(AjaxRequestTarget aAjaxRequestTarget,
            Form<AnnotationDocumentState> aForm)
    {
        aAjaxRequestTarget.addChildren(getPage(), IFeedback.class);
        if (resetDocument.getModelObject() == null) {
            error("Please select a document you wish to change its status first!");
        }
        else if (documentState.getModelObject() == null) {
            error("Please select a state you wish to assign to the document");
        }
        else {
            if (documentState.getModelObject().equals(AnnotationDocumentState.IN_PROGRESS)) {
                documentService.transitionAnnotationDocumentState(
                        documentService.getAnnotationDocument(
                                documentService.getSourceDocument(
                                        currentProject.getObject(),
                                        resetDocument.getModelObject().getName()),
                                userSelection.getModelObject()),
                        AnnotationDocumentStateTransition.
                            ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS);
            }
            else {
                documentService.transitionAnnotationDocumentState(
                        documentService.getAnnotationDocument(
                                documentService.getSourceDocument(
                                        currentProject.getObject(),
                                        resetDocument.getModelObject().getName()),
                                userSelection.getModelObject()),
                        AnnotationDocumentStateTransition.
                            ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);
            }

            success("Document status changed");
        }
    }

    private void actionAssignDocument(AjaxRequestTarget aAjaxRequestTarget,
            Form<List<SourceDocument>> aForm)
    {
        aAjaxRequestTarget.addChildren(getPage(), IFeedback.class);

        if (aForm.getModelObject() == null) {
            error("Please add documents you want to assign.");
        }
        else {
            for (SourceDocument sourceDocument : aForm.getModelObject()) {
                try {
                    AnnotationDocument annotationDocument = documentService
                            .getAnnotationDocument(sourceDocument, userSelection.getModelObject());
                    if (annotationDocument.getState().equals(AnnotationDocumentState.NEW)) {
                        documentService.transitionAnnotationDocumentState(annotationDocument,
                                AnnotationDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS);
                    }
                    else {
                        error("Document '" + sourceDocument.getName()
                                + "' is either already assigned or even finished.");
                    }

                }
                catch (NoResultException nre) {
                    AnnotationDocumentState state = AnnotationDocumentState.IN_PROGRESS;
                    AnnotationDocument annotationDocument = new AnnotationDocument();
                    annotationDocument.setName(sourceDocument.getName());
                    annotationDocument.setDocument(documentService.getSourceDocument(
                            currentProject.getObject(), annotationDocument.getName()));
                    annotationDocument.setProject(currentProject.getObject());
                    annotationDocument.setUser(userSelection.getModelObject().getUsername());
                    annotationDocument.setState(state);
                    documentService.createAnnotationDocument(annotationDocument);

                    success("Document(s) assigned");
                }
            }
        }
        aAjaxRequestTarget.add(this);
    }
}
