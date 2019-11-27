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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.inception.recommendation.chart.resources.ChartJsReference;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningCurve;

public class ChartPanel
    extends Panel
{
    private static final long serialVersionUID = -3849226240909011148L;

    private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

    private static final String MID_CHART_CONTAINER = "chart";
    private static final String OUTPUT_MARKUP_ID_CHART = "canvas";

    private final WebMarkupContainer chart;
    private final ChartAjaxBejavior chartAjaxBejavior;

    public ChartPanel(String aId, IModel<LearningCurve> aModel)
    {
        super(aId, aModel);

        chart = new WebMarkupContainer(MID_CHART_CONTAINER);
        chart.setMarkupId(OUTPUT_MARKUP_ID_CHART);
        add(chart);

        chartAjaxBejavior = new ChartAjaxBejavior();
        add(chartAjaxBejavior);
    }
    
    public LearningCurve getModelObject()
    {
        return (LearningCurve) getDefaultModelObject();
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

        aResponse.render(JavaScriptReferenceHeaderItem.forReference(
                getApplication().getJavaScriptLibrarySettings().getJQueryReference()));

        aResponse.render(JavaScriptHeaderItem.forReference(ChartJsReference.get()));
        
        String chartTriggerJavascript = String.join("\n",
                "$(document).ready(function(){", 
                "   $.ajax({",
                "       url:'" + chartAjaxBejavior.getCallbackUrl().toString() + "',",
                "       type:'post',",
                "       contentType:'application/json',",
                "       dataType:'json',",
                "       success : function(result){",
                "           updateLearningCurveDiagram(result)",
                "       }",
                "   })",
                "})");

        aResponse.render(JavaScriptContentHeaderItem.forScript(chartTriggerJavascript, null));
    }

    private final class ChartAjaxBejavior
        extends AbstractAjaxBehavior
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void onRequest()
        {
            RequestCycle requestCycle = RequestCycle.get();

            LearningCurve learningCurve = getModelObject();

            try {
                String json = addLearningCurve(learningCurve);

                // return the chart data back to the UI with the JSON. JSON define te learning
                // curves and the xaxis
                requestCycle.scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", json));
            }
            catch (JsonProcessingException e) {
                LOG.error(e.toString(), e);
            }
        }
    }

    /**
     * creates a JSON of learning curves of learning Curves including the xaxis.
     * 
     * @throws JsonProcessingException
     */
    public String addLearningCurve(LearningCurve aLearningCurve) throws JsonProcessingException
    {
        List<List<String>> lines = new ArrayList<>();

        // add xaxis to the list of lines
        List<String> asList = new ArrayList<>();
        asList.add("x");

        // only add the x-axis if it is present (C3 Javascript Library will handle the "no-data" )
        if (aLearningCurve != null && aLearningCurve.getXaxis() != null
                && !aLearningCurve.getXaxis().isEmpty()) {
            asList.addAll(Arrays.asList(aLearningCurve.getXaxis().split(",")));
        }
        lines.add(asList);

        // only add the curve data if it is present. (C3 Javascript Library will handle the
        // "no-data" )
        if (aLearningCurve != null && aLearningCurve.getCurveData() != null
                && !aLearningCurve.getCurveData().isEmpty()) {
            // there can be multiple learning curves. add them to te list of lines
            for (String data : aLearningCurve.getCurveData().keySet()) {
                List<String> newLine = new ArrayList<String>();
                newLine.add(data);
                newLine.addAll(Arrays.asList(aLearningCurve.getCurveData().get(data).split(",")));
                lines.add(newLine);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(lines);

        return json;
    }
}
