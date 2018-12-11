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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardButton;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardModel;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.ui.core.bootstrap.BootstrapWizard;
import de.tudarmstadt.ukp.inception.ui.core.bootstrap.BootstrapWizardButtonBar;
import de.tudarmstadt.ukp.inception.ui.kb.project.AccessSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.AccessSpecificSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.GeneralSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseIriPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseListPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;


/**
 * Wizard for registering a new knowledge base for a project.
 */
public class KnowledgeBaseCreationWizard extends BootstrapWizard {
    
    /*-
     * Wizard structure as of 2018-02 (use http://asciiflow.com):
     * 
     *             REMOTE                                   
     *            +-------> RemoteRepS. +-+                  
     *            |                       |                  
     * TypeStep +-+                       +-> SchemaConfigS. +-> FINISH
     *            |                       |
     *            +-------> LocalRepS.  +-+
     *             LOCAL
     */

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseCreationWizard.class);
    private static final long serialVersionUID = -3459525951269555510L;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbproperties;

    private final IModel<Project> projectModel;
    private final DynamicWizardModel wizardModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> wizardDataModel;
    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;

    public KnowledgeBaseCreationWizard(String id, IModel<Project> aProjectModel)
    {
        super(id);

        projectModel = aProjectModel;
        wizardDataModel = new CompoundPropertyModel<>(new KnowledgeBaseWrapper());
        wizardModel = new DynamicWizardModel(new TypeStep(null, wizardDataModel));
        wizardModel.setLastVisible(false);
        knowledgeBaseProfiles = readKbProfiles();

        init(wizardModel);
    }

    private Map<String, KnowledgeBaseProfile> readKbProfiles()
    {
        Map<String, KnowledgeBaseProfile> profiles = new HashMap<>();
        try {
            profiles = kbService.readKnowledgeBaseProfiles();
        }
        catch (IOException e) {
            error("Unable to read knowledge base profiles " + e.getMessage());
            log.error("Unable to read knowledge base profiles ", e);
        }
        return profiles;
    }

    /**
     * Wizard step asking for the KB name and whether it's a local or remote repository.
     * and language
     */
    private final class TypeStep extends DynamicWizardStep {

        private static final long serialVersionUID = 2632078392967948962L;
        
        private CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

        public TypeStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<KnowledgeBaseWrapper> aKbModel)
        {
            super(previousStep, "", "", aKbModel);
            kbModel = aKbModel;

            Component generalSettings = new GeneralSettingsPanel("generalSettings", projectModel,
                aKbModel);
            add(generalSettings);
            generalSettings.get("enabled").setVisible(false);

            Component accessSettings = new AccessSettingsPanel("accessSettings", projectModel,
                aKbModel);
            add(accessSettings);
            accessSettings.get("writeprotection").setVisible(false);

        }

        @Override
        public boolean isLastStep() {
            return false;
        }

        @Override
        public IDynamicWizardStep next() {
            return new AccessSpecificSettingsStep(this, kbModel);
        }
    }
    
    /**
     * Wizard step providing a file upload functionality for local (native) knowledge bases.
     */
    private final class AccessSpecificSettingsStep
        extends DynamicWizardStep {

        private static final long serialVersionUID = 8212277960059805657L;

        private CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

        public AccessSpecificSettingsStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<KnowledgeBaseWrapper> aKbModel)
        {
            super(previousStep, "", "", aKbModel);
            kbModel = aKbModel;
            kbModel.getObject().setFiles(new ArrayList<>());

            Component accessSpecificSettings = new AccessSpecificSettingsPanel(
                "accessSpecificSettings", projectModel, kbModel, knowledgeBaseProfiles);
            add(accessSpecificSettings);
            accessSpecificSettings.get("localSpecificSettings:exportButtons").setVisible(false);
            accessSpecificSettings.get("localSpecificSettings:clear").setVisible(false);
        }
        
        @Override
        public void applyState()
        {

            switch (kbModel.getObject().getKb().getType()) {
            case LOCAL:
                // local knowledge bases are editable by default
                kbModel.getObject().getKb().setReadOnly(false);
                break;
            case REMOTE:
                // MB: as of 2018-02, all remote knowledge bases are read-only, hence the
                // PermissionsStep is currently not shown. Therefore, set read-only property here
                // manually.
                kbModel.getObject().getKb().setReadOnly(true);
            }
        }

        @Override
        public boolean isLastStep()
        {
            return false;
        }

        @Override
        public IDynamicWizardStep next()
        {
            return new SchemaConfigurationStep(this, kbModel);
        }

        @Override
        public boolean isComplete()
        {
            return true;
        }
    }

    /**
     * Wizard step asking for the knowledge base schema
     */
    private final class SchemaConfigurationStep
        extends DynamicWizardStep
    {
        private static final long serialVersionUID = -12355235971946712L;

        private final CompoundPropertyModel<KnowledgeBaseWrapper> model;

        public SchemaConfigurationStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
        {
            super(previousStep, "", "", aModel);
            model = aModel;

            add(new KnowledgeBaseIriPanel("iriPanel", model));
        }

        @Override
        public void applyState()
        {   
            KnowledgeBaseWrapper wrapper = wizardDataModel.getObject();
            
            wrapper.getKb().setProject(projectModel.getObject());

            try {
                KnowledgeBaseWrapper.registerKb(wrapper, kbService);
            }
            catch (Exception e) {
                error(e.getMessage());

            }
        }

        @Override
        public boolean isComplete()
        {
            return true;
        }

        @Override
        public boolean isLastStep()
        {
            return true;
        }

        @Override
        public IDynamicWizardStep next()
        {
            return null;
        }
    }

    @Override
    protected Component newButtonBar(String id)
    {
        // add Bootstrap-compatible button bar which closes the parent dialog via the cancel and
        // finish buttons
        Component buttonBar = new BootstrapWizardButtonBar(id, this) {

            private static final long serialVersionUID = 5657260438232087635L;

            @Override
            protected FinishButton newFinishButton(String aId, IWizard aWizard)
            {
                FinishButton button = new FinishButton(aId, aWizard)
                {
                    private static final long serialVersionUID = -7070739469409737740L;

                    @Override
                    public void onAfterSubmit() {
                        // update the list panel and close the dialog - this must be done in
                        // onAfterSubmit, otherwise it cancels out the call to onFinish()
                        
                        IWizardStep step = wizardModel.getActiveStep();
                        if (step.isComplete()) {
                            AjaxRequestTarget target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class)
                                    .get();
                            target.add(findParent(KnowledgeBaseListPanel.class));
                            findParent(KnowledgeBaseCreationDialog.class).close(target);
                        }
                    }
                };
                return button;
            }

            @Override
            protected WizardButton newCancelButton(String aId, IWizard aWizard)
            {
                WizardButton button = super.newCancelButton(aId, aWizard);
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
