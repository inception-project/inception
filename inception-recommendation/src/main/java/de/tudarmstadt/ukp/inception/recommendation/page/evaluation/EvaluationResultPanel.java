/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.page.evaluation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.DefaultRefreshingView;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.LabelResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.Reporter;
import de.tudarmstadt.ukp.inception.recommendation.page.evaluation.model.EvaluationResultProvider;

public class EvaluationResultPanel
    extends Panel
{

    private final static Logger LOG = LoggerFactory.getLogger(EvaluationResultPanel.class);

    private static final long serialVersionUID = 7151761976918606160L;

    public EvaluationResultPanel(String id, IModel<List<ExtendedResult>> model)
    {
        super(id, model);
        setOutputMarkupId(true);

        DataView<ExtendedResult> data = new DataView<ExtendedResult>("evaluationDataRow",
                new EvaluationResultProvider(model.getObject()))
        {
            private static final long serialVersionUID = 8466632188375911927L;

            @Override
            protected void populateItem(final Item<ExtendedResult> item)
            {
                ExtendedResult result = item.getModelObject();

                item.add(new Label("iteration", result.getIterationNumber()));
                item.add(new Label("trainingSetSize", result.getTrainingSetSize()));
                item.add(new Label("fScore", result.getFscore()));
                item.add(new Label("precision", result.getPrecision()));
                item.add(new Label("recall", result.getRecall()));
                item.add(new Label("trainingDuration", result.getTrainingDuration()));
                item.add(new Label("classifyingDuration", result.getClassifyingDuration()));

            }
        };

        add(data);
        add(new Label("svg", LambdaModel.of(() -> Reporter.plotToString(model.getObject())))
                .setEscapeModelStrings(false));
        // add(new Image("graphImage", new GraphResource(model.getObject())));

        int size = model.getObject().size();
        final LabelResult labelResult = model.getObject().get(size - 1).getLabelResult();
        add(new ConfusionMatrix("confusionMatrix", Model.of(labelResult))).setVisible(size != 0);
        add(new LabelTable("labelTable", labelResult, model)).setVisible(size != 0);
    }

    public class LabelTable
        extends WebMarkupContainer
    {
        private static final long serialVersionUID = 1276731299896301305L;

        private RefreshingView<String> rows;

        public LabelTable(String id, LabelResult labelResult, IModel<List<ExtendedResult>> model)
        {
            super(id);
            setOutputMarkupId(true);

            int size = model.getObject().size();
            Map<String, Double> fScore, precision, recall;

            if (size != 0) {
                fScore = labelResult.getFScore();
                precision = labelResult.getPrecision();
                recall = labelResult.getRecall();
            }
            else {
                LOG.error("Empty results.");
                fScore = null;
                precision = null;
                recall = null;
            }

            final IModel<List<String>> scoreAdapter = new ListModel<>(
                    Arrays.asList(null, "F-Score", "Precision", "Recall"));

            List<String> labels = labelResult.getLabels();
            labels.add(0, null);

            rows = new DefaultRefreshingView<String>("rows",
                    LambdaModel.of(() -> labelResult != null ? labels : new String[] {}))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    setVisible(labelResult != null);
                }

                @Override
                protected void populateItem(final Item<String> aRowItem)
                {
                    // Render regular row
                    aRowItem.add(new DefaultRefreshingView<String>("cells", scoreAdapter)
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(Item<String> aCellItem)
                        {
                            aCellItem.setRenderBodyOnly(true);

                            Fragment cell;
                            // Content cell
                            if (aRowItem.getIndex() == 0 || aCellItem.getIndex() == 0) {
                                cell = new Fragment("cell", "th", LabelTable.this);
                            }
                            // Header cell
                            else {
                                cell = new Fragment("cell", "td", LabelTable.this);
                            }

                            // Top-left cell
                            if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                                cell.add(new Label("label", Model.of("Label")));
                            }
                            // Raters header horizontally
                            else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                                cell.add(new Label("label", Model.of(aCellItem.getModelObject())));
                            }
                            // Raters header vertically
                            else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                                cell.add(new Label("label", Model.of(aRowItem.getModelObject())));
                            }

                            // F-Score
                            else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 1) {
                                cell.add(new Label("label",
                                        Model.of(fScore.get(labels.get(aRowItem.getIndex() - 1)))));
                            }
                            // Precision
                            else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 2) {
                                cell.add(new Label("label", Model
                                        .of(precision.get(labels.get(aRowItem.getIndex() - 1)))));
                            }
                            // Recall
                            else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 3) {
                                cell.add(new Label("label",
                                        Model.of(recall.get(labels.get(aRowItem.getIndex() - 1)))));
                            }

                            // no data
                            else {
                                cell.add(new Label("label", Model.of("-")));
                            }

                            aCellItem.add(cell);
                        }
                    });
                    // Odd/even coloring is reversed here to account for the header row at index 0
                    aRowItem.add(new AttributeAppender("class",
                            (aRowItem.getIndex() % 2 == 0) ? "odd" : "even"));
                }

            };
            add(rows);
        }
    }

    @SuppressWarnings("serial")
    private static class ConfusionMatrix
        extends WebMarkupContainer
    {
        private RefreshingView<String> rows;

        public ConfusionMatrix(String aId, IModel<LabelResult> aModel)
        {
            super(aId, aModel);

            LabelResult labelResult = aModel.getObject();
            List<String> labels = labelResult.getLabels();
            // labels.add(0, "");

            rows = new DefaultRefreshingView<String>("rows",
                    LambdaModel.of(() -> labelResult != null ? labels : new String[] {}))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    setVisible(aModel.getObject() != null);
                }

                @Override
                protected void populateItem(final Item<String> aRowItem)
                {
                    // Render regular row
                    aRowItem.add(new DefaultRefreshingView<String>("cells", Model.of(labels))
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(Item<String> aCellItem)
                        {
                            aCellItem.setRenderBodyOnly(true);

                            Fragment cell;

                            // Content cell
                            if (aRowItem.getIndex() == 0 || aCellItem.getIndex() == 0) {
                                cell = new Fragment("cell", "th", ConfusionMatrix.this);
                            }
                            // Header cell
                            else {
                                cell = new Fragment("cell", "td", ConfusionMatrix.this);
                            }

                            WebMarkupContainer cellContent = new WebMarkupContainer("item");
                            cell.add(cellContent);

                            // Top-left cell
                            if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                                cellContent.add(new MultiLineLabel("label", Model
                                        .of("Expected\r\n" + "-------------- \r\n" + "Generated")));
                            }
                            // Expected labels
                            else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                                
                                int sum = 0;
                                for (int[] i : labelResult.getCounts()) {
                                    sum += i[aCellItem.getIndex() - 1];
                                }
                                
                                String tooltipContent = Integer.toString(sum);
                                DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
                                        "Sum of Expected", tooltipContent);
                                tooltip.setOption("position", (Object) null);

                                Label l = new Label("label", Model.of(aCellItem.getModelObject()));
                                l.add(tooltip);
                                cellContent.add(l);
                                
                            }
                            // Generated labels
                            else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                                int sum = 0;
                                for (int i : labelResult.getCounts()[aRowItem.getIndex() - 1]) {
                                    sum += i;
                                }
                                String tooltipContent = Integer.toString(sum);
                                DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
                                        "Sum of Generated", tooltipContent);
                                tooltip.setOption("position", (Object) null);

                                Label l = new Label("label", Model.of(aRowItem.getModelObject()));
                                l.add(tooltip);
                                cellContent.add(l);
                            }

                            // if there is data available
                            else if (labelResult.getCounts() != null) {
                                double content = 
                                    (double) labelResult.getCounts()
                                                [aRowItem.getIndex() - 1][aCellItem.getIndex() - 1] 
                                            / labelResult.getSummedGeneratedforLabel(
                                                        aCellItem.getIndex() - 1);

                                String tooltipContent = Integer
                                        .toString(labelResult.getCounts()[aRowItem.getIndex()
                                                - 1][aCellItem.getIndex() - 1]);
                                DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
                                        "Absolute value", tooltipContent);
                                tooltip.setOption("position", (Object) null);

                                Label l = new Label("label", Model.of(content));
                                l.add(tooltip);

                                cellContent.add(l);
                                String color = getColor(content,
                                        (aRowItem.getIndex() == aCellItem.getIndex()));

                                cellContent
                                        .add(new AttributeModifier("style", LambdaModel.of(() -> {
                                            return String.format("background-color: %s", color);
                                        })));
                            }

                            // no data
                            else {
                                cellContent.add(new Label("label", Model.of("-")));
                            }

                            aCellItem.add(cell);
                        }
                    });
                    // Odd/even coloring is reversed here to account for the header row at index 0
                    aRowItem.add(new AttributeAppender("class", "even"));
                }

            };
            add(rows);
        }

        public String getColor(double blending, boolean isDiagonal)
        {
            if (isDiagonal) {
                blending = 1 - blending;
            }

            int r_baseline = 220;
            int g_baseline = 220;
            int b_baseline = 220;

            int r_target = 216;
            int g_target = 43;
            int b_target = 34;

            int red = (int) ((r_target * blending) + (r_baseline * (1 - blending)));
            int green = (int) ((g_target * blending) + (g_baseline * (1 - blending)));
            int blue = (int) ((b_target * blending) + (b_baseline * (1 - blending)));

            String blendedColor = String.format("#%02x%02x%02x", red, green, blue);
            return blendedColor;
        }
    }
}
