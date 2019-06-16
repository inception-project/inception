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
        
        nameField = new TextField<>(MID_NAME, String.class);
        nameField.setRequired(true);
        form.add(nameField);
        
        tool = new TextField<>(MID_TOOL, String.class);
        tool.setRequired(true);
        form.add(tool);
        
        feature = new TextField<String>(MID_FEATURE,  String.class );
        form.add(feature);

        layer = new TextField<String>(MID_LAYER, String.class);
        form.add(layer);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(recommenderModel != null && recommenderModel.getObject() != null);
    }
}
