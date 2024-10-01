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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_NONE;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_RDF4J_LUCENE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapWizard;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapWizardButtonBar;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.kb.project.AccessSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.AccessSpecificSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.GeneralSettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseIriPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseListPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;

/**
 * Wizard for registering a new knowledge base for a project.
 */
public class KnowledgeBaseCreationWizard
    extends BootstrapWizard
{
    private static final long serialVersionUID = -3459525951269555510L;

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

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseCreationWizard.class);

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    private final IModel<Project> projectModel;
    private final DynamicWizardModel wizardModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> wizardDataModel;
    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;

    public KnowledgeBaseCreationWizard(String id, IModel<Project> aProjectModel)
    {
        super(id);

        projectModel = aProjectModel;
        var kb = new KnowledgeBase();
        wizardDataModel = new CompoundPropertyModel<>(new KnowledgeBaseWrapper(kb));
        wizardModel = new DynamicWizardModel(new TypeStep(null, wizardDataModel));
        wizardModel.setLastVisible(false);
        knowledgeBaseProfiles = readKbProfiles();

        init(wizardModel);
    }

    private Map<String, KnowledgeBaseProfile> readKbProfiles()
    {
        Map<String, KnowledgeBaseProfile> profiles = new HashMap<>();
        try {
            profiles = KnowledgeBaseProfile.readKnowledgeBaseProfiles().entrySet().stream()
                    .filter(e -> !e.getValue().isDisabled())
                    .sorted(comparing(e -> e.getValue().getName()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        catch (IOException e) {
            error("Unable to read knowledge base profiles " + e.getMessage());
            LOG.error("Unable to read knowledge base profiles ", e);
        }
        return profiles;
    }

    /**
     * Wizard step asking for the KB name and whether it's a local or remote repository. and
     * language
     */
    private final class TypeStep
        extends DynamicWizardStep
    {

        private static final long serialVersionUID = 2632078392967948962L;

        private CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

        public TypeStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<KnowledgeBaseWrapper> aKbModel)
        {
            super(previousStep, "Create Knowledgebase", "", aKbModel);
            kbModel = aKbModel;

            var generalSettings = new GeneralSettingsPanel("generalSettings", projectModel,
                    aKbModel);
            add(generalSettings);
            generalSettings.get("enabled").setOutputMarkupId(true).setVisible(false);

            Component accessSettings = new AccessSettingsPanel("accessSettings", aKbModel);
            add(accessSettings);
            accessSettings.get("writeprotection").setOutputMarkupId(true).setVisible(false);
        }

        @Override
        public void applyState()
        {
            kbModel.getObject().getKb().setMaxResults(kbProperties.getDefaultMaxResults());

            switch (kbModel.getObject().getKb().getType()) {
            case LOCAL:
                // Local KBs are writable by default
                kbModel.getObject().getKb().setReadOnly(false);
                // local KBs are always RDF4J + Lucene, so we can set the FTS mode accordingly
                kbModel.getObject().getKb().setFullTextSearchIri(FTS_RDF4J_LUCENE.stringValue());
                break;
            case REMOTE:
                // Local KBs are read-only
                kbModel.getObject().getKb().setReadOnly(true);
                // remote KBs are by default not using FTS but if we apply a remote DB profile,
                // then it will set the FTS according to the setting in the profile
                kbModel.getObject().getKb().setFullTextSearchIri(FTS_NONE.stringValue());
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
            return new AccessSpecificSettingsStep(this, kbModel);
        }
    }

    /**
     * Wizard step providing a file upload functionality for local (native) knowledge bases.
     */
    private final class AccessSpecificSettingsStep
        extends DynamicWizardStep
    {
        private static final long serialVersionUID = 8212277960059805657L;

        private final AccessSpecificSettingsPanel panel;
        private CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

        public AccessSpecificSettingsStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<KnowledgeBaseWrapper> aKbModel)
        {
            super(previousStep, "Create Knowledgebase", "", aKbModel);
            kbModel = aKbModel;

            panel = new AccessSpecificSettingsPanel("accessSpecificSettings", kbModel,
                    knowledgeBaseProfiles);
            add(panel);
        }

        @Override
        public void applyState()
        {
            panel.applyState();
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
            super(previousStep, "Create Knowledgebase", "", aModel);
            model = aModel;

            add(new KnowledgeBaseIriPanel("iriPanel", model));
        }

        @Override
        public void applyState()
        {
            try {
                var wrapper = wizardDataModel.getObject();
                var kb = wrapper.getKb();

                kb.setProject(projectModel.getObject());
                kb.setTraits(JSONUtil.toJsonString(wrapper.getTraits()));

                // set up the repository config, then register the knowledge base
                switch (kb.getType()) {
                case LOCAL:
                    finalizeLocalRepositoryConfiguration(wrapper, kb);
                    break;
                case REMOTE:
                    finalizeRemoteRepositoryConfiguration(wrapper, kb);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported knowledge base type [" + kb.getType() + "]");
                }
            }
            catch (Exception e) {
                error("Failed to create knowledge base: " + e.getMessage());
            }
        }

        private void finalizeRemoteRepositoryConfiguration(KnowledgeBaseWrapper wrapper,
                KnowledgeBase kb)
        {
            RepositoryImplConfig cfg = kbService.getRemoteConfig(wrapper.getUrl());
            kbService.registerKnowledgeBase(kb, cfg);
            success("Created knowledge base: " + kb.getName());
        }

        private void finalizeLocalRepositoryConfiguration(KnowledgeBaseWrapper wrapper,
                KnowledgeBase kb)
        {
            RepositoryImplConfig cfg = kbService.getNativeConfig();
            kbService.registerKnowledgeBase(kb, cfg);
            success("Created knowledge base: " + kb.getName());

            kbService.defineBaseProperties(kb);

            for (Pair<String, File> f : wrapper.getFiles()) {
                try (InputStream is = new FileInputStream(f.getValue())) {
                    kbService.importData(kb, f.getValue().getName(), is);
                    success("Imported: " + f.getKey());
                }
                catch (Exception e) {
                    error("Failed to import: " + f.getKey());
                }
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
        Component buttonBar = new BootstrapWizardButtonBar(id, this)
        {

            private static final long serialVersionUID = 5657260438232087635L;

            @Override
            protected FinishButton newFinishButton(String aId, IWizard aWizard)
            {
                FinishButton button = new FinishButton(aId, aWizard)
                {
                    private static final long serialVersionUID = -7070739469409737740L;

                    @Override
                    public void onAfterSubmit()
                    {
                        // update the list panel and close the dialog - this must be done in
                        // onAfterSubmit, otherwise it cancels out the call to onFinish()

                        IWizardStep step = wizardModel.getActiveStep();
                        if (step.isComplete()) {
                            AjaxRequestTarget target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class).get();
                            target.add(findParent(KnowledgeBaseListPanel.class));
                            target.addChildren(getPage(), IFeedback.class);
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
                button.add(new AjaxEventBehavior("click")
                {

                    private static final long serialVersionUID = 3425946914411261187L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target)
                    {
                        findParent(KnowledgeBaseCreationDialog.class).close(target);
                    }
                });
                return button;
            }
        };
        return buttonBar;
    }
}
