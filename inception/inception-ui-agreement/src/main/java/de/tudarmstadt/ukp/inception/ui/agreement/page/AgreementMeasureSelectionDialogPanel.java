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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureCapability;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureParadigm;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Dialog for choosing an agreement measure. Presents all registered measures grouped by paradigm as
 * cards showing a description and capability badges. Measures that are not applicable to the
 * currently selected layer/feature are shown greyed-out with an explanation, so the user can see
 * what exists and why it is unavailable rather than the option simply being missing.
 */
public class AgreementMeasureSelectionDialogPanel
    extends Panel
{
    private static final long serialVersionUID = -7069776539144632017L;

    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;

    private final AnnotationLayer layer;
    private final AnnotationFeature feature;
    private final SerializableBiConsumer onSelect;

    /**
     * @param aId
     *            markup id.
     * @param aLayer
     *            the currently selected layer (used to determine applicability).
     * @param aFeature
     *            the currently selected feature (may be null for position-based measures).
     * @param aOnSelect
     *            callback invoked with the id of the chosen measure when the user picks one.
     */
    public AgreementMeasureSelectionDialogPanel(String aId, AnnotationLayer aLayer,
            AnnotationFeature aFeature, SerializableBiConsumer aOnSelect)
    {
        super(aId);

        layer = aLayer;
        feature = aFeature;
        onSelect = aOnSelect;

        var groups = new ListView<ParadigmGroup>("paradigms",
                LambdaModel.of(this::listParadigmGroups))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ParadigmGroup> aGroupItem)
            {
                var group = aGroupItem.getModelObject();
                aGroupItem.queue(new Label("paradigmName", group.paradigm.getUiName()));
                aGroupItem.queue(new Label("paradigmDescription", group.paradigm.getDescription()));
                aGroupItem.queue(newMeasureList("measures", group.measures));
            }
        };
        queue(groups);

        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));
    }

    private ListView<MeasureItem> newMeasureList(String aId, List<MeasureItem> aMeasures)
    {
        return new ListView<MeasureItem>(aId, aMeasures)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<MeasureItem> aItem)
            {
                var item = aItem.getModelObject();

                var card = new LambdaAjaxLink("select", _target -> actionSelect(_target, item.id));
                card.setEnabled(item.applicable);
                aItem.queue(card);

                aItem.queue(new Label("name", item.name));
                aItem.queue(new Label("description", item.description));

                var reason = new Label("reason", item.inapplicabilityReason);
                reason.add(visibleWhen(() -> !item.applicable));
                aItem.queue(reason);

                aItem.queue(new ListView<String>("capabilities", item.capabilities)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> aBadgeItem)
                    {
                        aBadgeItem.queue(new Label("capability", aBadgeItem.getModelObject()));
                    }
                });
            }
        };
    }

    private List<ParadigmGroup> listParadigmGroups()
    {
        var applicable = agreementRegistry.getAgreementMeasureSupports(layer, feature);
        var applicableIds = applicable.stream().map(AgreementMeasureSupport::getId).toList();

        var items = new ArrayList<MeasureItem>();
        for (var support : agreementRegistry.getAgreementMeasureSupports()) {
            items.add(toItem(support, applicableIds.contains(support.getId())));
        }

        var byParadigm = items.stream().collect(groupingBy(i -> i.paradigm));

        var groups = new ArrayList<ParadigmGroup>();
        for (var paradigm : AgreementMeasureParadigm.values()) {
            var measures = byParadigm.get(paradigm);
            if (measures == null || measures.isEmpty()) {
                continue;
            }
            measures.sort(comparing(m -> m.name));
            groups.add(new ParadigmGroup(paradigm, measures));
        }
        return groups;
    }

    private MeasureItem toItem(AgreementMeasureSupport<?, ?, ?> aSupport, boolean aApplicable)
    {
        var item = new MeasureItem();
        item.id = aSupport.getId();
        item.name = aSupport.getName();
        item.description = aSupport.getDescription().orElse("");
        item.paradigm = aSupport.getParadigm();
        item.capabilities = aSupport.getCapabilities().stream() //
                // WEIGHTED is hidden for now: INCEpTION always uses a nominal distance function,
                // so weighting has no effect yet. Re-add this badge when weighted distance
                // functions become configurable - see PLAN_weighted_measures.md.
                .filter(c -> c != AgreementMeasureCapability.WEIGHTED) //
                .map(c -> c.getUiName()) //
                .sorted() //
                .toList();
        item.applicable = aApplicable;
        item.inapplicabilityReason = aApplicable ? ""
                : aSupport.getInapplicabilityReason(layer, feature)
                        .orElse("Not applicable to the selected layer / feature.");
        return item;
    }

    private void actionSelect(AjaxRequestTarget aTarget, String aMeasureId)
    {
        onSelect.accept(aTarget, aMeasureId);
        findParent(ModalDialog.class).close(aTarget);
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    @FunctionalInterface
    public interface SerializableBiConsumer
        extends Serializable
    {
        void accept(AjaxRequestTarget aTarget, String aMeasureId);
    }

    private static class ParadigmGroup
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        final AgreementMeasureParadigm paradigm;
        final List<MeasureItem> measures;

        ParadigmGroup(AgreementMeasureParadigm aParadigm, List<MeasureItem> aMeasures)
        {
            paradigm = aParadigm;
            measures = aMeasures;
        }
    }

    private static class MeasureItem
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        String id;
        String name;
        String description;
        AgreementMeasureParadigm paradigm;
        List<String> capabilities;
        boolean applicable;
        String inapplicabilityReason;
    }
}
