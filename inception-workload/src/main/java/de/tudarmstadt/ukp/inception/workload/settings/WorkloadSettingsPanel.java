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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistry;

public class WorkloadSettingsPanel extends Panel
{
    private static final long serialVersionUID = -6220828178550562376L;

    private final BootstrapSelect<String> workloadStrategy;
    private final Project project;

    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkloadRegistry workloadRegistry;

    public WorkloadSettingsPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        project = aProject.getObject();

        //Basic form
        Form<Void> form = new Form<>("form");

        workloadStrategy = new BootstrapSelect<String>("workloadStrategy");

        workloadStrategy.setModel(new Model<>());

        workloadStrategy.setRequired(true);
        workloadStrategy.setNullValid(false);

        //Add all possibilities to select from all entries in the registry
        List<String> strategies = new ArrayList<>();
        for (WorkloadExtension extension: workloadRegistry.getExtensions()) {
            strategies.add(extension.getId());
        }
        workloadStrategy.setChoices(strategies);

        //add them to the form
        form.add(workloadStrategy);

        //Finally, add the confirm button at the end
        Button confirm = new LambdaAjaxButton("save", this::actionConfirm);

        form.add(confirm);

        add(form);
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        workloadManagementService.setWorkloadManagerConfiguration(
            workloadStrategy.getModelObject(),project);

        success("Workload settings changed");
    }
}
