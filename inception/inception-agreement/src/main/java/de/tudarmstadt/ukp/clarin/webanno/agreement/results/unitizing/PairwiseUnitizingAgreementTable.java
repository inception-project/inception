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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
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

public class PairwiseUnitizingAgreementTable
    extends GenericPanel<PairwiseAgreementResult>
{
    private static final long serialVersionUID = 571396822546125376L;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private final RefreshingView<User> rows;

    public PairwiseUnitizingAgreementTable(String aId, IModel<PairwiseAgreementResult> aModel,
            DefaultAgreementTraits aTraits)
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
                            cell.add(new Label("label", aCellItem.getModelObject().getUiName()));
                        }
                        // Raters header vertically
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", aRowItem.getModelObject().getUiName()));
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

        this.add(visibleWhen(
                () -> (getModelObject() != null && !getModelObject().getRaters().isEmpty())));
        add(rows);
    }

    private Label makeLowerDiagonalCellLabel(User aRater1, User aRater2)
    {
        return new Label("label");
    }

    private Label makeUpperDiagonalCellLabel(User aRater1, User aRater2)
    {
        var result = getModelObject().getResult(aRater1.getUsername(), aRater2.getUsername());

        if (result == null || result.getCasGroupIds().isEmpty()) {
            return new Label("label", "no data");
        }

        if (result.getCasGroupIds().size() != 2) {
            throw new IllegalStateException(
                    "Pairwise agreeement always requires two annotators, but got: "
                            + result.getCasGroupIds());
        }

        var casGroupId1 = result.getCasGroupIds().get(0);
        var casGroupId2 = result.getCasGroupIds().get(1);
        var noDataRater1 = result.isAllNull(casGroupId1);
        var noDataRater2 = result.isAllNull(casGroupId2);

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
        else {
            label = format("%.2f", result.getAgreement());
        }

        var tooltipTitle = aRater1.getUiName() + " ↔ " + aRater2.getUiName();

        var tooltipContent = format("Documents counted: %d/%d%n", result.getUsableAgreementsCount(),
                result.getTotalAgreementsCount())
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

        return l;
    }

    private void actionScoreClicked(AjaxRequestTarget aTarget, User aRater1, User aRater2)
    {
        send(this, BUBBLE, new PairwiseAgreementScoreClickedEvent(aTarget, aRater1.getUsername(),
                aRater2.getUsername()));
    }
}
