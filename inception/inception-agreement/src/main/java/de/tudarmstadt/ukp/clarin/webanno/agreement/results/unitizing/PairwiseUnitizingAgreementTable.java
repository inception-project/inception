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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultRefreshingView;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.PopoverBehavior;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class PairwiseUnitizingAgreementTable
    extends Panel
{
    private static final long serialVersionUID = 571396822546125376L;

    private final static Logger LOG = LoggerFactory
            .getLogger(PairwiseUnitizingAgreementTable.class);

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private final RefreshingView<String> rows;

    public PairwiseUnitizingAgreementTable(String aId,
            IModel<PairwiseAnnotationResult<UnitizingAgreementResult>> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        PopoverConfig config = new PopoverConfig().withPlacement(Placement.left).withHtml(true);
        WebMarkupContainer legend = new WebMarkupContainer("legend");
        legend.add(new PopoverBehavior(new ResourceModel("legend"),
                new StringResourceModel("legend.content", legend), config));
        add(legend);

        // This model makes sure we add a "null" dummy rater which accounts for the header columns
        // of the table.
        final IModel<List<String>> ratersAdapter = LoadableDetachableModel.of(() -> {
            List<String> raters = new ArrayList<>();
            if (getModelObject() != null) {
                raters.add(null);
                raters.addAll(getModelObject().getRaters());
            }
            return raters;
        });

        rows = new DefaultRefreshingView<String>("rows", ratersAdapter)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<String> aRowItem)
            {
                // Render regular row
                aRowItem.add(new DefaultRefreshingView<String>("cells", ratersAdapter)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<String> aCellItem)
                    {
                        aCellItem.setRenderBodyOnly(true);

                        Fragment cell;
                        // Header cell
                        if (aRowItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-centered",
                                    PairwiseUnitizingAgreementTable.this);
                        }
                        else if (aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-right",
                                    PairwiseUnitizingAgreementTable.this);
                        }
                        // Content cell
                        else {
                            cell = new Fragment("cell", "td", PairwiseUnitizingAgreementTable.this);
                        }

                        // Top-left cell
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", Model.of("")));
                        }
                        // Raters header horizontally
                        else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell.add(new Label("label",
                                    userRepository.get(aCellItem.getModelObject()).getUiName()));
                        }
                        // Raters header vertically
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label",
                                    userRepository.get(aRowItem.getModelObject()).getUiName()));
                        }
                        // Upper diagonal
                        else if (aCellItem.getIndex() > aRowItem.getIndex()) {
                            cell.add(makeUpperDiagonalCellLabel(aRowItem.getModelObject(),
                                    aCellItem.getModelObject()));
                        }
                        // Lower diagonal
                        else if (aCellItem.getIndex() < aRowItem.getIndex()) {
                            cell.add(makeLowerDiagonalCellLabel(aRowItem.getModelObject(),
                                    aCellItem.getModelObject()));
                        }
                        // Rest
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

        Set<String> raters = getModelObject().getRaters();
        this.add(visibleWhen(
                () -> (getModelObject() != null && !getModelObject().getRaters().isEmpty())));
        add(rows);
    }

    private Label makeLowerDiagonalCellLabel(String aRater1, String aRater2)
    {
        // UnitizingAgreementResult result = getModelObject().getStudy(aRater1,
        // aRater2);

        // String label = String.format("%d/%d", result.getCompleteSetCount(),
        // result.getRelevantSetCount());
        //
        // String tooltipTitle = "Details about annotations excluded from agreement calculation";

        // StringBuilder tooltipContent = new StringBuilder();
        // if (result.isExcludeIncomplete()) {
        // tooltipContent.append(String.format("- Incomplete (missing): %d%n",
        // result.getIncompleteSetsByPosition().size()));
        // tooltipContent.append(String.format(
        // "- Incomplete (not labeled): %d%n", result
        // .getIncompleteSetsByLabel().size()));
        // }
        // tooltipContent.append(String.format("- Plurality: %d", result
        // .getPluralitySets().size()));

        // Label l = new Label("label", Model.of(label));
        // DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
        // tooltipTitle, tooltipContent.toString());
        // tooltip.setOption("position", (Object) null);
        // l.add(tooltip);
        // l.add(new AttributeAppender("style", "cursor: help", ";"));
        // return l;

        return new Label("label");
    }

    private Label makeUpperDiagonalCellLabel(String aRater1, String aRater2)
    {
        UnitizingAgreementResult result = getModelObject().getStudy(aRater1, aRater2);

        boolean noDataRater0 = isAllNull(result, 0);
        boolean noDataRater1 = isAllNull(result, 1);

        String label;
        if (result.getStudy().getUnitCount() == 0) {
            label = "no positions";
        }
        else if (noDataRater0 && noDataRater1) {
            label = "no labels";
        }
        else if (noDataRater0) {
            label = "no labels from " + result.getCasGroupIds().get(0);
        }
        else if (noDataRater1) {
            label = "no labels from " + result.getCasGroupIds().get(1);
        }
        // else if (incPos == result.getRelevantSetCount()) {
        // label = "positions disjunct";
        // }
        // else if (incLabel == result.getRelevantSetCount()) {
        // label = "labels disjunct";
        // }
        // else if ((incLabel + incPos) == result.getRelevantSetCount()) {
        // label = "labels/positions disjunct";
        // }
        else {
            label = String.format("%.2f", result.getAgreement());
        }

        String tooltipTitle = result.getCasGroupIds().get(0) + '/' + result.getCasGroupIds().get(1);

        String tooltipContent = "Positions annotated:\n"
                + String.format("- %s: %d/%d%n", result.getCasGroupIds().get(0),
                        getNonNullCount(result, 0), result.getStudy().getUnitCount(0))
                + String.format("- %s: %d/%d%n", result.getCasGroupIds().get(1),
                        getNonNullCount(result, 1), result.getStudy().getUnitCount(1))
                + String.format("Distinct labels: %d%n", result.getStudy().getCategoryCount());

        Label l = new Label("label", Model.of(label));
        DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(tooltipTitle,
                tooltipContent);
        tooltip.setOption("position", (Object) null);
        l.add(tooltip);
        l.add(new AttributeAppender("style", "cursor: help", ";"));

        return l;
    }

    public boolean isAllNull(AgreementResult<IUnitizingAnnotationStudy> aResult, int aRaterIdx)
    {
        return !aResult.getStudy().getUnits().stream()
                .anyMatch(u -> u.getRaterIdx() == aRaterIdx && u.getCategory() != null);
    }

    public long getNonNullCount(AgreementResult<IUnitizingAnnotationStudy> aResult, int aRaterIdx)
    {
        return aResult.getStudy().getUnits().stream()
                .filter(u -> u.getRaterIdx() == aRaterIdx && u.getCategory() != null).count();
    }

    public PairwiseAnnotationResult<UnitizingAgreementResult> getModelObject()
    {
        return (PairwiseAnnotationResult<UnitizingAgreementResult>) getDefaultModelObject();
    }

    public void setModelObject(PairwiseAnnotationResult<IUnitizingAnnotationStudy> aAgreements2)
    {
        setDefaultModelObject(aAgreements2);
    }
}
