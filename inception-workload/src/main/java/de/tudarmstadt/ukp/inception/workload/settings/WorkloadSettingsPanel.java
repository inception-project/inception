/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.workload.settings;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerType;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;
import de.tudarmstadt.ukp.inception.workload.traits.DynamicWorkloadTrait;

/**
 * This class represents the panel "workload" shown in any projects settings. It can be used to
 * enable the experimental feature "workload". It consists of a simple dropdown menu from which a
 * project manager can select which workload type he wants to use.
 */
public class WorkloadSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -6220828178550562376L;

    private final BootstrapSelect<WorkloadManagerType> workloadStrategy;
    private final Project project;

    // Springbeans
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkloadManagerExtensionPoint workloadManagerExtensionPoint;

    /**
     * Constructor, creates the whole panel. Consists a a single form.
     */
    public WorkloadSettingsPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        project = aProject.getObject();

        // Basic form
        Form<Void> form = new Form<>("form");

        // Dropdown menu
        workloadStrategy = new BootstrapSelect<>("workloadStrategy");
        workloadStrategy
                .setChoiceRenderer(new LambdaChoiceRenderer<>(WorkloadManagerType::getUiName));
        workloadStrategy.setModel(LoadableDetachableModel.of(this::getWorkloadManager));
        workloadStrategy.setRequired(true);
        workloadStrategy.setNullValid(false);
        workloadStrategy.setChoices(workloadManagerExtensionPoint.getTypes());

        // add them to the form
        form.add(workloadStrategy);

        // Finally, add the confirm button at the end
        Button confirm = new LambdaAjaxButton<>("save", this::actionConfirm);
        form.add(confirm);

        add(form);
    }

    /**
     * This method returns the current WorkloadManagerType.
     */
    private WorkloadManagerType getWorkloadManager()
    {
        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        Extension extension = workloadManagerExtensionPoint.getExtension(manager.getType());
        return new WorkloadManagerType(extension.getId(), extension.getId());
    }

    /**
     * Confimation action of the button
     */
    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm) throws IOException
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        // Either traits are already available for the project
        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        if (manager != null) {
            manager.setType(workloadStrategy.getModelObject().getWorkloadManagerExtensionId());
            workloadManagementService.saveConfiguration(manager);
        }
        // or they are not available (first time after project creation or "older" projects)
        else {
            DynamicWorkloadTrait trait = new DynamicWorkloadTrait();
            manager.setType(workloadStrategy.getModelObject().getWorkloadManagerExtensionId());
            manager.setTraits(JSONUtil.toJsonString(trait));
            workloadManagementService.saveConfiguration(manager);
        }
        // In both cases, get the feedback message that it is properly saved now
        success("Workload settings saved");
    }
}
