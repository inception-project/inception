/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.crowdsource;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EmbeddableImage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.TableDataProvider;

/**
 * Crowdsource page used to setup and monitor crowd source projects.
 *
 * @author Seid Muhie Yimam
 *
 */
public class CrowdSourcePage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(CrowdSourcePage.class);


    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private static final String CROWD_USER = "crowd_user";

    Project selectedProject;

    private class CrowdSourceForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -1L;

        public CrowdSourceForm(String id)
        {
            super(id);
            List<String> columnHeaders = new ArrayList<String>();
            columnHeaders.add("project");
            columnHeaders.add("document");
            columnHeaders.add("status");
            columnHeaders.add("edit");

            List<List<String>> rowData = new ArrayList<List<String>>();

            List<Project> allowedProject = new ArrayList<Project>();

            List<Project> allProjects = projectRepository.listProjects();

           User user = userRepository.get(CROWD_USER);
          for (Project project : allProjects) {
                List<User> users = projectRepository.listProjectUsersWithPermissions(project);
                if (users.contains(user)){
                    for (SourceDocument sourceDocument: projectRepository.listSourceDocuments(project)){
                        List<String> cellEntry = new ArrayList<String>();

                        cellEntry.add(project.getName());
                        cellEntry.add(sourceDocument.getName());
                        cellEntry.add(sourceDocument.getState().getName());
                        cellEntry.add(project.getName()+":"+sourceDocument.getName());

                        rowData.add(cellEntry);
                    }
                }
          }

            TableDataProvider provider = new TableDataProvider(columnHeaders,
                    rowData);
            List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

            for (int m = 0; m < provider.getColumnCount(); m++) {
                columns.add(new DocumentColumnMetaData(provider, m));
            }
            add(new DefaultDataTable("crowdSourceInformationTable", columns, provider, 10));

   /*         add(new Button("new", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    projectListForm.setVisible(true);
                }
            });*/

        }
    }

/*    private class ProjectListForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectListForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<Project>();

                            List<Project> allProjects = projectRepository.listProjects();

                           User user = userRepository.get(CROWD_USER);
                          for (Project project : allProjects) {
                                List<User> users = projectRepository.listProjectUsersWithPermissions(project);
                                if (users.contains(user)){
                                    allowedProject.add(project);
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    selectedProject = aNewSelection;
                    documentChoiceForm.setVisible(true);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });

        }
    }*/
/*    private class DocumentChoiceForm
    extends Form<SelectionModel>
{
    private static final long serialVersionUID = -1L;

    public DocumentChoiceForm(String id)
    {
        super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

        add(new CheckBoxMultipleChoice<SourceDocument>("document")
        {
            private static final long serialVersionUID = 1L;

            {
                setChoices(new LoadableDetachableModel<List<SourceDocument>>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<SourceDocument> load()
                    {
                       return projectRepository.listSourceDocuments(selectedProject);
                    }
                });
                setChoiceRenderer(new ChoiceRenderer<SourceDocument>("name"));
            }
        });

    }
}
    */
    private class CrowdDocumentDetailForm
    extends Form<SelectionModel>
{
    private static final long serialVersionUID = -1L;

    public CrowdDocumentDetailForm(String id)
    {
        super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

        add(new Label("document"));
        add(new Label("project"));
        add(new Label("link"));
        add(new Label("status"));

    }
}

    private class DocumentColumnMetaData
    extends AbstractColumn<List<String>>
{
    private int columnNumber;

    private Project project;

    public DocumentColumnMetaData(final TableDataProvider prov, final int colNumber)
    {
        super(new AbstractReadOnlyModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                return prov.getColNames().get(colNumber);

            }
        });
        columnNumber = colNumber;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
            final String componentId, final IModel<List<String>> rowModel)
    {
        int rowNumber = aCellItem.getIndex();
        aCellItem.setOutputMarkupId(true);

        final String value = rowModel.getObject().get(columnNumber).trim(); // the project

        if(rowNumber == rowModel.getObject().size()-1){
        aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                "/images_small/page_edit.png")));
        aCellItem.add(AttributeModifier.append("class", "centering"));
        aCellItem.add(new AjaxEventBehavior("onclick")
        {
            private static final long serialVersionUID = -4213621740511947285L;

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                SelectionModel selectionModel = new SelectionModel();
                selectionModel.project = value.substring(0, value.indexOf(":"));
                selectionModel.document = value.substring(value.indexOf(":")+1);
                crowdDocumentDetailForm.setVisible(true);
                crowdDocumentDetailForm.setModelObject(selectionModel);
                aTarget.add(crowdDocumentDetailForm);
                aTarget.appendJavaScript("window.location.reload()");
               // aTarget.add(projectListForm);
            }
        });
    }
        else{
            aCellItem.add(new Label(componentId, value));
        }
    }
}


    static private class SelectionModel
    implements Serializable
{
    private static final long serialVersionUID = -1L;

    /*private Project project;
    private SourceDocument document;*/
    String project;
    String document;
    String link;
    String status;
}
    private CrowdSourceForm crowdSourceForm;
   // private ProjectListForm projectListForm;
    private CrowdDocumentDetailForm crowdDocumentDetailForm;

    public CrowdSourcePage()
        throws UIMAException, IOException, ClassNotFoundException
    {
        crowdSourceForm = new CrowdSourceForm("crowdSourceForm");
        add(crowdSourceForm);
/*
        projectListForm = new ProjectListForm("projectListForm");
        projectListForm.setVisible(false);
        add(projectListForm);
*/
        crowdDocumentDetailForm = new CrowdDocumentDetailForm("crowdDocumentDetailForm");
        crowdDocumentDetailForm.setVisible(false);
        add(crowdDocumentDetailForm);
    }
}
