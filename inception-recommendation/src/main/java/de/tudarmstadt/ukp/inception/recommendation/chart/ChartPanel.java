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
package de.tudarmstadt.ukp.inception.recommendation.chart;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningCurve;

public class ChartPanel
    extends Panel
{
    private static final long serialVersionUID = -3849226240909011148L;

    private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

    private static final String MID_CHART_CONTAINER = "chart";
    private static final int MAX_POINTS_TO_PLOT = 50;

    private StringBuilder dataColumns;
    private StringBuilder chartType;
    private String javascript;
    private IModel<LearningCurve> model;
    private final WebComponent chart;

    public ChartPanel(String aId, IModel<LearningCurve> aModel)
    {
        super(aId);
        model = (aModel);
        dataColumns = new StringBuilder();
        chartType = new StringBuilder();

        chart = new Label(MID_CHART_CONTAINER);
        chart.setOutputMarkupId(true);
        add(chart);
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

    @SuppressWarnings("unchecked")
    public IModel<LearningCurve> getModel()
    {
        return (IModel<LearningCurve>) getDefaultModel();
    }

    /**
     * renders the chart and plot learning curve
     * 
     */
    public void renderChart(LearningCurve aLearningCurve)
    {
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        
        try {
            // there can be multiple learning curves. iterate over them to create data columns for
            // all
            for (String data : aLearningCurve.getCurveData().keySet()) {
                addLearningCurve(aLearningCurve.getCurveData().get(data), data);
            }

            createJSScript();

            LOG.debug("Rendering Recommender Evaluation Chart: {}", javascript);

            target.get().prependJavaScript(javascript);
        }
        catch (IOException e) {
            LOG.error("Unable to render chart", e);
            error("Unable to render chart: " + e.getMessage());
            target.get().addChildren(getPage(), IFeedback.class);
        }
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        LearningCurve chartdata = getModel().getObject();

        resetChart();

        renderChart(chartdata);
    }

    /**
     * to create a learning curve, it creates/updates the strings dataColumns and chartTypes for the
     * given recommender by appending data string. The type of the chart is set to be step. Calling
     * this method iteratively will generate multiple learning curves
     * 
     * @param aData
     *            a string that looks something like 2,5,3,7,8,4,
     * @param aName
     *            name of the learning curve
     */
    public void addLearningCurve(String aData, String aName)
    {
        // define chart type for the recommender
        chartType.append("'");
        chartType.append(aName);
        chartType.append("': 'step', ");

        // append recommender name to the data
        dataColumns.append("['");
        dataColumns.append(aName);

        // append data columns
        dataColumns.append("', ");
        dataColumns.append(aData);
        dataColumns.append("]");
        dataColumns.append(",");
    }

    public void resetChart()
    {
        chartType = new StringBuilder();
        dataColumns = new StringBuilder();
    }

    /**
     * Creates the JS script to render graph with the help of given data points. Also creates an
     * x-axis of a sequence from 0 to maximumNumberOfPoints (50). Example value of aDataColumns:
     * 
     * <pre>
     * ['recommender1', 1.0, 2.0, 3.0 ], ['recommender2', 2.0, 3.0, 4.0]
     * </pre>
     * 
     * Example value of aChartType
     * 
     * <pre>
     * recommender1: 'step', recommender2 : 'step'
     * </pre>
     */
    public void createJSScript() throws IOException
    {
        int[] intArray = IntStream.range(0, MAX_POINTS_TO_PLOT).map(i -> i).toArray();
        String xaxisValues = "[ 'x' ," + substring(Arrays.toString(intArray), 1, -1) + "]";
        String data = toJsonString(dataColumns).substring(1, dataColumns.toString().length());

        // bind data to chart container
        javascript = "var chart=c3.generate({bindto:'#" + chart.getMarkupId()
                + "',data:{ x:'x', columns:[" + xaxisValues + " ," + data + "],types:{" + chartType
                + "}},"
                + "axis: { y : { tick : { format: function(d){return Math.round(d * 10000) / 10000}}}}});;";
    }
}
