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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.wizard.CancelButton;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardModel;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBases;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.ui.kb.project.EnrichedKnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.project.EnrichedKnowledgeBaseUtils;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseListPanel;

/**
 * Wizard for registering a new knowledge base for a project.
 */
public class KnowledgeBaseCreationWizard extends BootstrapWizard {

    private static final long serialVersionUID = -3459525951269555510L;
    
    private static final int MAXIMUM_REMOTE_REPO_SUGGESTIONS = 10;

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<Project> projectModel;
    private DynamicWizardModel wizardModel;
    private CompoundPropertyModel<EnrichedKnowledgeBase> wizardDataModel;

    public KnowledgeBaseCreationWizard(String id, IModel<Project> aProjectModel) {
        super(id);

        projectModel = aProjectModel;
        wizardDataModel = new CompoundPropertyModel<>(new EnrichedKnowledgeBase());

        wizardModel = new DynamicWizardModel(new TypeStep(null, wizardDataModel));
        wizardModel.setLastVisible(false);
        init(wizardModel);
    }

    /**
     * Wizard step asking for the KB name and whether it's a local or remote repository.
     */
    private final class TypeStep extends DynamicWizardStep {

        private static final long serialVersionUID = 2632078392967948962L;
        
        private CompoundPropertyModel<EnrichedKnowledgeBase> model;

        public TypeStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep, "", "", model);
            this.model = model;
            
            add(new RequiredTextField<String>("name", model.bind("kb.name")));
            
            // subclassing is necessary for setting this form input as required
            add(new BootstrapRadioGroup<RepositoryType>("type", model.bind("kb.type"),
                    Arrays.asList(RepositoryType.values()),
                    new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this)) {

                private static final long serialVersionUID = -3015289695381851497L;

                @Override
                protected RadioGroup<RepositoryType> newRadioGroup(String id,
                        IModel<RepositoryType> model) {
                    RadioGroup<RepositoryType> group = super.newRadioGroup(id, model);
                    group.setRequired(true);
                    group.add(new AttributeAppender("class", " btn-group-justified"));
                    return group;
                }
            });
        }

        @Override
        public boolean isLastStep() {
            return false;
        }

        @Override
        public IDynamicWizardStep next() {
            switch (model.getObject().getKb().getType()) {
            case LOCAL:
                return new LocalRepositoryStep(this, model);
            case REMOTE:
                return new RemoteRepositoryStep(this, model);
            default:
                throw new IllegalStateException();            
            }
        }
    }
    
    /**
     * Wizard step providing a file upload functionality for local (native) knowledge bases.
     */
    private final class LocalRepositoryStep extends DynamicWizardStep {

        private static final long serialVersionUID = 8212277960059805657L;
        
        private CompoundPropertyModel<EnrichedKnowledgeBase> model;
        private FileUploadField fileUpload;

        public LocalRepositoryStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep);
            this.model = model;
            
            fileUpload = new FileUploadField("upload");
            add(fileUpload);
        }
        
        @Override
        public void applyState() {
            model.getObject().setFiles(fileUpload.getFileUploads());
        }

        @Override
        public boolean isLastStep() {
            return true;
        }

        @Override
        public IDynamicWizardStep next() {
            return null;
        }
    }

    /**
     * Wizard step asking for the remote repository URL.
     */
    private final class RemoteRepositoryStep extends DynamicWizardStep {

        private static final long serialVersionUID = -707885872360370015L;
        
        public RemoteRepositoryStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep, "", "", model);
            
            RequiredTextField<String> urlField = new RequiredTextField<>("url");
            urlField.add(EnrichedKnowledgeBaseUtils.URL_VALIDATOR);
            add(urlField);
            
            // for up to MAXIMUM_REMOTE_REPO_SUGGESTIONS of knowledge bases, create a link which
            // directly fills in the URL field (convenient for both developers AND users :))
            List<String> suggestions = new ArrayList<>(
                    KnowledgeBases.KNOWLEDGE_BASES.keySet());
            suggestions = suggestions.subList(0,
                    Math.min(suggestions.size(), MAXIMUM_REMOTE_REPO_SUGGESTIONS));
            add(new ListView<String>("suggestions", suggestions) {

                private static final long serialVersionUID = 4179629475064638272L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    // add a link for one knowledge base with proper label
                    LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", t -> {
                        model.getObject().setUrl(KnowledgeBases.KNOWLEDGE_BASES
                                .get(item.getModelObject()));
                        t.add(urlField);
                    });
                    link.add(new Label("suggestionLabel", item.getModelObject()));
                    item.add(link);
                }
            });            
        }

        @Override
        public boolean isLastStep() {
            return true;
        }

        @Override
        public IDynamicWizardStep next() {
            return null;
        }
    }

    @Override
    public void onFinish() {
        EnrichedKnowledgeBase ekb = wizardDataModel.getObject();
        // connect knowledge base to project
        ekb.getKb().setProject(projectModel.getObject());
        try {
            EnrichedKnowledgeBaseUtils.registerEkb(ekb, kbService);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @Override
    protected Component newButtonBar(String id) {
        // add Bootstrap-compatible button bar which closes the parent dialog via the cancel and
        // finish buttons
        Component buttonBar = new BootstrapWizardButtonBar(id, this) {

            private static final long serialVersionUID = 5657260438232087635L;

            @Override
            protected FinishButton newFinishButton(String id, IWizard wizard) {
                FinishButton button = new FinishButton(id, wizard) {

                    private static final long serialVersionUID = -7070739469409737740L;

                    @Override
                    public void onAfterSubmit() {
                        // update the list panel and close the dialog - this must be done in
                        // onAfterSubmit, otherwise it cancels out the call to onFinish()
                        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                        target.add(findParent(KnowledgeBaseListPanel.class));
                        findParent(KnowledgeBaseCreationDialog.class).close(target);
                    }
                };

                return button;
            }

            @Override
            protected CancelButton newCancelButton(String id, IWizard wizard) {
                CancelButton button = super.newCancelButton(id, wizard);
                button.add(new AjaxEventBehavior("click") {

                    private static final long serialVersionUID = 3425946914411261187L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        findParent(KnowledgeBaseCreationDialog.class).close(target);
                    }
                });
                return button;
            }
        };
        return buttonBar;
    }
}
