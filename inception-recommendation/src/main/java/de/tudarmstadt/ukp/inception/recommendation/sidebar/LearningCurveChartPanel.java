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
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderEvaluationResultEventAdapter.Details;

public class LearningCurveChartPanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String CHART_CONTAINER = "chart-container";

    private static final Logger log = LoggerFactory.getLogger(LearningCurveChartPanel.class);
    private final WebComponent chartContainer;

    private @SpringBean EventRepository eventRepo;
    private @SpringBean RecommendationService recommendationService;

    private final IModel<AnnotatorState> model;
    private final int maxPointsToPlot = 50;

    public LearningCurveChartPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId);
        model = aModel;

        chartContainer = new Label(CHART_CONTAINER);
        chartContainer.setOutputMarkupId(true);
        add(chartContainer);
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

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        log.trace("rendered annotation event");

        MultiValuedMap<String, Double> recommenderScoreMap = getLatestScores(aEvent);

        if (CollectionUtils.isEmpty(recommenderScoreMap.keys())) {
            log.error("No evaluation data for the learning curve. Project: {}",
                    model.getObject().getProject());

            error("Cannot plot the learning curve. Please make some annotations");
            aEvent.getRequestHandler().addChildren(getPage(), IFeedback.class);

            return;
        }

        // iterate over recommenderScoreMap to create data arrays to feed to the c3 graph
        StringBuilder dataColumns = new StringBuilder();
        StringBuilder chartType = new StringBuilder();
        
        for (String key : recommenderScoreMap.keySet()) {
            String data = recommenderScoreMap.get(key).stream().map(Object::toString).collect(Collectors.joining(", "));
            
            // append recommender name to the data
            dataColumns.append("['");
            String[] recommenderClass = key.toString().split("\\.");
            String recommenderName = recommenderClass[recommenderClass.length - 1];

            // define chart type for the recommender
            chartType.append("'");
            chartType.append(recommenderName);
            chartType.append("': 'step', ");
            dataColumns.append(recommenderName);

            // append data columns
            dataColumns.append("', ");
            dataColumns.append(data);
            dataColumns.append("]");
            dataColumns.append(",");
        }
 
        try {
            String javascript = createJSScript(dataColumns.toString(), chartType.toString());
            log.debug("Rendering Recommender Evaluation Chart: {}", javascript);

            aEvent.getRequestHandler().prependJavaScript(javascript);
        }
        catch (IOException e) {
            log.error("Unable to render chart", e);
            error("Unable to render chart: " + e.getMessage());
            aEvent.getRequestHandler().addChildren(getPage(), IFeedback.class);
        }
    }

    /**
     * Creates the JS script to render graph with the help of given data points. Also creates an
     * x-axis of a sequence from 0 to maximumNumberOfPoints (50). Example value of
     * aDataColumns: 
     * <pre>
     * ['recommender1', 1.0, 2.0, 3.0 ], ['recommender2', 2.0, 3.0, 4.0]
     * </pre>
     * 
     * Example value of aChartType
     * <pre>
     * recommender1: 'step', recommender2 : 'step'
     * </pre>
     */
    private String createJSScript(String aDataColumns, String aChartType) throws IOException
    {
        int[] intArray = IntStream.range(0, maxPointsToPlot).map(i -> i).toArray();
        String xaxisValues = "[ 'x' ," + substring(Arrays.toString(intArray), 1, -1) + "]";
        String data = toJsonString(aDataColumns).substring(1, aDataColumns.toString().length());

        // bind data to chart container
        String javascript = "var chart=c3.generate({bindto:'#" + chartContainer.getMarkupId()
                + "',data:{ x:'x', columns:[" + xaxisValues + " ," + data + "],types:{" + aChartType
                + "}},axis: { y : { tick : { format: function(d){return Math.round(d * 10000) / 10000}}}}});;";
        return javascript;
    }

    /**
     * Fetches a number of latest evaluation scores from the database and save it in the map
     * corresponding to each recommender for which the scores have been logged in the database
     * 
     * @return
     */
    private MultiValuedMap<String, Double> getLatestScores(RenderAnnotationsEvent aEvent)
    {
        // we want to plot RecommenderEvaluationResultEvent for the learning curve. The
        // value of the event
        String eventType = "RecommenderEvaluationResultEvent";

        List<LoggedEvent> loggedEvents = new ArrayList<LoggedEvent>();
        
        List<Recommender> listEnabledRecommenders = recommendationService
                .listEnabledRecommenders(model.getObject().getProject());
        
        if (listEnabledRecommenders.isEmpty())        {
            log.warn("The project has no enabled recommender");

            error("Cannot plot the learning curve. There is not recommender in the project.");

            aEvent.getRequestHandler().addChildren(getPage(), IFeedback.class);
        }
        
        for (Recommender recommender : listEnabledRecommenders) {
            List<LoggedEvent> tempLoggedEvents = eventRepo.listLoggedEvents(
                    model.getObject().getProject(), model.getObject().getUser().getUsername(),
                    eventType, maxPointsToPlot, recommender.getId());
            
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

                //do not include the scores from disabled recommenders
                List<Recommender> recommenderIfActive = recommendationService
                        .getEnabledRecommenders(detail.recommenderId);
                if (recommenderIfActive.isEmpty()) {
                    continue;
                }

                // sometimes score values NaN. Can result into error while rendering the graph on UI
                if (!Double.isFinite(detail.score)) {
                    continue;
                }
                
                //recommenderIfActive only has one member
                recommenderScoreMap.put(recommenderIfActive.get(0).getName(), detail.score);
            }
            catch (IOException e) {
                log.error("Invalid logged Event detail. Skipping record with logged event id: "
                        + loggedEvent.getId(), e);

                error("Invalid logged Event detail. Skipping record with logged event id: "
                        + loggedEvent.getId());

                aEvent.getRequestHandler().addChildren(getPage(), IFeedback.class);
            }
        }
        return recommenderScoreMap;
    }
}
