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
package de.tudarmstadt.ukp.inception.processing.curation;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.settings.MergeStrategyPanel;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class BulkCurationPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 3568501821432165745L;

    private static final String MID_FORM = "form";
    private static final String MID_MERGE_STRATEGY = "mergeStrategy";

    private @SpringBean ProjectService projectService;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean CurationService curationService;

    public BulkCurationPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        var formData = new FormData();
        formData.mergeStrategy = loadCurationWorkflow();

        var formModel = CompoundPropertyModel.of(formData);

        var form = new LambdaForm<>(MID_FORM, CompoundPropertyModel.of(formData));
        queue(form);

        queue(new MergeStrategyPanel(MID_MERGE_STRATEGY, formModel.bind(MID_MERGE_STRATEGY)));

        queue(new LambdaAjaxButton<>("startProcessing", this::actionStartProcessing)
                .triggerAfterSubmit());

        var closeDialogButton = new LambdaAjaxLink("closeDialog", this::actionCancel);
        closeDialogButton.setOutputMarkupId(true);
        queue(closeDialogButton);
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    private void actionStartProcessing(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        findParent(ModalDialog.class).close(aTarget);

        var formData = aForm.getModelObject();

        var layers = schemaService.listSupportedLayers(getModelObject());

        schedulingService.enqueue(BulkCurationTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withProject(getModelObject()) //
                .withTrigger("User request") //
                .withTargetUser(userService.getCurationUser().getUsername()) //
                .withCurationWorkflow(formData.mergeStrategy) //
                .withAnnotationLayers(layers) //
                .build());
    }

    private CurationWorkflow loadCurationWorkflow()
    {
        return curationService.readOrCreateCurationWorkflow(getModelObject());
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -9011757425705942346L;

        private CurationWorkflow mergeStrategy;
    }
}
