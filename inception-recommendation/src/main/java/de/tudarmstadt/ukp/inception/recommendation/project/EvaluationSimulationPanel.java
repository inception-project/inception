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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.chart.Chart;
import de.tudarmstadt.ukp.inception.recommendation.model.ChartData;

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
    private final static int MAX_POINTS_TO_PLOT = 50;

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationSimulationPanel.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userDao;
    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;

    private final Chart chart;
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
        
        chart = new Chart(MID_CHART_CONTAINER, Model.of());
        chart.setOutputMarkupId(true);
        form.add(chart);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        LambdaAjaxButton startButton = new LambdaAjaxButton(MID_SIMULATION_START_BUTTON,
            (_target, _form) -> {
                String scores = evaluate(_target);
                
                if (scores == null) {
                    return;
                }
                
                ChartData learningCurve = new ChartData();
                
                Map<String,String> curveData = new HashMap<String,String>();
                
                curveData.put(selectedRecommenderPanel.getObject().getName(),scores);
                learningCurve.setCurveData(curveData);
                learningCurve.setMaximumPointsToPlot(MAX_POINTS_TO_PLOT);
                learningCurve.setaRequestHandler(_target);
                
                chart.setDefaultModel(Model.of(learningCurve));
            });
        
        form.add(startButton);
    }
    
    private String evaluate(AjaxRequestTarget aTarget)
        throws IOException
    {
        //there must be some recommender selected by the user on the UI
        if (selectedRecommenderPanel.getObject() == null
                || selectedRecommenderPanel.getObject().getTool() == null) {
            LOG.error("Please select a recommender from the list");
            error("Please select a recommender from the list");
            aTarget.addChildren(getPage(), IFeedback.class);
            return null;
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

        RecommendationEngineFactory factory = recommenderRegistry
                .getFactory(selectedRecommenderPanel.getObject().getTool());
        RecommendationEngine recommender = factory.build(selectedRecommenderPanel.getObject());
        
        if (recommender == null)
        {
            LOG.warn("Unknown Recommender selected");
            warn("Unknown Recommender selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return null;
        }
        
        StringBuilder sb = new StringBuilder();

        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        int iterations = 0;
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
            iterations++;
        }

        String data = sb.toString();
        //String recommenderName = selectedRecommenderPanel.getObject().getName();

        return data;
    }
}
