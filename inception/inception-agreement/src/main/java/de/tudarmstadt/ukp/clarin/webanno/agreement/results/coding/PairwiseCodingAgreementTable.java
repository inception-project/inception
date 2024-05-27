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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.event.PairwiseAgreementScoreClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.bootstrap.PopoverBehavior;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.DefaultRefreshingView;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior;

public class PairwiseCodingAgreementTable
    extends GenericPanel<PairwiseAgreementResult>
{
    private static final long serialVersionUID = 571396822546125376L;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private final RefreshingView<User> rows;
    private final DefaultAgreementTraits traits;

    public PairwiseCodingAgreementTable(String aId, IModel<PairwiseAgreementResult> aModel,
            DefaultAgreementTraits aTraits)
    {
        super(aId, aModel);

        traits = aTraits;

        setOutputMarkupId(true);

        var config = new PopoverConfig().withPlacement(Placement.left).withHtml(true);
        var legend = new WebMarkupContainer("legend");
        legend.add(new PopoverBehavior(new ResourceModel("legend"),
                new StringResourceModel("legend.content", legend), config));
        add(legend);

        // This model makes sure we add a "null" dummy rater which accounts for the header columns
        // of the table.
        final IModel<List<User>> ratersAdapter = LoadableDetachableModel.of(() -> {
            var raters = new ArrayList<User>();
            if (getModelObject() != null) {
                raters.add(null);
                for (var rater : getModelObject().getRaters()) {
                    var user = userRepository.get(rater);
                    if (user != null) {
                        raters.add(user);
                    }
                }
            }
            return raters;
        });

        rows = new DefaultRefreshingView<User>("rows", ratersAdapter)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<User> aRowItem)
            {
                // Render regular row
                aRowItem.add(new DefaultRefreshingView<User>("cells", ratersAdapter)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item<User> aCellItem)
                    {
                        aCellItem.setRenderBodyOnly(true);

                        Fragment cell;

                        // Top-left cell
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-centered",
                                    PairwiseCodingAgreementTable.this);
                            cell.add(new Label("label", Model.of("")));
                        }
                        // Raters header horizontally
                        else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell = new Fragment("cell", "th-centered",
                                    PairwiseCodingAgreementTable.this);
                            cell.add(new Label("label", aCellItem.getModelObject().getUiName()));
                        }
                        // Raters header vertically
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th-right",
                                    PairwiseCodingAgreementTable.this);
                            cell.add(new Label("label", aRowItem.getModelObject().getUiName()));
                        }
                        // Upper diagonal
                        else if (aCellItem.getIndex() > aRowItem.getIndex()) {
                            cell = makeUpperDiagonalCell(aRowItem.getModelObject(),
                                    aCellItem.getModelObject());
                        }
                        // Lower diagonal
                        else {
                            cell = makeLowerDiagonalCell(aRowItem.getModelObject(),
                                    aCellItem.getModelObject());
                        }

                        aCellItem.add(cell);
                    }
                });
                // Odd/even coloring is reversed here to account for the header row at index 0
                aRowItem.add(new AttributeAppender("class",
                        (aRowItem.getIndex() % 2 == 0) ? "odd" : "even"));
            }
        };

        this.add(visibleWhen(
                () -> (getModelObject() != null && !getModelObject().getRaters().isEmpty())));
        add(rows);
    }

    private Fragment makeLowerDiagonalCell(User aRater1, User aRater2)
    {
        var result = getModelObject().getResult(aRater1.getUsername(), aRater2.getUsername());

        if (result == null || result.getCasGroupIds().isEmpty()
                || result.getRelevantSetCount() == result.getUsedSetCount()) {
            var cell = new Fragment("cell", "td-lower-ok", PairwiseCodingAgreementTable.this);
            cell.add(new Label("label", "-"));
            return cell;
        }

        var tooltipTitle = "Details about annotations excluded from agreement calculation";

        var msg = new StringBuilder();
        if (traits.isExcludeIncomplete()) {
            msg.append(
                    format("- Incomplete (missing): %d%n", result.getIncompleteSetsByPosition()));
            msg.append(
                    format("- Incomplete (not labeled): %d%n", result.getIncompleteSetsByLabel()));
        }
        msg.append(format("- Stacked: %d\n\n", result.getPluralitySets()));
        msg.append(
                format("Used: %d of %d", result.getUsedSetCount(), result.getRelevantSetCount()));

        var l = new Label("label",
                format("%d", result.getRelevantSetCount() - result.getUsedSetCount()));
        var tooltip = new DescriptionTooltipBehavior(tooltipTitle, msg.toString());
        tooltip.setOption("position", (Object) null);
        l.add(tooltip);
        l.add(new AttributeAppender("style", "cursor: help", ";"));

        var cell = new Fragment("cell", "td-lower-warn", PairwiseCodingAgreementTable.this);
        cell.add(l);
        return cell;
    }

    private Fragment makeUpperDiagonalCell(User aRater1, User aRater2)
    {
        var result = getModelObject().getResult(aRater1.getUsername(), aRater2.getUsername());

        if (result == null || result.getCasGroupIds().isEmpty()) {
            var cell = new Fragment("cell", "td-upper-warn", PairwiseCodingAgreementTable.this);
            cell.add(new Label("label", "no data"));
            return cell;
        }

        if (result.getCasGroupIds().size() != 2) {
            throw new IllegalStateException(
                    "Pairwise agreeement always requires two annotators, but got: "
                            + result.getCasGroupIds());
        }

        var fragmentId = "td-upper-warn";
        var casGroupId1 = result.getCasGroupIds().get(0);
        var casGroupId2 = result.getCasGroupIds().get(1);
        var noDataRater1 = result.isAllNull(casGroupId1);
        var noDataRater2 = result.isAllNull(casGroupId2);
        var incPos = result.getIncompleteSetsByPosition();
        var incLabel = result.getIncompleteSetsByLabel();

        String label;
        if (result.isEmpty()) {
            label = "no positions";
        }
        else if (noDataRater1 && noDataRater2) {
            label = "no labels";
        }
        else if (noDataRater1) {
            label = "no labels from " + aRater1.getUiName();
        }
        else if (noDataRater2) {
            label = "no labels from " + aRater2.getUiName();
        }
        else if (incPos == result.getRelevantSetCount()) {
            label = "positions disjunct";
        }
        else if (incLabel == result.getRelevantSetCount()) {
            label = "labels disjunct";
        }
        else if ((incLabel + incPos) == result.getRelevantSetCount()) {
            label = "labels/positions disjunct";
        }
        else {
            label = format("%.2f", result.getAgreement());
            fragmentId = "td-upper-ok";
        }

        var tooltipTitle = aRater1.getUiName() + " ↔ " + aRater2.getUiName();

        var tooltipContent = format("Documents with agreement score: %d/%d%n",
                result.getUsableAgreementsCount(), result.getTotalAgreementsCount())
                + "Positions annotated:\n"
                + format("- %s: %d/%d%n", aRater1.getUiName(), result.getNonNullCount(casGroupId1),
                        result.getItemCount(casGroupId1))
                + format("- %s: %d/%d%n", aRater2.getUiName(), result.getNonNullCount(casGroupId2),
                        result.getItemCount(casGroupId2))
                + format("Distinct labels: %d%n", result.getCategoryCount())
                + "Click to download pairwise diff as CSV file.";

        var l = new Label("label", Model.of(label));
        var tooltip = new DescriptionTooltipBehavior(tooltipTitle, tooltipContent);
        tooltip.setOption("position", (Object) null);
        l.add(tooltip);
        l.add(new AttributeAppender("style", "cursor: pointer", ";"));

        l.add(AjaxEventBehavior.onEvent(CLICK_EVENT,
                _target -> actionScoreClicked(_target, aRater1, aRater2)));

        var cell = new Fragment("cell", fragmentId, PairwiseCodingAgreementTable.this);
        cell.add(l);
        return cell;
    }

    private void actionScoreClicked(AjaxRequestTarget aTarget, User aRater1, User aRater2)
    {
        send(this, BUBBLE, new PairwiseAgreementScoreClickedEvent(aTarget, aRater1.getUsername(),
                aRater2.getUsername()));
    }
}
