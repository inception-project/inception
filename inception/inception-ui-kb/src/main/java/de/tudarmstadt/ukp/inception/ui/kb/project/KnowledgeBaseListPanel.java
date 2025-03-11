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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.KnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.ui.kb.project.wizard.KnowledgeBaseCreationDialog;

public class KnowledgeBaseListPanel
    extends ListPanel_ImplBase
{
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 8414963964131106164L;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private IModel<Project> projectModel;
    private IModel<KnowledgeBase> kbModel;
    private OverviewListChoice<KnowledgeBase> overviewList;
    private final BootstrapModalDialog dialog;
    private final LambdaAjaxLink addButton;

    private KnowledgeBaseCreationDialog modal;
    private final IModel<List<KnowledgeBaseInitializer>> knowledgeBaseInitializers;

    public KnowledgeBaseListPanel(String id, IModel<Project> aProjectModel,
            IModel<KnowledgeBase> aKbModel)
    {
        super(id, aProjectModel);

        setOutputMarkupId(true);

        kbModel = aKbModel;

        knowledgeBaseInitializers = LoadableDetachableModel.of(this::listKnowledgeBaseInitializers);
        add(LambdaBehavior.onDetach(knowledgeBaseInitializers::detach));

        dialog = new BootstrapModalDialog("dialog");
        dialog.trapFocus();
        queue(dialog);

        addButton = new LambdaAjaxLink("add", this::actionAddKnowledgeBase);
        addButton.setOutputMarkupPlaceholderTag(true);
        addButton.add(visibleWhenNot(knowledgeBaseInitializers.map(List::isEmpty)));
        add(addButton);

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

    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    @Override
    protected void actionCreate(AjaxRequestTarget aTarget) throws Exception
    {
        modal.show(aTarget);
    }

    private List<KnowledgeBaseInitializer> listKnowledgeBaseInitializers()
    {
        if (getModelObject() == null) {
            return emptyList();
        }

        return projectService.listProjectInitializers().stream()
                .filter(initializer -> initializer instanceof KnowledgeBaseInitializer)
                .map(KnowledgeBaseInitializer.class::cast)
                .filter(initializer -> !initializer.alreadyApplied(getModelObject())).toList();
    }

    private void actionAddKnowledgeBase(AjaxRequestTarget aTarget)
    {
        var dialogContent = new KnowledgeBaseTemplateSelectionDialogPanel(
                BootstrapModalDialog.CONTENT_ID, getModel(), knowledgeBaseInitializers);
        dialog.open(dialogContent, aTarget);
    }

    @OnEvent
    public void onLayerTemplateSelected(KnowledgeBaseTemplateSelectedEvent aEvent)
    {
        var target = aEvent.getTarget();
        var initializer = aEvent.getKnowledgeBaseInitializer();
        try {
            // target.add(initializersContainer);
            target.add(overviewList);
            target.add(addButton);
            target.addChildren(getPage(), IFeedback.class);
            var request = ProjectInitializationRequest.builder() //
                    .withProject(getModelObject()) //
                    .build();
            projectService.initializeProject(request, asList(initializer));
            success("Applyed knowledge base initializer [" + initializer.getName() + "]");
        }
        catch (Exception e) {
            error("Error applying knowledge base initializer [" + initializer.getName() + "]: "
                    + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error applying knowledge base initializer {}", initializer, e);
        }

        applicationEventPublisherHolder.get()
                .publishEvent(new KnowledgeBaseConfigurationChangedEvent(this, getModelObject()));
    }
}
