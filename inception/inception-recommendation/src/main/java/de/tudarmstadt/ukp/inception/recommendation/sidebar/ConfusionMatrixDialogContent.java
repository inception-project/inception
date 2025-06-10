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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.util.stream.Stream;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.ConfusionMatrix;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.DefaultRefreshingView;

public class ConfusionMatrixDialogContent
    extends Panel
{
    private static final long serialVersionUID = 6002385711741527179L;

    private RefreshingView<String> rows;

    public ConfusionMatrixDialogContent(String aId, IModel<EvaluationResult> aModel)
    {
        super(aId, aModel);

        var confusionMatrixData = aModel.map(EvaluationResult::getConfusionMatrix);

        // Prepend a dummy element for the header columns into the axes
        var axes = confusionMatrixData.map(ConfusionMatrix::getLabels)
                .map(label -> concat(Stream.of(""), label.stream()).collect(toList()));

        queue(new Label("totalDatapoints", confusionMatrixData.map(ConfusionMatrix::getTotal)));
        queue(new Label("datapointUnit", confusionMatrixData.map(ConfusionMatrix::getUnit)));
        queue(new Label("ignoredLabels", aModel.map(EvaluationResult::getIgnoreLabels)
                .map(labels -> labels.stream().collect(joining(", ")))));
        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));

        rows = new DefaultRefreshingView<String>("rows", axes)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<String> aRowItem)
            {
                // Render regular row
                aRowItem.add(new DefaultRefreshingView<String>("cells", axes)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<String> aCellItem)
                    {
                        aCellItem.setRenderBodyOnly(true);

                        Fragment cell;
                        // Header cell
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell = new Fragment("cell", "th-column",
                                    ConfusionMatrixDialogContent.this);
                        }
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-row",
                                    ConfusionMatrixDialogContent.this);
                        }
                        else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th", ConfusionMatrixDialogContent.this);
                        }
                        // Content cell
                        else {
                            cell = new Fragment("cell", "td", ConfusionMatrixDialogContent.this);
                        }

                        // Top-left cell
                        // Horizontal headers
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell.add(new Label("label", aCellItem.getModel()));
                        }
                        // Vertical headers
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", aRowItem.getModel()));
                        }
                        else {
                            renderCell(confusionMatrixData, aRowItem, aCellItem, cell);
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

    private void renderCell(IModel<ConfusionMatrix> aModel, Item<String> aRowItem,
            Item<String> aCellItem, Fragment aCell)
    {
        var predicted = aCellItem.getModelObject();
        var gold = aRowItem.getModelObject();
        var count = aModel.map(m -> m.getEntryCount(predicted, gold));

        var cellContent = new WebMarkupContainer("cellContent");
        if (aCellItem.getIndex() == aRowItem.getIndex()) {
            if (!getModel().map(EvaluationResult::getIgnoreLabels)
                    .map(ignLabels -> ignLabels.contains(gold)).getObject()) {
                cellContent.add(AttributeAppender.append("class", "text-bg-success"));
            }
        }
        else if (count.map(v -> v > 0).getObject()) {
            cellContent.add(AttributeAppender.append("class", "text-bg-danger"));
        }
        aCell.queue(cellContent);

        var label = new Label("label", count);
        label.add(AttributeModifier.append("class", getModel() //
                .map(EvaluationResult::getIgnoreLabels) //
                .map(ignLabels -> ignLabels.contains(gold) && predicted.equals(gold)) //
                .map(ignored -> ignored ? "text-muted" : "")));
        aCell.queue(label);
    }

    @SuppressWarnings("unchecked")
    public IModel<EvaluationResult> getModel()
    {
        return (IModel<EvaluationResult>) getDefaultModel();
    }

    public EvaluationResult getModelObject()
    {
        return (EvaluationResult) getDefaultModelObject();
    }

    protected void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
