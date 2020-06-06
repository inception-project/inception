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
package de.tudarmstadt.ukp.inception.workload.dynamic.page.settings.workload;


import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;

//Custom panel inside the page
public class WorkflowAndMonitoringPanel extends Panel
{
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

        //Create the radio button group
        RadioChoice<String> workflowChoices =
            new RadioChoice<>("workflowRadios"
                , new Model<>(getString("defaultWorkflow")), workflow);
        //Set default for the group
        workflowChoices.setModel(new Model<String>(getString("defaultWorkflow")));

        //add them to the form
        form.add(workflowChoices);

        //Add two possibilities to select from the monitoring manager
        List<String> monitoring = new ArrayList<>();
        monitoring.add("Default monitoring page");
        monitoring.add("Workload monitoring page");

        //Craete the radio button group
        RadioChoice<String> monitoringChoices =
            new RadioChoice<>("monitoringRadios"
                , new Model<>(getString("defaultMonitoring")), monitoring);
        //Set default value for the group
        monitoringChoices.setModel(new Model<>(getString("defaultMonitoring")));

        //add them to the form
        form.add(monitoringChoices);

        //Finally, add the confirm button at the end
        form.add(new LambdaAjaxButton("confirm", this::actionConfirm));
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        //TODO logic for the button, set the correct workflow and monitoring page
        //according to the selection
    }
}
