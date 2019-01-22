/*
 * Copyright 2018
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos.OpenNlpPosRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos.OpenNlpPosRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos.OpenNlpPosRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.model.Chart;

public class EvaluationSimulationPanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String MID_CHART_CONTAINER = "chart-container";
    private static final String MID_SIMULATION_START_BUTTON = "simulation-start-button";
    private static final String MID_FORM = "form";
    
    private static final double TRAIN_PERCENTAGE = 0.8;
    private static final int INCREMENT = 250;
    private static final int LOW_SAMPLE_THRESHOLD = 10;
    
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationSimulationPanel.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userDao;
    
    private final WebComponent chartContainer;
    private final Project project;
    private final IModel<Recommender> selectedRecommenderPanel;

    public EvaluationSimulationPanel(String aId, Project aProject,
            IModel<Recommender> aSelectedRecommenderPanel)
    {
        super(aId);
        project = aProject;
        selectedRecommenderPanel = aSelectedRecommenderPanel;
                
        Form<Recommender> form = new Form<>(MID_FORM);
        add(form);

        chartContainer = new Label(MID_CHART_CONTAINER);
        chartContainer.setOutputMarkupId(true);
        form.add(chartContainer);
        
        form.add(new LambdaAjaxButton<>(MID_SIMULATION_START_BUTTON, this::actionStartEvaluation));
    }
    
    private void actionStartEvaluation(AjaxRequestTarget aTarget, Form<Void> aForm)
        throws IOException
    {
        //there must be some recommender selected by the user on the UI
        if (selectedRecommenderPanel.getObject() == null
                || selectedRecommenderPanel.getObject().getTool() == null) {
            LOG.error("Please select a recommender from the list");
            error("Please select a recommender from the list");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        Map<SourceDocument, AnnotationDocument> listAllDocuments = documentService
                .listAllDocuments(project, userDao.getCurrentUser());

        //create a list of CAS from the pre-annotated documents of the project
        List<CAS> casList = new ArrayList<>();
        listAllDocuments.forEach((source, annotation) -> {
            try {
                CAS cas = documentService.createOrReadInitialCas(source).getCas();
                casList.add(cas);
            }
            catch (IOException e1) {
                LOG.error("Unable to render chart", e1);
                error("Unable to render chart: " + e1.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }
        });
        
        IncrementalSplitter splitStrategy = new IncrementalSplitter(TRAIN_PERCENTAGE,
                INCREMENT, LOW_SAMPLE_THRESHOLD);

        RecommendationEngine recommender = getRecommendationEngine(selectedRecommenderPanel);
        if (recommender == null)
        {
            LOG.warn("Unknown Recommender selected");
            warn("Unknown Recommender selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        StringBuilder sb = new StringBuilder();

        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        int iterationCount = 0;
        while (splitStrategy.hasNext()) {
            splitStrategy.next();

            double score;
            try {
                score = recommender.evaluate(casList, splitStrategy);
            }
            catch (RecommendationException e) {
                LOG.error(e.toString(),e);
                continue;
            }

            sb.append(score + ",");
            iterationCount++;
        }

        String data = sb.toString();
        String recommenderName = selectedRecommenderPanel.getObject().getName();

        Chart chart = new Chart(chartContainer.getMarkupId(), iterationCount);
        chart.addLearningCurve( data, recommenderName);
        chart.renderChart(this,aTarget);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // import Js
        aResponse.render(JavaScriptHeaderItem
                .forReference(new WebjarsJavaScriptResourceReference("c3/current/c3.js")));
        aResponse.render(JavaScriptHeaderItem
                .forReference(new WebjarsJavaScriptResourceReference("d3js/current/d3.js")));

        // import Css
        aResponse.render(
                CssHeaderItem.forReference(new WebjarsCssResourceReference("c3/current/c3.css")));

    }

    /**
     * returns the recommender Engine for the given recommender model
     * 
     */
    private RecommendationEngine getRecommendationEngine(IModel<Recommender> recommenderModel)
    {
        if (recommenderModel.getObject().getTool().equals(StringMatchingRecommenderFactory.ID)) {
            return new StringMatchingRecommender(recommenderModel.getObject(),
                    new StringMatchingRecommenderTraits());
        }
        else if (recommenderModel.getObject().getTool().equals(OpenNlpPosRecommenderFactory.ID)) {
            return new OpenNlpPosRecommender(recommenderModel.getObject(),
                    new OpenNlpPosRecommenderTraits());
        }
        else if (recommenderModel.getObject().getTool().equals(OpenNlpNerRecommenderFactory.ID)) {
            return new OpenNlpNerRecommender(recommenderModel.getObject(),
                    new OpenNlpNerRecommenderTraits());
        }
        else if (recommenderModel.getObject().getTool()
                .equals(OpenNlpDoccatRecommenderFactory.ID)) {
            return new OpenNlpDoccatRecommender(recommenderModel.getObject(),
                    new OpenNlpDoccatRecommenderTraits());
        }
        else if (recommenderModel.getObject().getTool()
                .equals(OpenNlpDoccatRecommenderFactory.ID)) {
            return new OpenNlpDoccatRecommender(recommenderModel.getObject(),
                    new OpenNlpDoccatRecommenderTraits());
        }
        else if (recommenderModel.getObject().getTool().equals(ExternalRecommenderFactory.ID)) {
            return new ExternalRecommender(recommenderModel.getObject(),
                    new ExternalRecommenderTraits());
        }
        else {
            return null;
        }
    }
}
