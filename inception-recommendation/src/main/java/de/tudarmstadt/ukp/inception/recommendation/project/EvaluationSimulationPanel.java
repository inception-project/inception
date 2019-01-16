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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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

public class EvaluationSimulationPanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String CHART_CONTAINER = "chart-container";
    private static final String SIMULATION_START_BUTTON = "simulation-start-button";
    
    private static final double TRAIN_PERCENTAGE = 0.8;
    private static final int INCREMENT = 250;
    private static final int LOW_SAMPLE_THRESHOLD = 10;
    
    private static final String FORM = "form";

    private static final Logger log = LoggerFactory.getLogger(EvaluationSimulationPanel.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userDao;
    
    private final WebComponent chartContainer;
    private final Project project;

    public EvaluationSimulationPanel(String aId, Project aProject,
            RecommenderEditorPanel aRecommenderEditorPanel)
    {
        super(aId);
        project = aProject;

        Form<Recommender> form = new Form<>(FORM);
        add(form);

        chartContainer = new Label(CHART_CONTAINER);
        chartContainer.setOutputMarkupId(true);
        form.add(chartContainer);

        form.add(new AjaxButton(SIMULATION_START_BUTTON)
        {
            private static final long serialVersionUID = -3902555252753037183L;

            @Override
            protected void onError(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);
            }

            @Override
            protected void onAfterSubmit(AjaxRequestTarget aTarget)
            {
                //get the selected recommender
                IModel<Recommender> recommenderModel = aRecommenderEditorPanel
                        .getRecommenderModel();
                
                //there must be some recommender selected by the user on the UI
                if (recommenderModel.getObject() == null
                        || recommenderModel.getObject().getTool() == null) {
                    log.error("Please select a recommender from the list");
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
                        log.error("Unable to render chart", e1);
                        error("Unable to render chart: " + e1.getMessage());
                        aTarget.addChildren(getPage(), IFeedback.class);
                        return;
                    }
                });
                
                IncrementalSplitter splitStrategy = new IncrementalSplitter(TRAIN_PERCENTAGE,
                        INCREMENT, LOW_SAMPLE_THRESHOLD);

                RecommendationEngine recommender = getRecommendationEngine(recommenderModel);
                if (recommender == null)
                {
                    log.warn("Unknown Recommender selected");
                    warn("Unknown Recommender selected");
                    aTarget.addChildren(getPage(), IFeedback.class);
                    return;
                }
                
                StringBuilder dataColumns = new StringBuilder();
                StringBuilder chartType = new StringBuilder();
                StringBuilder sb = new StringBuilder();
                StringBuilder xaxis = new StringBuilder();

                // create a list of comma separated string of scores from every iteration of
                // evaluation.
                int i = 0;
                while (splitStrategy.hasNext()) {
                    splitStrategy.next();

                    double score;
                    try {
                        score = recommender.evaluate(casList, splitStrategy);
                    }
                    catch (RecommendationException e) {
                        log.error(e.toString(),e);
                        continue;
                    }

                    xaxis.append(i + ",");
                    sb.append(score + ",");
                    i++;
                }

                String data = sb.toString();

                // append recommender name to the data
                dataColumns.append("['");
                String recommenderName = recommenderModel.getObject().getName();

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

                try {
                    String javascript = createJSScript(dataColumns.toString(),
                            chartType.toString(), xaxis.toString());
                    log.debug("Rendering Recommender Evaluation Chart: {}", javascript);

                    aTarget.prependJavaScript(javascript);
                }
                catch (IOException e) {
                    log.error("Unable to render chart", e);
                    error("Unable to render chart: " + e.getMessage());
                    aTarget.addChildren(getPage(), IFeedback.class);
                } 
            }
        });
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
    private String createJSScript(String aDataColumns, String aChartType, String aXaxis)
        throws IOException
    {
        String xaxisValues = "[ 'x' ," + aXaxis + "]";
        String data = toJsonString(aDataColumns).substring(1, aDataColumns.toString().length());

        // bind data to chart container
        String javascript = "var chart=c3.generate({bindto:'#" + chartContainer.getMarkupId()
                + "',data:{ x:'x', columns:[" + xaxisValues + " ," + data + "],types:{" + aChartType
                + "}},axis: { y : { tick : { format: function(d){return Math.round(d * 10000) / 10000}}}}});;";
        return javascript;
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
