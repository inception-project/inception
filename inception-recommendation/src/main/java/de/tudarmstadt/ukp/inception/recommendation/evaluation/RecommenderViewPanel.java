/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class RecommenderViewPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private static final String MID_FORM = "form";
    private static final String MID_NAME = "name";
    private static final String MID_FEATURE = "feature";
    private static final String MID_LAYER = "layer";
    private static final String MID_TOOL = "tool";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annotationSchemaService;
    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;
    private @SpringBean ApplicationEventPublisherHolder appEventPublisherHolder;
    private @SpringBean UserDao userDao;

    private TextField<String> nameField;
    private TextField<String> tool;
    private TextField<String> feature;
    private TextField<String> layer;

    private IModel<Recommender> recommenderModel;

    public RecommenderViewPanel(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        recommenderModel = aRecommender;

        Form<Recommender> form = new Form<>(MID_FORM, CompoundPropertyModel.of(aRecommender));
        add(form);
        
        nameField = new TextField<String>(MID_NAME, Model.of(getNameField()));
        form.add(nameField);
 
        tool = new TextField<String>(MID_TOOL, Model.of(getTool()));
        form.add(tool);

        feature = new TextField<String>(MID_FEATURE,  Model.of(getFeature()));
        form.add(feature);

        layer = new TextField<String>(MID_LAYER, Model.of(getLayer()));
        form.add(layer);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(recommenderModel != null && recommenderModel.getObject() != null);
    }
    
    private String getNameField()
    {
        if ((recommenderModel == null | recommenderModel.getObject() == null)) {
            return "";
        }
        return (recommenderModel.getObject().getName());
    }
    private String getTool()
    {
        if ((recommenderModel == null | recommenderModel.getObject() == null)) {
            return "";
        }
        return (recommenderModel.getObject().getTool());
    }
    private String getFeature()
    {
        if ((recommenderModel == null | recommenderModel.getObject() == null)) {
            return "";
        }
        return (recommenderModel.getObject().getFeature().getName());
    }
    private String getLayer()
    {
        if ((recommenderModel == null | recommenderModel.getObject() == null)) {
            return "";
        }
        return (recommenderModel.getObject().getLayer().getName());
    }
}
