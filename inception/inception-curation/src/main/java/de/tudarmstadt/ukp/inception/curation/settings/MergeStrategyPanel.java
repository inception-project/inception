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
package de.tudarmstadt.ukp.inception.curation.settings;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactoryExtensionPoint;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;

public class MergeStrategyPanel
    extends Panel
{
    private static final String MID_FORM = "form";
    private static final String MID_MERGE_STRATEGY_TRAITS_CONTAINER = "mergeStrategyTraitsContainer";
    private static final String MID_MERGE_STRATEGY_TRAITS = "mergeStrategyTraits";
    private static final String MID_MERGE_STRATEGY = "mergeStrategy";

    private static final long serialVersionUID = -1006138487509501842L;

    private @SpringBean MergeStrategyFactoryExtensionPoint mergeStrategyFactoryExtensionPoint;

    private WebMarkupContainer mergeStrategyTraitsContainer;
    private DropDownChoice<Pair<String, String>> mergeStrategyChoice;

    public MergeStrategyPanel(String aId, IModel<CurationWorkflow> aModel)
    {
        super(aId, aModel);
        setOutputMarkupPlaceholderTag(true);

        var form = new Form<CurationWorkflow>(MID_FORM, CompoundPropertyModel.of(aModel));
        add(form);

        IModel<Pair<String, String>> mergeStrategyModel = LambdaModelAdapter.of( //
                () -> {
                    Optional<MergeStrategyFactory<?>> factory = mergeStrategyFactoryExtensionPoint
                            .getExtension(getModelObject().getMergeStrategy());
                    return factory.map(_ext -> Pair.of(_ext.getId(), _ext.getLabel())).orElse(null);
                }, //
                (v) -> getModelObject().setMergeStrategy(v != null ? v.getKey() : null));

        mergeStrategyChoice = new DropDownChoice<Pair<String, String>>(MID_MERGE_STRATEGY,
                mergeStrategyModel, this::listMergeStrategies);

        mergeStrategyChoice.setChoiceRenderer(new ChoiceRenderer<>("value"));
        mergeStrategyChoice.setOutputMarkupId(true);
        mergeStrategyChoice.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            mergeStrategyTraitsContainer.addOrReplace(makeMergeStrategyTraitsEditor());
            t.add(mergeStrategyTraitsContainer);
        }));
        form.add(mergeStrategyChoice);

        mergeStrategyTraitsContainer = new WebMarkupContainer(MID_MERGE_STRATEGY_TRAITS_CONTAINER);
        mergeStrategyTraitsContainer.setOutputMarkupPlaceholderTag(true);
        mergeStrategyTraitsContainer.add(makeMergeStrategyTraitsEditor());
        form.add(mergeStrategyTraitsContainer);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        mergeStrategyTraitsContainer.addOrReplace(makeMergeStrategyTraitsEditor());
    }

    private Component makeMergeStrategyTraitsEditor()
    {
        // If the feature type has changed, we need to set up a new traits editor
        Component newTraits;
        IModel<MergeStrategyFactory<?>> factoryModel = mergeStrategyChoice.getModel()
                .map(_sel -> mergeStrategyFactoryExtensionPoint.getExtension(_sel.getKey())
                        .orElse(null));

        if (factoryModel.isPresent().getObject()) {
            newTraits = factoryModel.getObject().createTraitsEditor(MID_MERGE_STRATEGY_TRAITS,
                    getModel());
        }
        else {
            newTraits = new EmptyPanel(MID_MERGE_STRATEGY_TRAITS);
        }

        return newTraits;
    }

    public CurationWorkflow getModelObject()
    {
        return (CurationWorkflow) getDefaultModelObject();
    }

    @SuppressWarnings("unchecked")
    public IModel<CurationWorkflow> getModel()
    {
        return (IModel<CurationWorkflow>) getDefaultModel();
    }

    private List<Pair<String, String>> listMergeStrategies()
    {
        if (getModelObject() == null) {
            return Collections.emptyList();
        }

        return mergeStrategyFactoryExtensionPoint.getExtensions(getModelObject().getProject())
                .stream() //
                .map(ext -> Pair.of(ext.getId(), ext.getLabel())) //
                .collect(Collectors.toList());
    }
}
