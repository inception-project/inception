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

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
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

    private static final String MID_SAVE = "save";
    private static final String MID_FORM = "form";
    private static final String MID_WORKLOAD_HELP_LINK = "workloadHelpLink";
    private static final String MID_WORKLOAD_STRATEGY = "workloadStrategy";
    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";

    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean WorkloadManagerExtensionPoint workloadManagerExtensionPoint;

    private final DropDownChoice<WorkloadManagerType> workloadStrategy;
    private final Project project;
    private final WebMarkupContainer traitsContainer;
    private final CompoundPropertyModel<WorkloadManager> workloadManager;

    public WorkloadSettingsPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        project = aProject.getObject();

        workloadManager = CompoundPropertyModel
                .of(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(project));
        Form<WorkloadManager> form = new Form<>(MID_FORM, workloadManager);

        form.add(new DocLink(MID_WORKLOAD_HELP_LINK, "sect_workload"));

        form.add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupId(true);

        workloadStrategy = new DropDownChoice<>(MID_WORKLOAD_STRATEGY)
        {
            private static final long serialVersionUID = 9069776195986324794L;

            @Override
            protected void onModelChanged()
            {
                // If the feature type has changed, we need to set up a new traits editor
                Component newTraits;
                var wlm = workloadManager.getObject();
                if (wlm != null) {
                    var wlmExt = workloadManagerExtensionPoint.getExtension(wlm.getType())
                            .orElseThrow();
                    newTraits = wlmExt.createTraitsEditor(MID_TRAITS, workloadManager);
                }
                else {
                    newTraits = new EmptyPanel(MID_TRAITS);
                }

                traitsContainer.addOrReplace(newTraits);
            }
        };
        workloadStrategy
                .setChoiceRenderer(new LambdaChoiceRenderer<>(WorkloadManagerType::getUiName));
        workloadStrategy.setChoices(workloadManagerExtensionPoint.getTypes());
        workloadStrategy.setModel(LambdaModelAdapter.of( //
                () -> workloadManagerExtensionPoint
                        .getWorkloadManagerType(workloadManager.getObject()),
                (v) -> workloadManager.getObject().setType(v.getWorkloadManagerExtensionId())));
        workloadStrategy.setRequired(true);
        workloadStrategy.setNullValid(false);
        workloadStrategy.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
            _target.add(traitsContainer);
        }));

        form.add(workloadStrategy);

        // Processing the data in onAfterSubmit so the traits panel can use the override onSubmit in
        // its nested form and store the traits before we clear the currently selected data.
        form.add(new LambdaAjaxButton<>(MID_SAVE, this::actionSave).triggerAfterSubmit());

        add(form);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<?> aForm) throws IOException
    {
        workloadManagementService.saveConfiguration(workloadManager.getObject());
        success("Workload settings saved");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
