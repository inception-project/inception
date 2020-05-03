/*
 * Copyright 2017
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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.DataProvider;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.TableMetaData;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

public class Monitoring extends ApplicationPageBase
{
    //Default annotations label and text box
    public static final String DEFAULTDOCUMENTSLABEL = "default # of documents:";
    public static final TextField DEFAULTDOCUMENTSTEXTFIELD = new TextField("defaultDocumentsTextField");

    //Layout Panel
    private Panel table;

    //Data of the table
    private ListDataProvider data;

    //Menu
    private DashboardMenu menu;

    //Columns in the datatable
    public static final String DOCUMENT = "Document";
    public static final String INPROGRESS = "In Progress";
    public static final String FINISHED = "Finished";

    //SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;

    //Constructor
    public Monitoring(final PageParameters aPageParameters)
    {
        //Get current user
        User user = userRepository.getCurrentUser();

        //Get current Project
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        Optional<Project> currentProject = getProjectFromParameters(projectParameter);

        //get Documents for current project
        List<SourceDocument> documentList = documentService.listSourceDocuments(currentProject.
            get());

        //Headers of the Table
        List<String> headers = new ArrayList<>();
        headers.add(DOCUMENT);
        headers.add(INPROGRESS);
        headers.add(FINISHED);

        //Create empty List with data
        //Create provider with empty list
        List<List<String>> data = new ArrayList<>();
        for (int i = 0; i < documentList.size(); i++)
        {
            data.add(new ArrayList<>());
            for (int j = 0; j < headers.size(); j++)
            {
                data.get(i).add(j,"");
            }
        }

        //Data Provider for the able
        DataProvider dataProvider = new DataProvider(headers, data);

        //Columns of the table
        List<IColumn<?,?>> columns = new ArrayList<>();
        for (int j = 0; j < dataProvider.getTableHeaders().size(); j++)
        {
            //Each column creates TableMetaData
            columns.add(new TableMetaData(dataProvider, j, documentList));
        }

        //The DefaultDataTable
        table = new DefaultDataTable("dataTable", columns, dataProvider, 10);

        //Miscellaneous for fields
        //DEFAULTDOCUMENTSTEXTFIELD.setRequired(true);


        //Add to page according to the following structure

        //Menu first
        menu = new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems));
        add(menu);

        //Then default annotations
        add(DEFAULTDOCUMENTSTEXTFIELD);

        //Finally table
        add(table);

    }


    //Return current project
    private Optional<Project> getProjectFromParameters(StringValue aProject)
    {
        if (aProject == null || aProject.isEmpty())
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of(projectService.getProject(aProject.toLong()));
        }
        catch (NoResultException e)
        {
            return Optional.empty();
        }
    }

    //Used for creating the menu
    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream().filter(item -> item.getPath().matches("/[^/]+"))
            .collect(Collectors.toList());
    }
}
