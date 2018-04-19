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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
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
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderDeletedEvent;
import de.tudarmstadt.ukp.inception.recommendation.model.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

public class RecommenderEditorPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annotationSchemaService;
    private @SpringBean ClassificationToolRegistry toolRegistry;
    private @SpringBean ApplicationEventPublisherHolder appEventPublisherHolder;
    private @SpringBean UserDao userDao;

    private IModel<Project> projectModel;
    private IModel<Recommender> recommenderModel;
    private Component threshold;

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
        form.add(new CheckBox("alwaysSelected")
                .add(new LambdaAjaxFormSubmittingBehavior("change", t -> {
                    t.add(form);
                })));
        form.add(new CheckBox("enabled"));
        form.add(new DropDownChoice<>("layer")
                .setChoices(LambdaModel.of(this::listLayers))
                .setChoiceRenderer(new ChoiceRenderer<>("uiName"))
                .setRequired(true)
                // The available features and tools tools depend on the layer, so reload them
                // when the layer is changed
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> { 
                    if (listFeatures().size() == 1) {
                        recommenderModel.getObject().setFeature(listFeatures().get(0));
                    } else {
                        recommenderModel.getObject().setFeature(null);
                    }
                    recommenderModel.getObject().setTool(null);
                    t.add(form.get("tool"));
                    t.add(form.get("feature")); 
                })));
        form.add(new DropDownChoice<>("feature")
                .setChoices(LambdaModel.of(this::listFeatures))
                .setRequired(true)
                .setOutputMarkupId(true)
                // The available tools depend on the feature, so reload the tools when the layer
                // is changed
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
                    recommenderModel.getObject().setTool(null);
                    t.add(form.get("tool"));
                })));
        
        IModel<Pair<String, String>> toolModel = LambdaModelAdapter.of(() -> {
            return listTools().stream()
                    .filter(r -> r.getKey().equals(recommenderModel.getObject().getTool()))
                    .findFirst().orElse(null);
        }, (v) -> recommenderModel.getObject().setTool(v.getKey()));
        form.add(new DropDownChoice<Pair<String, String>>("tool", toolModel,
                LambdaModel.of(this::listTools))
                .setChoiceRenderer(new ChoiceRenderer<Pair<String, String>>("value"))
                .setRequired(true)
                .setOutputMarkupId(true));
        form.add(threshold = new NumberTextField<>("threshold", Float.class)
                .setMinimum(0.0f)
                .setMaximum(100.0f)
                .setStep(0.01f)
                .setOutputMarkupId(true)
                .add(new Behavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onConfigure(org.apache.wicket.Component component)
                    {
                        component.setEnabled(!recommenderModel.getObject().isAlwaysSelected());
                    };
                }));
        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() != null)));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() == null)));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        setVisible(recommenderModel != null && recommenderModel.getObject() != null);
    }
    
    private List<AnnotationLayer> listLayers()
    {
        List<AnnotationLayer> layers = new ArrayList<>();
        
        for (AnnotationLayer layer : annotationSchemaService
                .listAnnotationLayer(projectModel.getObject())) {
            if (WebAnnoConst.SPAN_TYPE.equals(layer.getType())
                    && !Token.class.getName().equals(layer.getName())) {
                layers.add(layer);
            }
        }
        
        return layers;
    }

    private List<String> listFeatures()
    {
        if (recommenderModel != null && recommenderModel.getObject().getLayer() != null) {
            List<String> features = new ArrayList<>();
            
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
    
    private List<Pair<String, String>> listTools()
    {
        if (recommenderModel != null && recommenderModel.getObject().getLayer() != null
                && recommenderModel.getObject().getFeature() != null) {
            AnnotationLayer layer = recommenderModel.getObject().getLayer();
            AnnotationFeature feature = annotationSchemaService
                    .getFeature(recommenderModel.getObject().getFeature(), layer);
            return toolRegistry.getTools(layer, feature).stream()
                    .map(f -> Pair.of(f.getId(), f.getName()))
                    .collect(Collectors.toList());
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
        
        // causes deselection after saving
        recommenderModel.setObject(null);

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) {
        recommendationService.deleteRecommender(recommenderModel.getObject());
        appEventPublisherHolder.get().publishEvent(
            new RecommenderDeletedEvent(this, recommenderModel.getObject(),
                userDao.getCurrentUser().getUsername(), projectModel.getObject()));
        actionCancel(aTarget);
    }
    
    private void actionCancel(AjaxRequestTarget aTarget) {
        recommenderModel.setObject(null);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
}
