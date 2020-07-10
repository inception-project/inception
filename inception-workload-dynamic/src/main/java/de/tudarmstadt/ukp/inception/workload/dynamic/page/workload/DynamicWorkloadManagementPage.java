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
package de.tudarmstadt.ukp.inception.workload.dynamic.page.workload;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeaderlessColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDatePicker;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.DefaultAnnotationsProperties;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadProperties;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.TableContentProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.WorkloadMetadataDialog;



@MountPath("/workload.html")
public class DynamicWorkloadManagementPage extends ApplicationPageBase
{
    private static final long serialVersionUID = 1180618893870240262L;

    private IModel<Project> currentProject = new Model<>();

    private DataTable<SourceDocument, String> table;
    private ModalWindow infoDialog;
    
    // SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;
    private @SpringBean WorkloadProperties workloadProperties;
    private @SpringBean DefaultAnnotationsProperties defaultAnnotations;

    //Default constructor, no project selected (only when workload.html
    // put directly in the browser without any parameters)
    public DynamicWorkloadManagementPage() {
        super();
        //Error, user is returned to home page, nothing else to do
        error("No Project selected, please enter the monitoring page only with a valid project reference");
        setResponsePage(getApplication().getHomePage());
    }

    //Constructor with a project
    public DynamicWorkloadManagementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        //Get current Project
        currentProject.setObject(getProjectFromParameters(aPageParameters.get
            (PAGE_PARAM_PROJECT_ID)).get());

        //Header of the page
        Label name = new Label("name", currentProject.getObject().getName());
        add(name);

        //Headers of the table
        List<String> headers = new ArrayList<>();
        headers.add(getString("Document"));
        headers.add(getString("Finished"));
        headers.add(getString("Processed"));
        headers.add(getString("Users"));
        headers.add(getString("Updated"));
        headers.add(getString("actions"));

        //13/05/2020 15:16:57 // Christoph

        //Data Provider for the table
        TableContentProvider dataProvider = new TableContentProvider(documentService.
            listSourceDocuments(currentProject.getObject()),
            headers, documentService.listAnnotationDocuments
            (currentProject.getObject()));

        //Init defaultDocumentsNumberTextField
        NumberTextField<Integer> defaultNumberDocumentsTextField =
            new NumberTextField<>("defaultDocumentsNumberTextField",
                new Model<Integer>(), Integer.class);

        //Set minimum value for input
        defaultNumberDocumentsTextField.setDefaultModel(new CompoundPropertyModel<>(6));
        defaultNumberDocumentsTextField.setMinimum(1);
        defaultNumberDocumentsTextField.setRequired(true);
        defaultNumberDocumentsTextField.setConvertEmptyInputStringToNull(false);
        add(defaultNumberDocumentsTextField);

        infoDialog = new ModalWindow("infoDialog");
        add(infoDialog);

        //Columns of the table
        //Each column creates TableMetaData
        List<IColumn<SourceDocument, String>> columns = new ArrayList<>();
        columns.add(new LambdaColumn<>(new ResourceModel(
            "Document"), getString("Document"),
            SourceDocument::getName));
        columns.add(new LambdaColumn<>(new ResourceModel(
            "Finished"), getString("Finished"),
            _doc -> dataProvider
                .getFinishedAmountForDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel(
            "Processed"), getString("Processed"),
            _doc -> dataProvider
                .getInProgressAmountForDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel(
            "Users"), getString("Users"),
            _doc -> dataProvider
                .getUsersWorkingOnTheDocument((SourceDocument) _doc)));
        columns.add(new LambdaColumn<>(new ResourceModel("Updated"), _doc -> 
                dataProvider.lastAccessTimeForDocument((SourceDocument) _doc)));

        //Own column type, contains only a clickable image (AJAX event),
        //creates a small panel dialog containing metadata
        columns.add(new HeaderlessColumn<SourceDocument, String>()
        {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void populateItem(
                    Item<ICellPopulator<SourceDocument>> aItem,
                    String componentId, IModel<SourceDocument> rowModel)
            {
                Fragment fragment = new Fragment(componentId, "infoColumn",
                        DynamicWorkloadManagementPage.this);
                fragment.add(new LambdaAjaxLink("showInfoDialog", _target -> 
                        actionShowInfoDialog(_target, rowModel)));
                aItem.add(fragment);
            };
        });

