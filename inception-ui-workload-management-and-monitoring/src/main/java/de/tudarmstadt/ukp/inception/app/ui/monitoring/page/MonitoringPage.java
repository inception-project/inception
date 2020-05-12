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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.DataProvider;
import de.tudarmstadt.ukp.inception.app.ui.monitoring.support.MetaImageColumn;


@MountPath("MonitoringPage.html")
public class MonitoringPage extends ApplicationPageBase
{

    //Layout Panel
    private final Panel table;

    //Default annotations label and textbox
    private final NumberTextField DEFAULTDOCUMENTSNUMBERTEXTFIELD;


    //Columns in the datatable
    public static final String DOCUMENT = "Document";
    public static final String INPROGRESS = "InProgress";
    public static final String FINISHED = "Finished";
    public static final String METADATA = "Metadata";

    //Default annotations
    public int defaultAnnotations;

    //SpringBeans
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean DocumentService documentService;

    //Constructor
    public MonitoringPage(final PageParameters aPageParameters)
    {

        //Get current Project
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        Optional<Project> currentProject = getProjectFromParameters(projectParameter);

        //get Documents for current project
        List<SourceDocument> documentList = documentService.
            listSourceDocuments(currentProject.get());

        //Create list with data for the provider
        List<SourceDocument> data = new ArrayList<>();
        for (int i = 0; i < documentList.size(); i++)
        {
            data.add(documentList.get(i));
        }

        //Data Provider for the table
        DataProvider dataProvider = new DataProvider(data);

        //Init defaultDocumentsNumberTextField
        DEFAULTDOCUMENTSNUMBERTEXTFIELD = new NumberTextField("defaultDocumentsNumberTextField");

        //Columns of the table
        List<IColumn> columns = new ArrayList<>();

        //Each column creates TableMetaData
        columns.add(new LambdaColumn<>(new ResourceModel(DOCUMENT)
            , DOCUMENT, SourceDocument::getName));
        columns.add(new LambdaColumn<>(new ResourceModel(FINISHED)
            , FINISHED, SourceDocument::getState));
        columns.add(new LambdaColumn<>(new ResourceModel(INPROGRESS)
            , INPROGRESS, SourceDocument::getId));
        //Own column type
        columns.add(new MetaImageColumn(new ResourceModel(METADATA), METADATA));


        //The DefaultDataTable
        table = new DefaultDataTable("dataTable", columns, dataProvider, 20);

        //Miscellaneous for fields
        DEFAULTDOCUMENTSNUMBERTEXTFIELD.setRequired(true);

        //Add to page according to the following structure

        //Then default annotations
        add(DEFAULTDOCUMENTSNUMBERTEXTFIELD);

        //Finally the table
        add(table);

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

    public int getDefaultAnnotations()
    {
        return defaultAnnotations;
    }

    public void setDefaultAnnotations(int defaultAnnotations)
    {
        this.defaultAnnotations = defaultAnnotations;
    }
}
