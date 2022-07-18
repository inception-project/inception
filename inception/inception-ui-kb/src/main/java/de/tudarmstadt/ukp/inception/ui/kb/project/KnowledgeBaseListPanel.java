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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.project.wizard.KnowledgeBaseCreationDialog;

public class KnowledgeBaseListPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = 8414963964131106164L;

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<Project> projectModel;
    private IModel<KnowledgeBase> kbModel;
    private OverviewListChoice<KnowledgeBase> overviewList;

    private KnowledgeBaseCreationDialog modal;

    public KnowledgeBaseListPanel(String id, IModel<Project> aProjectModel,
            IModel<KnowledgeBase> aKbModel)
    {
        super(id, aProjectModel);

        setOutputMarkupId(true);

        kbModel = aKbModel;

        projectModel = aProjectModel;
        overviewList = new OverviewListChoice<>("knowledgebases");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setChoices(LoadableDetachableModel
                .of(() -> kbService.getKnowledgeBases(projectModel.getObject())));
        overviewList.setModel(kbModel);
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        modal = new KnowledgeBaseCreationDialog("modal", projectModel);
        add(modal);
        add(new LambdaAjaxLink("new", this::actionCreate));
    }

    @Override
    protected void actionCreate(AjaxRequestTarget aTarget) throws Exception
    {
        modal.show(aTarget);
    }
}
