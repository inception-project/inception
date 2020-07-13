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
package de.tudarmstadt.ukp.inception.workload.dynamic.page.settings;


import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkflowProperties;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadProperties;

//Custom panel inside the page
public class WorkflowAndMonitoringPanel extends Panel
{

    //SpringBeans
    private @SpringBean WorkloadProperties workloadProperties;
    private @SpringBean WorkflowProperties workflowProperties;


    public WorkflowAndMonitoringPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        //Basic form
        Form<Void> form = new Form<>("form");
        add(form);

        //Add two possibilities to select for the workflow manager
        List<String> workflow = new ArrayList<>();
        workflow.add("Default workflow manager");
        workflow.add("Dynamic workflow manager");

        BootstrapRadioChoice<String> workflowChoices = new BootstrapRadioChoice<>("workflowRadios",
            new Model<>(getString("defaultWorkflow")), workflow);
        workflowChoices.setInline(true);
        workflowChoices.setOutputMarkupId(true);
        //Set default for the group
        workflowChoices.setModel(new Model<>(workflowProperties.getProperty()));

        //add them to the form
        form.add(workflowChoices);

        //Add two possibilities to select from the monitoring manager
        List<String> monitoring = new ArrayList<>();
        monitoring.add("Default monitoring page");
        monitoring.add("Workload monitoring page");

        BootstrapRadioChoice<String> monitoringChoices = new BootstrapRadioChoice<>("monitoringRadios",
            new Model<>(getString("defaultMonitoring")), monitoring);
        monitoringChoices.setInline(true);
        monitoringChoices.setOutputMarkupId(true);

        //Set default value for the group
        monitoringChoices.setModel(new Model<>(workloadProperties.getProperty()));


        //add them to the form
        form.add(monitoringChoices);

        //Finally, add the confirm button at the end

        Button confirm = new AjaxButton(getString("confirm"),Model.of("Confirm"))
        {
            @Override
            public void onSubmit(AjaxRequestTarget target)
            {
                if (monitoringChoices.getDefaultModelObjectAsString().equals(monitoring.get(0)))
                {
                    workloadProperties.setActive(false);
                } else {
                    workloadProperties.setActive(true);
                }

                if (workflowChoices.getDefaultModelObjectAsString().equals(workflow.get(0)))
                {
                    workflowProperties.setActive(false);
                } else {
                    workflowProperties.setActive(true);
                }
            }
        };

        form.add(confirm);
    }
}
