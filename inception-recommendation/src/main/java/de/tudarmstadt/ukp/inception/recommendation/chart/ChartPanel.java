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
package de.tudarmstadt.ukp.inception.recommendation.chart;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;

import java.io.IOException;
import java.util.ArrayList;
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum;

public class ChartPanel
    extends Panel
{
    private static final long serialVersionUID = -3849226240909011148L;

    private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

    private static final String MID_CHART_CONTAINER = "chart";

    private final IModel<RecommenderEvaluationScoreMetricEnum> metric;
    private final WebMarkupContainer chart;
    private final ChartAjaxBejavior chartAjaxBejavior;

    public ChartPanel(String aId, IModel<List<EvaluationResult>> aModel,
            IModel<RecommenderEvaluationScoreMetricEnum> aMetric)
    {
        super(aId, aModel);

        metric = aMetric;

        chart = new WebMarkupContainer(MID_CHART_CONTAINER);
        add(chart);

        chartAjaxBejavior = new ChartAjaxBejavior();
        add(chartAjaxBejavior);
    }

    @SuppressWarnings("unchecked")
    public List<EvaluationResult> getModelObject()
    {
        return (List<EvaluationResult>) getDefaultModelObject();
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

        String chartTriggerJavascript = String.join("\n", //
                "$(document).ready(function(){", //
                "   $.ajax({", //
                "       url:'" + chartAjaxBejavior.getCallbackUrl().toString() + "',", //
                "       type:'post',", //
                "       contentType:'application/json',", //
                "       dataType:'json',", //
                "       success : function(result){", //
                "           updateLearningCurveDiagram('#" + chart.getMarkupId() + "', result)", //
                "       }", //
                "   })", //
                "})");

        aResponse.render(JavaScriptContentHeaderItem.forScript(chartTriggerJavascript, null));
    }

    private List<Datapoint> convert(List<EvaluationResult> aEvaluationResults)
    {
        List<Datapoint> data = new ArrayList<>();
        int run = 0;
        for (EvaluationResult evaluationResult : aEvaluationResults) {
            run++;

            if (evaluationResult.isEvaluationSkipped()) {
                continue;
            }

            double score;
            switch (metric.getObject()) {
            case Accuracy:
                score = evaluationResult.computeAccuracyScore();
                break;
            case Precision:
                score = evaluationResult.computePrecisionScore();
                break;
            case Recall:
                score = evaluationResult.computeRecallScore();
                break;
            case F1:
                score = evaluationResult.computeF1Score();
                break;
            default:
                score = evaluationResult.computeAccuracyScore();
            }

            data.add(new Datapoint(run, evaluationResult.getTrainingSetSize(),
                    evaluationResult.getTrainDataRatio(), score));
        }

        return data;
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
    private final static class Datapoint
    {
        final int run;
        final int trainSize;
        final double trainRatio;
        final double score;

        public Datapoint(int aRun, int aTrainSize, double aTrainRatio, double aScore)
        {
            run = aRun;
            trainSize = aTrainSize;
            trainRatio = aTrainRatio;
            score = aScore;
        }
    }

    private final class ChartAjaxBejavior
        extends AbstractAjaxBehavior
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void onRequest()
        {
            try {
                String json = toInterpretableJsonString(convert(getModelObject()));
                RequestCycle.get().scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", json));
            }
            catch (IOException e) {
                LOG.error(e.toString(), e);
            }
        }
    }
}