        table = new DataTable<>("dataTable", columns, dataProvider, 20);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar<>(table, dataProvider));

        add(table);

        add(createSearchForm(dataProvider));
        
    }
    
    private void actionShowInfoDialog(AjaxRequestTarget aTarget, IModel<SourceDocument> aDoc)
    {
        //Get the current selected Row
        SourceDocument doc = aDoc.getObject();

        //Create the modalWindow
        infoDialog.setTitle("Metadata of document: " + doc.getName());

        //Set contents of the modalWindow
        infoDialog.setContent(new WorkloadMetadataDialog(infoDialog.
            getContentId(), doc, listUsersFinishedForDocument(doc),
            listUsersInProgressForDocument(doc)));

        //Open the dialog
        infoDialog.show(aTarget);
    }

    //--------------------------------------- Helper methods -------------------------------------//

    //Return current project, required for several purposes
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

    //
    public List<String> listUsersFinishedForDocument(SourceDocument aDocument)
    {
        List<String> result = new ArrayList<>();
        for (AnnotationDocument anno: documentService.
            listAnnotationDocuments(currentProject.getObject()))
        {
            if (anno.getState().equals(AnnotationDocumentState.FINISHED)
                && anno.getName().equals(aDocument.getName()))
            {
                result.add(anno.getUser());
            }
        }
        return result;
    }

    //
    public List<String> listUsersInProgressForDocument(SourceDocument aDocument)
    {
        List<String> result = new ArrayList<>();
        for (AnnotationDocument anno: documentService.
            listAnnotationDocuments(currentProject.getObject()))
        {
            if (anno.getState().equals(AnnotationDocumentState.IN_PROGRESS)
                && anno.getName().equals(aDocument.getName()))
            {
                result.add(anno.getUser());
            }
        }
        return result;
    }

    public Form<Void> createSearchForm(TableContentProvider aProv)
    {
        Form<Void> searchForm = new Form<>("searchForm");
        
        searchForm.setOutputMarkupId(true);

        //Filter Textfields and their AJAX events
        TextField<String> userFilterTextField = new TextField<>("userFilter",
            PropertyModel.of(aProv, "filter.username"), String.class);

        userFilterTextField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        TextField<String> documentFilterTextField = new TextField<>("documentFilter",
            PropertyModel.of(aProv, "filter.documentName"), String.class);

        documentFilterTextField.setOutputMarkupId(true);

        searchForm.add(userFilterTextField);
        searchForm.add(documentFilterTextField);

        //Input dates
        AjaxDatePicker dateFrom = new AjaxDatePicker("from", PropertyModel.of(aProv, "filter.from"),
                "MM/dd/yyyy");
        AjaxDatePicker dateTo = new AjaxDatePicker("to", PropertyModel.of(aProv, "filter.to"),
                "MM/dd/yyyy");

        dateFrom.setOutputMarkupId(true);
        dateTo.setOutputMarkupId(true);

        searchForm.add(dateFrom);
        searchForm.add(dateTo);

        // Date choices
        // FIXME: This should use an Enum value as model - and I18N should be used to map the 
        // enum's values to the display values
        List<String> dateChoice = new ArrayList<>();
        dateChoice.add(getString("from"));
        dateChoice.add(getString("until"));
        dateChoice.add(getString("between"));

        //Create the radio button group
        BootstrapRadioChoice<String> dateChoices = new BootstrapRadioChoice<>("date",
                new Model<>(getString("between")), dateChoice);
        dateChoices.setInline(true);
        dateChoices.setOutputMarkupId(true);

        //Update Behaviour on click, disable according date inputs and reset their values
        dateChoices.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                if (getComponent().getDefaultModelObjectAsString().equals("from")) {
                    dateTo.setModelObject(null);
                    dateTo.setEnabled(false);
                    dateFrom.setEnabled(true);
                }
                else if (getComponent().getDefaultModelObjectAsString().equals("until")) {
                    dateFrom.setModelObject(null);
                    dateFrom.setEnabled(false);
                    dateTo.setEnabled(true);
                }
                else {
                    dateTo.setEnabled(true);
                    dateFrom.setEnabled(true);
                }
                ajaxRequestTarget.add(dateFrom);
                ajaxRequestTarget.add(dateTo);
            }
        });

        //add them to the form
        searchForm.add(dateChoices);

        //Submit button
        Button submit = new AjaxButton(getString("Search"), Model.of("Search")) {
            private static final long serialVersionUID = 3521172967850377971L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                target.add(table);
            }
        };

        searchForm.add(submit);

        //Checkbox for showing only unused source documents, disables other textfields
        AjaxCheckBox unused = new AjaxCheckBox("unused",
            PropertyModel.of(aProv,"filter.selected")) {

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {

                if (getDefaultModelObjectAsString().equals("false"))
                {
                    userFilterTextField.setEnabled(true);
                    documentFilterTextField.setEnabled(true);
                    dateFrom.setEnabled(true);
                    dateTo.setEnabled(true);
                    dateChoices.setEnabled(true);
                } else {
                    userFilterTextField.setModelObject(null);
                    userFilterTextField.setEnabled(false);
                    dateFrom.setModelObject(null);
                    dateFrom.setEnabled(false);
                    dateTo.setModelObject(null);
                    dateTo.setEnabled(false);
                    dateChoices.setEnabled(false);
                }
                ajaxRequestTarget.add(dateFrom);
                ajaxRequestTarget.add(dateTo);
                ajaxRequestTarget.add(dateChoices);
                ajaxRequestTarget.add(userFilterTextField);
                ajaxRequestTarget.add(documentFilterTextField);
            }
        };

        unused.setOutputMarkupId(true);
        searchForm.add(unused);

        //Reset button
        Button reset = new AjaxButton(getString("Reset"), Model.of("Reset")) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {

                dateFrom.setEnabled(true);
                dateFrom.setModelObject(null);
                dateTo.setEnabled(true);
                dateTo.setModelObject(null);
                dateChoices.setEnabled(true);
                unused.setModelObject(null);
                userFilterTextField.setEnabled(true);
                documentFilterTextField.setEnabled(true);
                userFilterTextField.setModelObject(null);
                documentFilterTextField.setModelObject(null);



                target.add(userFilterTextField);
                target.add(documentFilterTextField);
                target.add(dateFrom);
                target.add(dateTo);
                target.add(unused);
                target.add(dateChoices);
            }
        };

        searchForm.add(reset);

        return searchForm;
    }
}
