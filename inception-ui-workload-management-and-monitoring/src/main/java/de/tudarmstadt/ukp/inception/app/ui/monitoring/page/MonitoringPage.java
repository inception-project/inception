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


package de.tudarmstadt.ukp.inception.app.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.persistence.NoResultException;


import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapFeedbackPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.DataProvider;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.DocumentSupport;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.ImagePanel;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.ModalPanel;





@MountPath("/workload.html")
public class MonitoringPage extends ApplicationPageBase
{

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringPage.class);
    private static final long serialVersionUID = 1180618893870240262L;


    //Feedbackpanel
    BootstrapFeedbackPanel feedbackPanel;

    //Layout Panel
    private Panel table;

    //Modal Window (popup panel on clicking the image in the metadata column)
    private ModalWindow modalWindow;

    //Default annotations textbox
    private NumberTextField DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD;

    //FilterTextField and input
    private TextField filterTextfield;
    private String filterTextFieldInput;
    private DropDownChoice filterDropDown;

    //The Icons
    private static final ResourceReference META =
        new PackageResourceReference(MonitoringPage.class, "book_open.png");

    //Current Project
    private Project currentProject;

    //List containing all documents
    private List<SourceDocument> documentList;

    //List containing all documents finished in their
    // progress of annotation by any user in the project
    private List<String> annotatedDocuments;


    //Default annotations
    public int defaultAnnotations;


    //SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;
    private DocumentSupport documentSupport;

    //Default constructor, no project selected (only when workload.html
    // put directly in the browser without any parameters)
    public MonitoringPage() {
        super();


        feedbackPanel = new BootstrapFeedbackPanel("feedbackPanel");
        feedbackPanel.setOutputMarkupId(true);

        //Required due to wicket
        add(new ModalWindow("modalWindow"));
        add(new NumberTextField("defaultDocumentsNumberTextField"));
        add(new EmptyPanel("dataTable"));

        //Error, user is returned to home page, nothing else to do
        //TODO show errors properly
        feedbackPanel.error("No Project selected, please enter the monitoring page only with a valid project reference");
        setResponsePage(getApplication().getHomePage());

    }


    //Constructor with a project
    public MonitoringPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        //Get current Project
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        if (getProjectFromParameters(projectParameter).isPresent())
        {
            currentProject = getProjectFromParameters(projectParameter).get();
        } else {
            currentProject = null;
        }


        //Get the current user
        User user = userRepository.getCurrentUser();
        //Check if Project exists
        if (currentProject != null)
        {

            feedbackPanel = new BootstrapFeedbackPanel("feedbackPanel");
            feedbackPanel.setOutputMarkupId(true);


            //Check if user is allowed to see the monitoring page of this project
            if (currentProject != null && !(projectService.isCurator(currentProject, user)
                || projectService.isManager(currentProject, user)))
            {
                //Required even in case of error due to wicket
                add(new ModalWindow("modalWindow"));
                add(new NumberTextField("defaultDocumentsNumberTextField"));
                add(new EmptyPanel("dataTable"));
                feedbackPanel.error("You have no permission to access project [" + currentProject.getId() + "]");
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
        //get Documents for current project
        documentList = documentService.
            listSourceDocuments(currentProject);

        //Get access to advanced methods for documents
        documentSupport = new DocumentSupport(currentProject, documentList);


        //Initialize list for all Finished documents
        annotatedDocuments = getAnnotatedDocuments();


        //Headers of the table
        List<String> headers = new ArrayList<>();
        headers.add(getString("Document"));
        headers.add(getString("Finished"));
        headers.add(getString("InProgress"));
        headers.add(getString("Metadata"));

        //Data Provider for the table
        DataProvider dataProvider = new DataProvider(documentList,
            headers, currentProject, annotatedDocuments);

        //Filter Dropdown and Textfield
        filterDropDown = new DropDownChoice(getString("filterDropDown")
            , PropertyModel.of(dataProvider,"filterState.type"), this::getFilter);
        filterTextfield = new TextField("filterTextfield",
            PropertyModel.of(dataProvider, "filterState.input"), String.class);


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
                defaultAnnotations = Integer.parseInt
                    (DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD.getInput());
            }
        });

        //Create the modalWindow
        modalWindow = new ModalWindow("modalWindow");
        modalWindow.setTitle("Metadata:");
        add(modalWindow);

        //Columns of the table
        List<IColumn> columns = new ArrayList<>();

        //Filter properties
        FilterForm<SourceDocument> filter = new FilterForm(getString("filter"),
            dataProvider);
        filter.add(filterDropDown);



        //Each column creates TableMetaData
        columns.add(new LambdaColumn<>(new ResourceModel(getString("Document"))
            , getString("Document"), SourceDocument::getName));
        columns.add(new LambdaColumn(new ResourceModel(getString("Finished"))
            , getString("Finished"), aDocument -> documentSupport.getFinishedAmountForDocument
            ((SourceDocument)aDocument, annotatedDocuments)));
        columns.add(new LambdaColumn<>(new ResourceModel(getString("InProgress"))
            , getString("InProgress"), aDocument -> documentSupport.getInProgressAmountForDocument
            ((SourceDocument)aDocument, annotatedDocuments)));

        //Own column type, contains only a clickable image (AJAX event),
        //creates a small panel dialog containing metadata
        columns.add(new PropertyColumn(new Model(getString("Metadata")),
            getString("Metadata"))
        {
            private static final long serialVersionUID = 1L;
            @Override
            public void populateItem(Item aItem, String aID, IModel aModel)
            {

                //Add the Icon
                ImagePanel icon = new ImagePanel(aID, META);
                icon.add(new AttributeAppender("style", "cursor: pointer", ";"));
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
                        Item rowItem = aItem.findParent( Item.class );
                        int rowIndex = rowItem.getIndex();

                        //Set contents of the modalWindow
                        modalWindow.setContent(new ModalPanel(modalWindow.
                            getContentId(), documentList.get(rowIndex)));

                        //Open the dialog
                        modalWindow.show(aTarget);
                    }
                });
            }
        });

        //The DefaultDataTable
        table = new DataTable("dataTable", columns, dataProvider, 20);
        table.setOutputMarkupId(true);


        //FilterToolbar
        FilterToolbar filterToolbar = new FilterToolbar((DataTable)table, filter);
        ((DataTable) table).addTopToolbar(filterToolbar);
        ((DataTable) table).addTopToolbar(new NavigationToolbar((DataTable)table));
        ((DataTable) table).addTopToolbar(new HeadersToolbar((DataTable)table, dataProvider));

        //Add the table to the filter form, as well as the input textfield
        filter.add(filterTextfield);
        filter.add(table);


        //Add to page according to the following structure

        //Then default annotations texfield
        add(DEFAULT_DOCUMENTS_NUMBER_TEXT_FIELD);


        //Filter components with the table
        add(filter);

    }

    //Return current project, required for several purposes,
    // might be put globally to be accessible for all packages
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
        filterList.add("None");
        filterList.add("Document creation time:");
        filterList.add("User:");
        filterList.add("Unused documents:");

        return filterList;
    }

    public List<String> getAnnotatedDocuments()
    {
        //List for annotated documents
        List<String> list = new ArrayList<>();

        for (AnnotationDocument doc: documentService.listFinishedAnnotationDocuments
            (currentProject))
        {
            list.add(doc.getName());
        }

        return list;
    }

    //Returns a random document out of all documents in the project.
    //Only a document is chosen which is not yet given to annotators more than the default number
    //per document number
    public SourceDocument getRandomDocument()
    {
        //Create an empty document
        SourceDocument document = null;
        Random r = new Random();

        while (document == null)
        {
            int i = r.nextInt(documentList.size() - 1);
            //If the random chosen document wont surpass the amount of default number combining
            // "inProgress" for the document + "finished" amount for the document + 1
            if ((documentSupport.getInProgressAmountForDocument(documentList.
                get(i), annotatedDocuments) +
                documentSupport.getFinishedAmountForDocument(documentList.
                    get(i), annotatedDocuments))
                + 1 <= defaultAnnotations)
            {
                //If that was not the case, assign this document
                document = documentList.get(r.nextInt(documentList.size() - 1));
            }
        }
        //Return the document
        //REMINDER: Document MIGHT BE NULL if there is not a single document left!
        // Annotator should then get the message: "No more documents to annotate"
        return document;
    }
}
