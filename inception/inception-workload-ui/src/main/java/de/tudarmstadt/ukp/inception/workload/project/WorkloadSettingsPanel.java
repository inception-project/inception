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
package de.tudarmstadt.ukp.inception.workload.project;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerType;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * This class represents the panel "workload" shown in any projects settings. It can be used to
 * enable the experimental feature "workload". It consists of a simple dropdown menu from which a
 * project manager can select which workload type he wants to use.
 */
public class WorkloadSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -6220828178550562376L;

    private final DropDownChoice<WorkloadManagerType> workloadStrategy;
    private final Project project;

    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkloadManagerExtensionPoint workloadManagerExtensionPoint;

    public WorkloadSettingsPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        project = aProject.getObject();

        // Basic form
        Form<Void> form = new Form<>("form");

        form.add(new DocLink("workloadHelpLink", "sect_workload"));

        // Dropdown menu
        workloadStrategy = new DropDownChoice<>("workloadStrategy");
        workloadStrategy
                .setChoiceRenderer(new LambdaChoiceRenderer<>(WorkloadManagerType::getUiName));
        workloadStrategy.setModel(LoadableDetachableModel.of(this::getWorkloadManager));
        workloadStrategy.setRequired(true);
        workloadStrategy.setNullValid(false);
        workloadStrategy.setChoices(workloadManagerExtensionPoint.getTypes());

        form.add(workloadStrategy);

        // Finally, add the confirm button at the end
        Button confirm = new LambdaAjaxButton<>("save", this::actionConfirm);
        form.add(confirm);

        add(form);
    }

    /**
     * @return current {@link WorkloadManager}
     */
    private WorkloadManagerType getWorkloadManager()
    {
        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        WorkloadManagerExtension<?> extension = workloadManagerExtensionPoint
                .getExtension(manager.getType()).orElseThrow();
        return new WorkloadManagerType(extension.getId(), extension.getId());
    }

    /**
     * Confirmation action of the button
     */
    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm) throws IOException
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        manager.setType(workloadStrategy.getModelObject().getWorkloadManagerExtensionId());
        workloadManagementService.saveConfiguration(manager);

        success("Workload settings saved");
    }
}
