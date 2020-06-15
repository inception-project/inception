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


package de.tudarmstadt.ukp.inception.workload.dynamic.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.DataProvider;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.Filter;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.ImagePanel;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.ModalPanel;


@MountPath("/workload.html")
public class MonitoringPage extends ApplicationPageBase
{

    private static final long serialVersionUID = 1180618893870240262L;

    //Top Form
    MonitoringForm workload;

    //Dataprovider for table
    private DataProvider dataProvider;

    //Layout Panel
    private DataTable table;

    //Modal Window (popup panel on clicking the image in the metadata column)
    private ModalWindow modalWindow;

    //Default annotations textbox
    private NumberTextField<Integer> DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD;

    //FilterTextField and input
    private TextField<String> filterTextfield;
    private DropDownChoice<String> filterDropDown;

    //The Icons
    private static final ResourceReference META =
        new PackageResourceReference(MonitoringPage.class, "book_open.png");

    //Current Project
    private IModel<Project> currentProject = new Model<>();

    //List containing all documents
    private IModel<List<SourceDocument>> documentList = new Model();


    //Default annotations
    public IModel<Integer> defaultAnnotations = new Model<>();


    //SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;


    //Default constructor, no project selected (only when workload.html
    // put directly in the browser without any parameters)
    public MonitoringPage() {
        super();

        //top form initialize
        workload = new MonitoringForm("workload");
        workload.setOutputMarkupId(true);

        //Required due to wicket
        add(new ModalWindow("modalWindow"));
        add(new NumberTextField("defaultDocumentsNumberTextField"));
        add(new EmptyPanel("dataTable"));

        //Error, user is returned to home page, nothing else to do
        error("No Project selected, please enter the monitoring page only with a valid project reference");
        setResponsePage(getApplication().getHomePage());

    }



    //Constructor with a project
    public MonitoringPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        //top form initialize
        workload = new MonitoringForm("workload");
        workload.setOutputMarkupId(true);

        //Get current Project
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        if (getProjectFromParameters(projectParameter).isPresent())
        {
            currentProject.setObject(getProjectFromParameters(projectParameter).get());
        } else {
            currentProject = null;
        }


