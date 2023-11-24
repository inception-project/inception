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
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

public class TagSetSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private OverviewListChoice<TagSet> overviewList;
    private TagSetImportPanel tagSetImportPanel;

    private IModel<Project> selectedProject;

    public TagSetSelectionPanel(String id, IModel<Project> aProject, IModel<TagSet> aTagset)
    {
        super(id, aProject);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        selectedProject = aProject;

        overviewList = new OverviewListChoice<>("tagset");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(aTagset);
        overviewList.setChoices(LoadableDetachableModel.of(this::listTagSets));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        add(new LambdaAjaxLink("create", this::actionCreate));

        tagSetImportPanel = new TagSetImportPanel("importPanel", selectedProject);
        add(tagSetImportPanel);
    }

    private List<TagSet> listTagSets()
    {
        if (selectedProject.getObject() != null) {
            return annotationSchemaService.listTagSets(selectedProject.getObject());
        }
        else {
            return Collections.emptyList();
        }
    }
}
