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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.results.SkipDescriptions.describeSkips;
import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
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

    /**
     * The traits are deliberately not taken here. Everything this table reports about exclusion
     * comes from the result, which records what the measure actually did - a measure that cannot
     * score incomplete sets hard-codes the setting regardless of the traits the UI holds.
     */
    public PairwiseCodingAgreementTable(String aId, IModel<PairwiseAgreementResult> aModel)
    {
        super(aId, aModel);

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

                if (getModelObject().getRaters().contains(CURATION_USER)) {
                    raters.add(userRepository.getCurationUser());
                }

                if (getModelObject().getRaters().contains(INITIAL_CAS_PSEUDO_USER)) {
                    raters.add(userRepository.getInitialCasUser());
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

                        // The fragment's own tag is a <div> in the markup. Rendering only its body
                        // keeps the <td> supplied by the fragment a direct child of the <tr>,
                        // which is what the browser expects - otherwise the <td> gets hoisted out
                        // of the stray <div> and the cell loses its styling and behaviors.
                        cell.setRenderBodyOnly(true);

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

        var excludedSetCount = result.getRelevantSetCount() - result.getUsedSetCount();

        // The breakdown explains the excluded count above, so it has to report what each cause
        // actually excluded rather than how often the tag occurs. Whether incomplete sets were
        // excluded is taken from the result, i.e. the setting the measure actually used - which
        // need not match the traits the UI holds, as measures that cannot score incomplete sets
        // hard-code it. When they were scored instead, they excluded nothing and count as zero
        // here, even though the sets are still tagged incomplete.
        //
        // Sets incomplete by label are not listed: the coding measures treat an unset label as the
        // empty label, which is a valid category, so no set is ever excluded for that reason.
        var incompleteByPosition = result.isExcludeIncomplete()
                ? result.getIncompleteSetsByPosition()
                : 0;

        //
        // Incomplete sets that were scored rather than excluded can still be ignored by the
        // measure itself, if it only uses values it can pair with another rater's. Those are
        // listed separately, as it is the measure rather than the setting that excluded them.
        var msg = new StringBuilder();
        msg.append(format("- Incomplete (missing): %d%n", incompleteByPosition));
        if (result.getUnscoredSetCount() > 0) {
            msg.append(format("- Value from only one rater: %d%n", result.getUnscoredSetCount()));
        }
        msg.append(format("- Stacked: %d%n%n", result.getPluralitySets()));
        // The share rather than the count is what can be compared across pairs - pairs with more
        // positions have more to exclude - so it is what makes an annotator whose pairs are all
        // elevated stand out as a column or row.
        var excludedShare = Math.round(100.0 * excludedSetCount / result.getRelevantSetCount());
        msg.append(format("Excluded: %d of %d (%d%%)%n", excludedSetCount,
                result.getRelevantSetCount(), excludedShare));
        msg.append(
                format("Used: %d of %d", result.getUsedSetCount(), result.getRelevantSetCount()));

        var l = new Label("label", format("%d (%d%%)", excludedSetCount, excludedShare));

        // Repeat the cell's warning icon in the tooltip title so the connection between the two is
        // visible when the tooltip covers the cell it belongs to.
        var tooltip = new DescriptionTooltipBehavior(tooltipTitle, msg.toString())
                .withTitleIcon("fa-exclamation-triangle", "text-warning");
        tooltip.setOption("position", (Object) null);

        // The behaviors go on the <td> inside the fragment rather than on the fragment itself:
        // the fragment renders as a <div> wrapping that <td>, so anything attached to it would
        // land outside the table cell.
        var content = new WebMarkupContainer("content");
        content.add(l);
        content.add(tooltip);
        content.add(new AttributeAppender("class", "agreement-cell-interactive", " "));
        content.add(new AttributeAppender("style", "cursor: help", ";"));

        var cell = new Fragment("cell", "td-lower-warn", PairwiseCodingAgreementTable.this);
        cell.add(content);
        return cell;
    }

    private Fragment makeUpperDiagonalCell(User aRater1, User aRater2)
    {
        var result = getModelObject().getResult(aRater1.getUsername(), aRater2.getUsername());

        if (result == null || result.getCasGroupIds().isEmpty()) {
            var content = new WebMarkupContainer("content");
            content.add(new Label("label", "no data"));

            // Say why no document produced a score - e.g. that nothing is in a curation state yet -
            // rather than leave the user to guess.
            var skips = result != null
                    ? describeSkips(result, getModelObject().getTraits(), aRater1, aRater2)
                    : "";
            if (!skips.isEmpty()) {
                var tooltip = new DescriptionTooltipBehavior(
                        aRater1.getUiName() + " ↔ " + aRater2.getUiName(),
                        "No document produced a score.\n" + skips);
                tooltip.withTitleIcon("fa-exclamation-triangle", "text-warning");
                tooltip.setOption("position", (Object) null);
                content.add(tooltip);
            }

            content.add(new WebMarkupContainer("notes").setVisible(false));

            var cell = new Fragment("cell", "td-upper-warn", PairwiseCodingAgreementTable.this);
            cell.add(content);
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
        var positionsRater1 = result.getAnnotatedPositionCount(casGroupId1);
        var positionsRater2 = result.getAnnotatedPositionCount(casGroupId2);
        var incPos = result.getIncompleteSetsByPosition();

        // For a link feature only the links are compared, so a rater who created spans but no
        // links has not annotated anything this comparison looks at - but "no annotations" would
        // contradict the spans they can see in the editor.
        var feature = getModelObject().getFeature();
        var nothingFrom = feature != null && feature.getLinkMode() != NONE ? "no links from "
                : "no annotations from ";

        String label;
        // Checked before the empty study: when incomplete positions are excluded, positions that
        // the raters never share leave the study empty, and "no positions" would then wrongly
        // suggest that there were no annotations at all. A rater who annotated nothing also makes
        // every position incomplete, but "positions disjunct" would then suggest that both raters
        // annotated, just in different places - so that case is told apart first.
        if (result.getRelevantSetCount() > 0 && positionsRater1 == 0) {
            label = nothingFrom + aRater1.getUiName();
        }
        else if (result.getRelevantSetCount() > 0 && positionsRater2 == 0) {
            label = nothingFrom + aRater2.getUiName();
        }
        else if (result.getRelevantSetCount() > 0 && incPos == result.getRelevantSetCount()) {
            label = "positions disjunct";
        }
        else if (result.isEmpty()) {
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
        else {
            label = format("%.2f", result.getAgreement());
            fragmentId = "td-upper-ok";
        }

        var tooltipTitle = aRater1.getUiName() + " ↔ " + aRater2.getUiName();

        var tooltipContent = format("Documents with agreement score: %d/%d%n",
                result.getUsableAgreementsCount(), result.getTotalAgreementsCount())
                + describeSkips(result, getModelObject().getTraits(), aRater1, aRater2)
                + "Positions annotated:\n"
                + format("- %s: %d/%d%n", aRater1.getUiName(), result.getNonNullCount(casGroupId1),
                        result.getItemCount(casGroupId1))
                + format("- %s: %d/%d%n", aRater2.getUiName(), result.getNonNullCount(casGroupId2),
                        result.getItemCount(casGroupId2))
                + format("Distinct labels: %d%n", result.getCategoryCount());

        // Notes that apply to every pair are left to the notes above the table - marking every
        // cell for them would only drown out the notes that single out particular pairs.
        var notes = result.getDiagnostics().stream() //
                .filter(d -> !getModelObject().isCommonToAllComparisons(d)) //
                .toList();
        if (!notes.isEmpty()) {
            tooltipContent += "\nNotes:\n" + notes.stream() //
                    .map(d -> "- " + d.getObservation() + "\n") //
                    .collect(joining()) + "\n";
        }

        tooltipContent += "Click to download pairwise diff as CSV file.";

        var l = new Label("label", Model.of(label));

        var tooltip = new DescriptionTooltipBehavior(tooltipTitle, tooltipContent);
        // Only the warn fragment renders an icon on the cell, so only then is there one to echo.
        if ("td-upper-warn".equals(fragmentId)) {
            tooltip.withTitleIcon("fa-exclamation-triangle", "text-warning");
        }
        tooltip.setOption("position", (Object) null);

        // The behaviors go on the <td> inside the fragment rather than on the fragment itself:
        // the fragment renders as a <div> wrapping that <td>, so anything attached to it would
        // land outside the table cell.
        var content = new WebMarkupContainer("content");
        content.add(l);
        content.add(makeNotesBadge(notes.size()));
        content.add(tooltip);
        content.add(new AttributeAppender("class", "agreement-cell-interactive", " "));
        content.add(new AttributeAppender("style", "cursor: pointer", ";"));
        content.add(AjaxEventBehavior.onEvent(CLICK_EVENT,
                _target -> actionScoreClicked(_target, aRater1, aRater2)));

        var cell = new Fragment("cell", fragmentId, PairwiseCodingAgreementTable.this);
        cell.add(content);
        return cell;
    }

    private Component makeNotesBadge(int aCount)
    {
        if (aCount == 0) {
            return new WebMarkupContainer("notes").setVisible(false);
        }

        var badge = new Fragment("notes", "notes-badge", PairwiseCodingAgreementTable.this);
        badge.add(new Label("count", aCount));
        return badge;
    }

    private void actionScoreClicked(AjaxRequestTarget aTarget, User aRater1, User aRater2)
    {
        send(this, BUBBLE, new PairwiseAgreementScoreClickedEvent(aTarget, aRater1.getUsername(),
                aRater2.getUsername()));
    }
}
