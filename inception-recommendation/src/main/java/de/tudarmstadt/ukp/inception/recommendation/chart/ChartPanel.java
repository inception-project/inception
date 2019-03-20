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

import java.io.IOException;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
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

    private StringBuilder dataColumns;
    private StringBuilder chartType;
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

        model = getModel();

        if (model == null)
            return;

        resetChart();

        LearningCurve learningCurve = model.getObject();

        // there can be multiple learning curves. iterate over them to create data
        // columns for all
        for (String data : learningCurve.getCurveData().keySet()) {
            addLearningCurve(learningCurve.getCurveData().get(data), data);
        }

        String javascript = createJSScript(learningCurve.getXaxis());

        if (javascript == null || javascript.isEmpty()) {
            LOG.warn("No javascript to render the learning curve diagram.");
            return;
        }

        LOG.debug("Rendering Recommender Evaluation Chart: {}", javascript);
        aResponse.render(OnDomReadyHeaderItem.forScript(javascript));
    }

    @SuppressWarnings("unchecked")
    public IModel<LearningCurve> getModel()
    {
        return (IModel<LearningCurve>) getDefaultModel();
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
    public String createJSScript(String xaxis)
    {
        try {
            String xaxisValues = "[ 'x' ," + xaxis + "]";
            String data = toJsonString(dataColumns).substring(1, dataColumns.toString().length());

            // bind data to chart container
            String javascript = "var chart=c3.generate({bindto:'#" + chart.getMarkupId()
                    + "',data:{ x:'x', columns:[" + xaxisValues + " ," + data + "],types:{"
                    + chartType + "}},"
                    + "axis: { x:{type: 'category',tick: {rotate: 0,multiline: true}}, y : { tick : { format: function(d){return Math.round(d * 10000) / 10000}}}}});;";
            return javascript;
        }
        catch (IOException e) {
            LOG.error("Could not create the dataColumns. Bad Value. Javascript creation failed");
            return null;
        }
    }
}
