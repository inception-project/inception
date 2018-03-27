/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.project;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

public class RecommenderEditorPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annotationSchemaService;
    
    private IModel<Project> projectModel;
    private IModel<Recommender> recommenderModel;

    public RecommenderEditorPanel(String aId, IModel<Project> aProject,
            IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        projectModel = aProject;
        recommenderModel = aRecommender;
        
        Form<Recommender> form = new Form<>("form", CompoundPropertyModel.of(aRecommender));
        add(form);
        form.add(new Label("name"));
        form.add(new CheckBox("alwaysSelected"));
        form.add(new DropDownChoice<>("layer")
                .setChoices(LambdaModel.of(this::listLayers))
                .setChoiceRenderer(new ChoiceRenderer<>("uiName"))
                .setRequired(true)
                // The available tools depend on the layer, so reload the tools when the layer is
                // changed
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> 
                        t.add(form.get("tool"))))
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> 
                        t.add(form.get("feature")))));
        form.add(new DropDownChoice<>("feature")
                .setChoices(LambdaModel.of(this::listFeatures))
                .setRequired(true)
                .setOutputMarkupId(true));
        form.add(new DropDownChoice<>("tool")
                .setChoices(LambdaModel.of(this::listTools))
                .setRequired(true)
                .setOutputMarkupId(true));
        form.add(new NumberTextField<>("threshold", Float.class)
                .setMinimum(0.0f)
                .setMaximum(100.0f)
                .setStep(0.01f));
        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() != 0)));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() == 0)));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        setVisible(recommenderModel != null && recommenderModel.getObject() != null);
    }
    
    private List<AnnotationLayer> listLayers()
    {
        return annotationSchemaService.listAnnotationLayer(projectModel.getObject());
    }

    private List<String> listFeatures()
    {
        if (recommenderModel != null && recommenderModel.getObject().getLayer() != null) {
            List<String> features = new LinkedList<>();
            
            annotationSchemaService
                .listAnnotationFeature(recommenderModel.getObject().getLayer())
                .forEach(annotationFeature -> {
                    if (annotationFeature.getType() instanceof String) {
                        features.add(annotationFeature.getName());
                    }
                });   
            return features;
            
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<String> listTools()
    {
        if (recommenderModel != null && recommenderModel.getObject().getLayer() != null) {
            return recommendationService.getAvailableTools(recommenderModel.getObject().getLayer());
        }
        else {
            return Collections.emptyList();
        }
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Recommender> aForm) {
        Recommender recommender = aForm.getModelObject();
        recommender.setName(String.format(Locale.US, "[%s] %s (%.2f)",
                recommender.getLayer().getUiName(),
                StringUtils.substringAfterLast(recommender.getTool(), "."),
                recommender.getThreshold()));
        recommender.setProject(recommender.getLayer().getProject());
        recommendationService.createOrUpdateRecommender(recommender);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) {
        recommendationService.deleteRecommender(recommenderModel.getObject());
        actionCancel(aTarget);
    }
    
    private void actionCancel(AjaxRequestTarget aTarget) {
        recommenderModel.setObject(null);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
}
