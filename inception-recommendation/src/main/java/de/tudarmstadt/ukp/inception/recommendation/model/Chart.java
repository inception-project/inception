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
package de.tudarmstadt.ukp.inception.recommendation.model;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;
import static org.apache.commons.lang3.StringUtils.substring;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chart
{
    private static final Logger LOG  = LoggerFactory.getLogger(Chart.class);
    
    private StringBuilder dataColumns;
    private StringBuilder chartType;
    private String javascript;
    private int maximumPointsToPlot;
    private String markupId;
    
    public Chart(String aMarkupId, int aMaximumPointsToPlot)
    {
        dataColumns = new StringBuilder();
        chartType = new StringBuilder();
        markupId = aMarkupId;
        maximumPointsToPlot = aMaximumPointsToPlot;
    }

    /**
     * to create a learning curve, it creates/updates the strings dataColumns and chartTypes for the
     * given recommender by appending data string. The type of the chart is set to be step. Calling
     * this method iteratively will generate multiple learning curves
     * 
     * @param data a string that looks something like 2,5,3,7,8,4,
     * @param name name of the learning curve
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
    public void createJSScript() throws IOException
    {
        int[] intArray = IntStream.range(0, maximumPointsToPlot).map(i -> i).toArray();
        String xaxisValues = "[ 'x' ," + substring(Arrays.toString(intArray), 1, -1) + "]";
        String data = toJsonString(dataColumns).substring(1,
                dataColumns.toString().length());
    
        // bind data to chart container
        javascript = "var chart=c3.generate({bindto:'#" 
                + markupId
                + "',data:{ x:'x', columns:[" 
                + xaxisValues + " ," 
                + data + "],types:{"
                + chartType
                + "}},"
                + "axis: { y : { tick : { format: function(d){return Math.round(d * 10000) / 10000}}}}});;";
    }
    
    /**
     * Renders the chart using the given request handler 
     * 
     * @param aPanel
     * @param aRequestHandler
     */
    public void renderChart(Panel aPanel, IPartialPageRequestHandler aRequestHandler)
    {
        try {
            createJSScript();

            LOG.debug("Rendering Recommender Evaluation Chart: {}", javascript);

            aRequestHandler.prependJavaScript(javascript);
        }
        catch (IOException e) {
            LOG.error("Unable to render chart", e);
            aPanel.error("Unable to render chart: " + e.getMessage());
            aRequestHandler.addChildren(aPanel.getPage(), IFeedback.class);
        }
    }
}
