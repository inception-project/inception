/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.project;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderRegistry;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class DocumentRepositoryEditorPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private static final String MID_PROPERTIES_CONTAINER = "propertiesContainer";
    private static final String MID_PROPERTIES = "properties";

    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean AnnotationSchemaService annotationSchemaService;
    private @SpringBean ExternalSearchProviderRegistry repositoryRegistry;
    private @SpringBean ApplicationEventPublisherHolder appEventPublisherHolder;
    private @SpringBean UserDao userDao;

    private WebMarkupContainer propertiesContainer;
    private DropDownChoice<Pair<String, String>> typeChoice;
    private Form<DocumentRepository> form;

    private IModel<Project> projectModel;
    private IModel<DocumentRepository> repositoryModel;

    public DocumentRepositoryEditorPanel(String aId, IModel<Project> aProject,
            IModel<DocumentRepository> aRepository)
    {
        super(aId, aRepository);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        projectModel = aProject;
        repositoryModel = aRepository;

        form = new Form<>("form", CompoundPropertyModel.of(aRepository));
        add(form);

        form.add(new TextField<String>("name")
                .add(new LambdaAjaxFormSubmittingBehavior("change", t -> {
                    t.add(form);
                })));

        IModel<Pair<String, String>> typeModel = LambdaModelAdapter.of(() -> {
            return listTypes().stream()
                    .filter(r -> r.getKey().equals(repositoryModel.getObject().getType()))
                    .findFirst().orElse(null);
        }, (v) -> repositoryModel.getObject().setType(v.getKey()));

        typeChoice = new BootstrapSelect<Pair<String, String>>("type", typeModel, this::listTypes)
        {
            private static final long serialVersionUID = -1869081847783375166L;

            @Override
            protected void onModelChanged()
            {
                Component newProperties;
                if (form.getModelObject() != null && getModelObject() != null) {
                    ExternalSearchProviderFactory espf = repositoryRegistry
                            .getExternalSearchProviderFactory(getModelObject().getKey());
                    newProperties = espf.createTraitsEditor(MID_PROPERTIES, form.getModel());
                }
                else {
                    newProperties = new EmptyPanel(MID_PROPERTIES);
                }

                propertiesContainer.addOrReplace(newProperties);
            }
        };

        typeChoice.setChoiceRenderer(new ChoiceRenderer<Pair<String, String>>("value"));
        typeChoice.setRequired(true);
        typeChoice.setOutputMarkupId(true);
        typeChoice.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 229921732568860645L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(propertiesContainer);
            }
        });
        form.add(typeChoice);

        form.add(new AjaxButton("save")
        {
            private static final long serialVersionUID = -3902555252753037183L;

            @Override
            protected void onAfterSubmit(AjaxRequestTarget aTarget)
            {
                actionSave(aTarget);
            };
            
            @Override
            protected void onError(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);
            };
        });
        form.add(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() != null)));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() == null)));

        form.add(propertiesContainer = new WebMarkupContainer(MID_PROPERTIES_CONTAINER));
        propertiesContainer.setOutputMarkupId(true);

        propertiesContainer.add(new EmptyPanel(MID_PROPERTIES));
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(repositoryModel != null && repositoryModel.getObject() != null);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        // Since typeChoice uses a lambda model, it needs to be notified explicitly.
        typeChoice.modelChanged();
    }

    private List<Pair<String, String>> listTypes()
    {
        return repositoryRegistry.getExternalSearchProviderFactories().stream()
                .map(f -> Pair.of(f.getBeanName(), f.getDisplayName()))
                .collect(Collectors.toList());
    }

    private void actionSave(AjaxRequestTarget aTarget)
    {
        DocumentRepository documentRepository = form.getModelObject();
        documentRepository.setProject(projectModel.getObject());
        externalSearchService.createOrUpdateDocumentRepository(documentRepository);

        success("Document repository settings saved");
        
        // causes deselection after saving
        repositoryModel.setObject(null);

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(findParent(ProjectDocumentRepositoriesPanel.class));
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        externalSearchService.deleteDocumentRepository(repositoryModel.getObject());
        // TODO
        // Add a deleted event or not?
        // appEventPublisherHolder.get().publishEvent(
        // new RecommenderDeletedEvent(this, repositoryModel.getObject(),
        // userDao.getCurrentUser().getUsername(), projectModel.getObject()));
        actionCancel(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        repositoryModel.setObject(null);

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
}