        //Get the current user
        User user = userRepository.getCurrentUser();
        //Check if Project exists
        if (currentProject != null)
        {




            //Check if user is allowed to see the monitoring page of this project
            if (currentProject != null &&
                !(projectService.isCurator(currentProject.getObject(), user)
                || projectService.isManager(currentProject.getObject(), user)))
            {
                //Required even in case of error due to wicket
                add(new ModalWindow("modalWindow"));
                add(new NumberTextField("defaultDocumentsNumberTextField"));
                add(new EmptyPanel("dataTable"));
                error("You have no permission to access project [" + currentProject.getObject().getId() + "]");
                setResponsePage(getApplication().getHomePage());

            } else {
                initialize();
            }

        } else {
            //Required even in case of error due to wicket
            add(new ModalWindow("modalWindow"));
            add(new NumberTextField("defaultDocumentsNumberTextField"));
            add(new EmptyPanel("dataTable"));
            //Project does not exists, returning to homepage
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());

        }

    }

    public void initialize()
    {

        //Header of the page
        Label name = new Label("name",currentProject.getObject().getName());
        add(name);

        //get Documents for current project
        documentList.setObject(documentService.
            listSourceDocuments(currentProject.getObject()));

        //Headers of the table
        List<String> headers = new ArrayList<>();
        headers.add(getString("Document"));
        headers.add(getString("Finished"));
        headers.add(getString("Processed"));
        headers.add(getString("Metadata"));

        //Data Provider for the table
        dataProvider = new DataProvider(documentList.getObject(),
            headers, documentService.
            listAnnotationDocuments(currentProject.getObject()));

        //Init defaultDocumentsNumberTextField
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD = new NumberTextField
        ("defaultDocumentsNumberTextField", Integer.class);

        //Set minimum value for input
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.setMinimum(1);
        //After upstream change
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.setRequired(true);




        //Get value for the project and set it accordingly
        //TODO get correct value after upstream change
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.setDefaultModel(new CompoundPropertyModel<Integer>(6));
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.setConvertEmptyInputStringToNull(false);

        //add AJAX event handler on changing input value
        DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = 2607214157084529408L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget)
            {
                defaultAnnotations.setObject(Integer.parseInt
                    (DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.getInput()));
            }
        });

        //Create the modalWindow
        modalWindow = new ModalWindow("modalWindow");
        modalWindow.setTitle("Metadata:");
        workload.add(modalWindow);

        //Columns of the table
        List<IColumn> columns = new ArrayList<>();

        //Filter properties
        FilterForm<Filter> filter = new FilterForm<>(getString("filter"), dataProvider);
        filter.setOutputMarkupId(true);

        //Filter Dropdown and Textfield
        filterDropDown = new DropDownChoice(getString("filterDropDown")
            , PropertyModel.of(dataProvider,"filter.type"), this::getFilter);

        filterTextfield = new TextField("filterTextfield",
            PropertyModel.of(dataProvider, "filter.input"), String.class);


        //Remove the "Choose one" default option
        filterDropDown.setNullValid(true);

        filter.add(filterDropDown);
        filter.add(filterTextfield);

        //Each column creates TableMetaData
        columns.add(new LambdaColumn<>(new ResourceModel(getString("Document"))
            , getString("Document"), SourceDocument::getName));
        columns.add(new LambdaColumn<>(new ResourceModel(getString("Finished"))
            , getString("Finished"), aDocument -> dataProvider.getFinishedAmountForDocument
            ((SourceDocument)aDocument)));
        columns.add(new LambdaColumn<>(new ResourceModel(getString("Processed"))
            , getString("Processed"), aDocument -> dataProvider.
            getInProgressAmountForDocument((SourceDocument)aDocument)));

        //Own column type, contains only a clickable image (AJAX event),
        //creates a small panel dialog containing metadata
        columns.add(new PropertyColumn<SourceDocument,String>(new Model(getString("Metadata")),
            getString("Metadata"))
        {
            private static final long serialVersionUID = 1L;
            @Override
            public void populateItem(Item aItem, String aID, IModel aModel)
            {

                //Add the Icon
                ImagePanel icon = new ImagePanel(aID, META);
                icon.add(new AttributeAppender("style", "cursor: pointer", ";"));
                icon.add(new AttributeModifier("align","center"));
                aItem.add(icon);
                aItem.add(AttributeModifier.append("class", "centering"));

                //Click event in the metadata column
                aItem.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = 7624208971279187266L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {

                        //Get the current selected Row
                        Item rowItem = aItem.findParent(Item.class);
                        int rowIndex = rowItem.getIndex();

                        SourceDocument doc = dataProvider.getShownDocuments().
                            get((int)(table.getCurrentPage() * table.getItemsPerPage()) + rowIndex);


                        //Set contents of the modalWindow
                        modalWindow.setContent(new ModalPanel(modalWindow.
                            getContentId(), doc, listUsersFinishedForDocument(doc),
                            listUsersInProgressForDocument(doc)));

                        //Open the dialog
                        modalWindow.show(aTarget);
                    }
                });
            }
        });

        //The DefaultDataTable
        table = new DataTable("dataTable", columns, dataProvider, 12);
        table.setOutputMarkupId(true);

        //FilterToolbar
        FilterToolbar filterToolbar = new FilterToolbar(table, filter);
        table.addTopToolbar(filterToolbar);
        table.addTopToolbar(new NavigationToolbar(table));
        table.addTopToolbar(new HeadersToolbar(table, dataProvider));

        //Add the table to the filter form
        filter.add(table);

        //Checkbox for showing only unused source documents
        AjaxCheckBox unused = new AjaxCheckBox("unused",
            PropertyModel.of(dataProvider,"filter.selected")) {

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                ajaxRequestTarget.add(table);
            }
        };

        unused.setOutputMarkupId(true);

        filter.add(unused);

        //Submit button
        Button submit = new Button(getString("Search"), Model.of("Search"));
        submit.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                ajaxRequestTarget.add(table);
            }
        });

        filter.add(submit);



        //Add to page according to the following structure

        //Then default annotations texfield
        workload.add(DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD);

        //Filter components with the table
        workload.add(filter);

        add(workload);

    }


    //--------------------------------------- Helper methods -------------------------------------//


    //Return current project, required for several purposes
    private Optional<Project> getProjectFromParameters(StringValue projectParam)
    {
        if (projectParam == null || projectParam.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(projectService.getProject(projectParam.toLong()));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }

    //Helper methods, Additional filters
    public List<String> getFilter()
    {
        List<String> filterList = new ArrayList<>();
        filterList.add("Document creation time:");
        filterList.add("User:");

        return filterList;
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

}
