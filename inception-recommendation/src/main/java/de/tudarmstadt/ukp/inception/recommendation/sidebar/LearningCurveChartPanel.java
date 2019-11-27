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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.chart.ChartPanel;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderEvaluationResultEventAdapter.Details;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningCurve;
import de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum;

public class LearningCurveChartPanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String MID_CHART_CONTAINER = "chart-container";
    private static final String MID_DROPDOWN_PANEL = "dropdownPanel";

    private static final int MAX_POINTS_TO_PLOT = 50;
    private static final Logger LOG = LoggerFactory.getLogger(LearningCurveChartPanel.class);
    
    private @SpringBean EventRepository eventRepo;
    private @SpringBean RecommendationService recommendationService;
    
    private final ChartPanel chartPanel;
    private final IModel<AnnotatorState> model;
    public RecommenderEvaluationScoreMetricEnum selectedMetric;

    public LearningCurveChartPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);
        model = aModel;

        setOutputMarkupId(true);
        
        //initially the chart is empty. passing empty model
        chartPanel = new ChartPanel(MID_CHART_CONTAINER,
                LoadableDetachableModel.of(this::renderChart));
        // chartPanel.add(visibleWhen(() -> chartPanel.getModelObject() != null));
        
        chartPanel.setOutputMarkupId(true);
        add(chartPanel);
        
        final Panel dropDownPanel = new MetricSelectDropDownPanel(MID_DROPDOWN_PANEL);
        dropDownPanel.setOutputMarkupId(true);
        add(dropDownPanel);
        
        selectedMetric = RecommenderEvaluationScoreMetricEnum.Accuracy;
    }
    

    @Override
    public void onEvent(IEvent<?> event)
    {
        super.onEvent(event);
        if (event.getPayload() instanceof DropDownEvent) {
            DropDownEvent dEvent = (DropDownEvent) event.getPayload();
            
            RecommenderEvaluationScoreMetricEnum aSelectedMetric = dEvent.getSelectedValue();
            AjaxRequestTarget target = dEvent.getTarget();
            
            target.add(this);

            selectedMetric = aSelectedMetric;
            LOG.debug("Option selected: " + aSelectedMetric);
            
            event.stop();
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        LOG.trace("rendered annotation event");

        aEvent.getRequestHandler().add(this);
    }
    
    /**
     * Returns chart data wrapped in LearningCurve
     */
    private LearningCurve renderChart()
    {
        LOG.debug("SELECTED METRIC IS " + selectedMetric);
        MultiValuedMap<String, Double> recommenderScoreMap = getLatestScores();

        if (CollectionUtils.isEmpty(recommenderScoreMap.keys())) {
            LOG.debug("Cannot plot the learning curve because there are no scores. Project: {}",
                    model.getObject().getProject());
            return null;
        }

        Map<String,String> curveData = new HashMap<String,String>();
        LearningCurve learningCurve = new LearningCurve();

        // iterate over recommenderScoreMap to create data
        for (String recommenderName : recommenderScoreMap.keySet()) {
            // extract the scores from the recommenderScoreMap. The format of data is a comma
            // separated string of scores(each score is Double cast-able) to be. 
            // Example 2.3, 4.5 ,6, 5, 3, 9,
            String data = recommenderScoreMap.get(recommenderName).stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
            
            curveData.put(recommenderName,data);
            
            learningCurve.setCurveData(curveData);
            
            // the Curve is not allowed to have more points as compared to MAX_POINTS_TO_PLOT. This
            // is how many scores we have retrieved from the database
            int[] intArray = IntStream.range(0, MAX_POINTS_TO_PLOT).map(i -> i).toArray();
            String xaxisValues =  substring(Arrays.toString(intArray), 1, -1)  ;
            
            learningCurve.setXaxis(xaxisValues);
        }

        return learningCurve;
    }

    /**
     * Fetches a number of latest evaluation scores from the database and save it in the map
     * corresponding to each recommender for which the scores have been logged in the database
     * 
     * @return
     */
    private MultiValuedMap<String, Double> getLatestScores()
    {
        // we want to plot RecommenderEvaluationResultEvent for the learning curve. The
        // value of the event
        String eventType = "RecommenderEvaluationResultEvent";

        List<LoggedEvent> loggedEvents = new ArrayList<LoggedEvent>();
        
        List<Recommender> listEnabledRecommenders = recommendationService
                .listEnabledRecommenders(model.getObject().getProject());
    
        if (listEnabledRecommenders.isEmpty()) {
            LOG.warn("The project has no enabled recommender");
        }
        
        for (Recommender recommender : listEnabledRecommenders) {
            List<LoggedEvent> tempLoggedEvents = eventRepo.listLoggedEventsForRecommender(
                    model.getObject().getProject(), model.getObject().getUser().getUsername(),
                    eventType, MAX_POINTS_TO_PLOT, recommender.getId());
            
            // we want to show the latest record on the right side of the graph
            Collections.reverse(tempLoggedEvents);
            
            loggedEvents.addAll(tempLoggedEvents);
        }
                
        if (CollectionUtils.isEmpty(loggedEvents)) {
            return new ArrayListValuedHashMap<String, Double>();
        }

        MultiValuedMap<String, Double> recommenderScoreMap = new ArrayListValuedHashMap<>();

        // iterate over the logged events to extract the scores and map it against its corresponding
        // recommender.
        for (LoggedEvent loggedEvent : loggedEvents) {
            String detailJson = loggedEvent.getDetails();
            try {
                Details detail = fromJsonString(Details.class, detailJson);

                // If the log is inconsistent and we do not have an ID for some reason, then we
                // have to skip it
                if (detail.recommenderId == null) {
                    continue;
                }
                
                //do not include the scores from disabled recommenders
                Optional<Recommender> recommenderIfActive = recommendationService
                        .getEnabledRecommender(detail.recommenderId);
                if (!recommenderIfActive.isPresent()) {
                    continue;
                }

                // sometimes score values NaN. Can result into error while rendering the graph on UI
                double score;
                
                switch (selectedMetric ) {
                case Accuracy:
                    score = detail.accuracy;
                    break;
                case Precision:
                    score = detail.precision;
                    break;
                case Recall:
                    score = detail.recall;
                    break;
                case F1:
                    score = detail.f1;
                    break;
                default:
                    score = detail.accuracy;
                }
                
                if (!Double.isFinite(score)) {
                    continue;
                }
                
                //recommenderIfActive only has one member
                recommenderScoreMap.put(recommenderIfActive.get().getName(), score);
            }
            catch (IOException e) {
                LOG.error("Invalid logged Event detail. Skipping record with logged event id: "
                        + loggedEvent.getId(), e);
            }
        }
        return recommenderScoreMap;
    }
}

