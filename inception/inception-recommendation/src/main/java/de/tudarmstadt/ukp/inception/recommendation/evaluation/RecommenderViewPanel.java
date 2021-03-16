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
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ComponentPropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;

public class RecommenderViewPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private static final String MID_FORM = "form";
    private static final String MID_NAME = "name";
    private static final String MID_FEATURE = "feature";
    private static final String MID_LAYER = "layer";
    private static final String MID_TOOL = "tool";

    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;

    public RecommenderViewPanel(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        Form<Recommender> form = new Form<>(MID_FORM, CompoundPropertyModel.of(aRecommender));
        add(form);

        form.add(new TextField<>(MID_NAME, String.class));
        form.add(new TextField<>(MID_TOOL, LoadableDetachableModel.of(this::getToolName),
                String.class));
        form.add(new TextField<AnnotationFeature>(MID_FEATURE,
                new ComponentPropertyModel<>("feature.uiName")));
        form.add(new TextField<AnnotationLayer>(MID_LAYER,
                new ComponentPropertyModel<>("layer.uiName")));
    }

    public Recommender getModelObject()
    {
        return (Recommender) getDefaultModelObject();
    }

    private String getToolName()
    {
        if (getModelObject() == null) {
            return null;
        }

        RecommendationEngineFactory<?> factory = recommenderRegistry
                .getFactory(getModelObject().getTool());
        if (factory == null) {
            return "[UNSUPPORTED]";
        }

        return factory.getName();
    }
}
