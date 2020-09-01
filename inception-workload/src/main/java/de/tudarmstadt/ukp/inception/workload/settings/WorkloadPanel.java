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
import org.springframework.beans.factory.annotation.Autowired;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistry;

//Custom panel inside the page
public class WorkloadPanel extends Panel
{
    private static final long serialVersionUID = -6220828178550562376L;

    private final List<String> monitoring;
    private final BootstrapRadioChoice<String> monitoringChoices;
    private final Project project;

    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkloadRegistry workloadRegistry;

    @Autowired
    public WorkloadPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        project = aProject.getObject();

        //Basic form
        Form<Void> form = new Form<>("form");

        //Add two possibilities to select from the monitoring manager
        monitoring = new ArrayList<>();
        monitoring.add(getString("defaultMonitoring"));
        monitoring.add(getString("workload"));

        monitoringChoices = new BootstrapRadioChoice<>("monitoringRadios",
            new Model<>(getString("defaultMonitoring")), monitoring);
        monitoringChoices.setInline(true);
        monitoringChoices.setOutputMarkupId(true);

        //Set default value for the group
        switch (workloadRegistry.getSelectedExtension(project)) {
        case("StaticWorkloadExtension"):
            monitoringChoices.setModel(new Model(getString("defaultMonitoring")));
            break;
        case("DynamicWorkloadExtension"):
            monitoringChoices.setModel(new Model(getString("workload")));
        }

        //add them to the form
        form.add(monitoringChoices);

        //Finally, add the confirm button at the end
        Button confirm = new LambdaAjaxButton("save", this::actionConfirm);

        form.add(confirm);

        add(form);
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        if (monitoringChoices.getDefaultModelObjectAsString().equals(getString("workload"))) {
            workloadRegistry.setSelectedExtension(project,
                workloadRegistry.getExtensions().get(0).getId());
        } else {
            workloadRegistry.setSelectedExtension(project,
                workloadRegistry.getExtensions().get(1).getId());
        }

        success("Workflow and workload settings changed");
    }
}
