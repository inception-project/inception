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
package de.tudarmstadt.ukp.inception.processing.tagset;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature_;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer_;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet_;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class TagSetExtractionPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 3568501821432165745L;

    private @SpringBean ProjectService projectService;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService schemaService;

    private CompoundPropertyModel<FormData> formModel;

    public TagSetExtractionPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        formModel = new CompoundPropertyModel<>(new FormData());
        queue(new Form<FormData>("form", formModel));

        queue(new DropDownChoice<>("tagSet") //
                .setNullValid(true) //
                .setChoices(LoadableDetachableModel.of(this::listTagSets)) //
                .setChoiceRenderer(new ChoiceRenderer<>(TagSet_.NAME)) //
                .setRequired(false));

        var featureChoice = new DropDownChoice<>("feature") //
                .setChoices(LoadableDetachableModel.of(this::listFeatures)) //
                .setChoiceRenderer(new ChoiceRenderer<>(AnnotationFeature_.UI_NAME)) //
                .setRequired(true);
        queue(featureChoice);

        queue(new DropDownChoice<>("layer") //
                .setChoices(LoadableDetachableModel.of(this::listLayers)) //
                .setChoiceRenderer(new ChoiceRenderer<>(AnnotationLayer_.UI_NAME)) //
                .setRequired(true) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        $ -> $.add(featureChoice))));

        queue(new CheckBox("addTagsetToFeature").setOutputMarkupId(true));

        queue(new LambdaAjaxButton<>("startProcessing", this::actionStartProcessing));

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
        schedulingService.enqueue(TagSetExtractionTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withProject(getModelObject()) //
                .withAnnotationFeature(formData.feature) //
                .withAddTagsetToFeature(formData.addTagsetToFeature) //
                .withTagSet(formData.tagSet) //
                .withTrigger("User request") //
                .build());
    }

    private List<TagSet> listTagSets()
    {
        return schemaService.listTagSets(getModelObject());
    }

    private List<AnnotationLayer> listLayers()
    {
        return schemaService.listAnnotationLayer(getModelObject());
    }

    private List<AnnotationFeature> listFeatures()
    {
        var layer = formModel.getObject().layer;
        if (layer == null) {
            return Collections.emptyList();
        }
        return schemaService.listAnnotationFeature(layer);
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -9011757425705942346L;

        private TagSet tagSet;
        private AnnotationLayer layer;
        private AnnotationFeature feature;
        private boolean addTagsetToFeature = true;
    }
}
