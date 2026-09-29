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
package de.tudarmstadt.ukp.inception.ui.agreement.page;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcerns.suggestSegmentationMeasures;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.BOUNDARY_DISAGREEMENT;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureParadigm.CODING;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.Math.round;
import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.config.AgreementDiagnosticsProperties;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcern;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Collapsible panel reporting characteristics of the annotated data that affect how the agreement
 * score above it should be read.
 * <p>
 * The concerns shown here are diagnoses of a result that has already been computed, not predictions
 * made before looking at the data. They are phrased as considerations rather than verdicts: which
 * agreement measure is appropriate for e.g. skewed data is a live methodological question, so the
 * panel states what was observed and what it implies for the measure that was used, and leaves the
 * decision to the user.
 */
public class AgreementConcernsPanel
    extends Panel
{
    private static final long serialVersionUID = 4382277020466046423L;

    /** Values closer than this are treated as the same measurement rather than as a range. */
    private static final double RANGE_EPSILON = 0.005;

    /**
     * Up to this many comparisons exhibiting a note are named. Beyond that, the list would bury the
     * note, and the count alone says that the note is widespread.
     */
    private static final int MAX_NAMED_COMPARISONS = 3;

    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;
    private @SpringBean UserDao userService;
    // The analyzer is a class without a default constructor, which Wicket cannot proxy - so the
    // properties it is built from are injected instead.
    private @SpringBean AgreementDiagnosticsProperties diagnosticsProperties;

    private final IModel<? extends AgreementResult_ImplBase> result;

    /**
     * The measure is held by id rather than as a reference because the supports are Spring beans
     * and not serializable, while this panel is.
     */
    private final String measureId;

    private boolean expanded = false;

    public AgreementConcernsPanel(String aId, IModel<? extends AgreementResult_ImplBase> aResult,
            String aMeasureId)
    {
        super(aId);

        result = aResult;
        measureId = aMeasureId;

        setOutputMarkupPlaceholderTag(true);

        // The whole panel disappears when the data gives no reason for comment. An advisory that
        // shows up on every result would quickly be ignored.
        add(visibleWhen(() -> !listConcerns().isEmpty()));

        queue(new Label("concernCount",
                LambdaModel.of(() -> new StringResourceModel("concernCount", this) //
                        .setParameters(listConcerns().size()) //
                        .getString())));

        var toggle = new LambdaAjaxLink("toggle", this::actionToggle);
        toggle.setOutputMarkupId(true);
        toggle.add(AttributeModifier.replace("aria-expanded",
                LambdaModel.of(() -> String.valueOf(expanded))));
        queue(toggle);

        var caret = new WebMarkupContainer("caret");
        caret.add(new AttributeAppender("class",
                LambdaModel.of(() -> expanded ? "fa-caret-down" : "fa-caret-right"), " "));
        queue(caret);

        var body = new WebMarkupContainer("body");
        body.setOutputMarkupPlaceholderTag(true);
        body.add(visibleWhen(() -> expanded));
        queue(body);
        toggle.add(AttributeModifier.replace("aria-controls", LambdaModel.of(body::getMarkupId)));

        queue(new ListView<ConcernItem>("concerns", LambdaModel.of(this::listConcerns))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ConcernItem> aItem)
            {
                var item = aItem.getModelObject();

                aItem.queue(new Label("observation", item.observation));
                aItem.queue(new Label("implication", item.implication));

                var suggestion = new Label("suggestion", item.suggestion);
                suggestion.add(visibleWhen(() -> item.suggestion != null));
                aItem.queue(suggestion);

                var scope = new Label("scope", item.scope);
                scope.add(visibleWhen(() -> item.scope != null));
                aItem.queue(scope);

                var range = new Label("range", item.range);
                range.add(visibleWhen(() -> item.range != null));
                aItem.queue(range);

                var criterion = new Label("criterion", item.criterion);
                criterion.add(visibleWhen(() -> item.criterion != null));
                aItem.queue(criterion);
            }
        });
    }

    private void actionToggle(AjaxRequestTarget aTarget)
    {
        expanded = !expanded;
        aTarget.add(this);
    }

    private List<ConcernItem> listConcerns()
    {
        // Every result type aggregates its diagnostics over whatever it compares - rater pairs for
        // the pairwise result, documents for the per-document one - so the panel does not have to
        // know which kind it was handed.
        var aggregatedResult = result.getObject();
        if (aggregatedResult == null) {
            return emptyList();
        }

        var support = agreementRegistry.getAgreementMeasureSupport(measureId);
        if (support == null) {
            return emptyList();
        }

        var items = new ArrayList<ConcernItem>();
        for (var diagnostic : aggregatedResult.getDiagnostics()) {
            support.getConcern(diagnostic) //
                    .ifPresent(concern -> items.add(toItem(concern, aggregatedResult)));
        }

        return items;
    }

    private ConcernItem toItem(AgreementConcern aConcern, AgreementResult_ImplBase aResult)
    {
        var item = new ConcernItem();
        item.observation = aConcern.getObservation();
        item.implication = aConcern.getImplication();
        item.suggestion = aConcern.getSuggestion().orElse(null);

        if (item.suggestion == null && aConcern.getType() == BOUNDARY_DISAGREEMENT) {
            item.suggestion = suggestSegmentationMeasures(listSegmentationMeasures(aResult))
                    .orElse(null);
        }

        // A characteristic seen in only a few of many rater pairs means something different from
        // one seen in all of them, so say which it is instead of averaging it away.
        var affected = aResult.getDiagnosticCount(aConcern.getDiagnostic());
        var total = aResult.getComparisonCount();
        if (total > 1) {
            // Where only a few comparisons exhibit it, name them - otherwise the table offers no
            // way of telling which of them the note is about.
            var named = affected < total && affected <= MAX_NAMED_COMPARISONS
                    ? aResult.getComparisonsExhibiting(aConcern.getDiagnostic(), this::getUserName)
                    : List.<String> of();
            item.scope = named.isEmpty() //
                    ? new StringResourceModel("concernScope", this) //
                            .setParameters(affected, total) //
                            .getString()
                    : new StringResourceModel("concernScopeNamed", this) //
                            .setParameters(affected, total, String.join(", ", named)) //
                            .getString();
        }

        // The observation above belongs to whichever rater pair happened to be encountered first -
        // diagnostics are grouped by type so they can be counted per pair, not by their measured
        // value. Where the pairs actually measured different values, state the range so that the
        // one pair's number is not read as holding for all of them.
        if (affected > 1) {
            aResult.getDiagnosticValueRange(aConcern.getDiagnostic()) //
                    .filter(range -> range[1] - range[0] > RANGE_EPSILON) //
                    .ifPresent(range -> item.range = new StringResourceModel("concernRange", this) //
                            .setParameters(formatValue(aConcern.getType(), range[0]),
                                    formatValue(aConcern.getType(), range[1])) //
                            .getString());
        }

        // Each threshold is applied to every comparison on its own, so one just short of it stays
        // silent although its data looks much the same. Stating the threshold explains that.
        item.criterion = new AgreementDiagnostics(diagnosticsProperties)
                .describeCriterion(aConcern.getDiagnostic()) //
                .orElse(null);

        return item;
    }

    private String getUserName(String aUsername)
    {
        return Optional.ofNullable(userService.getUserOrCurationUser(aUsername)) //
                .map(User::getUiName) //
                .orElse(aUsername);
    }

    /**
     * @return the names of the measures applicable to the result's layer and feature that score the
     *         segmentation rather than compare labels at matching positions.
     */
    private List<String> listSegmentationMeasures(AgreementResult_ImplBase aResult)
    {
        var feature = aResult.getFeature();
        if (feature == null) {
            return emptyList();
        }

        return agreementRegistry.getAgreementMeasureSupports(feature.getLayer(), feature) //
                .stream() //
                .filter(support -> support.getParadigm() != CODING) //
                .map(AgreementMeasureSupport::getName) //
                .toList();
    }

    /**
     * Diagnostics carry either a share or an absolute count, depending on what they claim, so the
     * range has to be rendered accordingly - see {@code AgreementDiagnosticsProperties}.
     */
    private static String formatValue(AgreementDiagnosticType aType, double aValue)
    {
        switch (aType) {
        case FEW_ITEMS:
        case SINGLE_CATEGORY:
            return String.valueOf((long) aValue);
        case MARGINAL_ASYMMETRY:
            // The value is the gap between two shares, not a share itself
            return round(aValue * 100) + " percentage points";
        default:
            return round(aValue * 100) + "%";
        }
    }

    private static class ConcernItem
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        String observation;
        String implication;
        String suggestion;
        String scope;
        String range;
        String criterion;
    }
}
