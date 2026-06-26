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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.getFtsBackendName;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.support.kendo.FreeTextMultiSelect;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.project.validators.Validators;

public class QuerySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -1594852739133649866L;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    private final TextField<Integer> queryLimitField;
    private final CheckBox maxQueryLimitCheckBox;
    private final CheckBox useFuzzyCheckBox;
    private final MultiSelect<String> datasetsField;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    // Named graphs fetched on-demand from the endpoint to offer as choices in addition to any IRIs
    // the user enters manually.
    private final List<String> availableDatasets = new ArrayList<>();

    public QuerySettingsPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);
        setOutputMarkupId(true);

        kbModel = aModel;

        queryLimitField = queryLimitField("maxResults", kbModel.bind("kb.maxResults"));
        add(queryLimitField);
        useFuzzyCheckBox = new CheckBox("useFuzzy", kbModel.bind("kb.useFuzzy"));
        useFuzzyCheckBox.setOutputMarkupId(true);
        add(useFuzzyCheckBox);
        maxQueryLimitCheckBox = maxQueryLimitCheckbox("maxQueryLimit", Model.of(false));
        add(maxQueryLimitCheckBox);
        add(ftsField("fullTextSearchIri", "kb.fullTextSearchIri"));

        datasetsField = new FreeTextMultiSelect("datasets")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> getChoices(String aInput)
            {
                // There is no fixed vocabulary of dataset IRIs. We offer the graphs fetched from
                // the endpoint (if any) plus whatever has been entered so far, and allow the user
                // to add arbitrary IRIs via the current input.
                var choices = new LinkedHashSet<>(getModelObject());
                choices.addAll(availableDatasets);
                var result = new ArrayList<>(choices);
                if (isNotBlank(aInput)) {
                    result.remove(aInput);
                    result.add(0, aInput);
                }
                return result;
            }
        };
        // The legacy "default dataset" has no special role at query time - it is merged into the
        // query the same way as the additional datasets. So we present a single combined "Datasets"
        // field. For backwards compatibility we keep storing the first dataset in the legacy
        // defaultDatasetIri field and only the remaining ones in the additionalDatasetIris.
        datasetsField.setModel(LambdaModel.of(this::getDatasets, this::setDatasets));
        datasetsField.add(this::validateDatasets);
        add(datasetsField);

        // Enumerating the named graphs requires a connection to the endpoint, which is only
        // available once the knowledge base has been saved (i.e. registered).
        var loadDatasetsButton = new LambdaAjaxLink("loadDatasets", this::actionLoadDatasets);
        loadDatasetsButton.add(LambdaBehavior
                .visibleWhen(() -> kbModel.getObject().getKb().getRepositoryId() != null));
        loadDatasetsButton.add(new AttributeModifier("title",
                new StringResourceModel("kb.iri.datasets.load", this)));
        add(loadDatasetsButton);
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        super.onEvent(aEvent);

        if (aEvent.getPayload() instanceof RemoteRepositoryUrlChangedEvent event) {
            // The discovered graphs belong to the previous endpoint - drop them so we do not offer
            // datasets that may not exist on the new endpoint.
            availableDatasets.clear();
            event.getTarget().add(datasetsField);
        }
    }

    private void actionLoadDatasets(AjaxRequestTarget aTarget)
    {
        var kb = kbModel.getObject().getKb();

        try {
            availableDatasets.clear();
            availableDatasets.addAll(kbService.listDatasets(kb));
            success("Loaded [" + availableDatasets.size() + "] graph(s) from the endpoint.");
        }
        catch (RuntimeException e) {
            availableDatasets.clear();
            error("Unable to load graphs from the endpoint: " + e.getMessage());
        }

        aTarget.add(datasetsField);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private Collection<String> getDatasets()
    {
        var kb = kbModel.getObject().getKb();
        var datasets = new LinkedHashSet<String>();
        if (isNotBlank(kb.getDefaultDatasetIri())) {
            datasets.add(kb.getDefaultDatasetIri());
        }
        datasets.addAll(kb.getAdditionalDatasetIris());
        return datasets;
    }

    private void setDatasets(Collection<String> aDatasets)
    {
        var kb = kbModel.getObject().getKb();
        var datasets = aDatasets != null ? new ArrayList<>(new LinkedHashSet<>(aDatasets))
                : new ArrayList<String>();
        if (datasets.isEmpty()) {
            kb.setDefaultDatasetIri(null);
            kb.setAdditionalDatasetIris(emptyList());
        }
        else {
            kb.setDefaultDatasetIri(datasets.get(0));
            kb.setAdditionalDatasetIris(datasets.subList(1, datasets.size()));
        }
    }

    private void validateDatasets(IValidatable<Collection<String>> aValidatable)
    {
        for (var iri : aValidatable.getValue()) {
            var validatable = new Validatable<String>(iri);
            Validators.IRI_VALIDATOR.validate(validatable);
            validatable.getErrors().forEach(aValidatable::error);
        }
    }

    private CheckBox maxQueryLimitCheckbox(String id, IModel<Boolean> aModel)
    {
        return new AjaxCheckBox(id, aModel)
        {
            private static final long serialVersionUID = -8390353018496338400L;

            @Override
            public void onUpdate(AjaxRequestTarget aTarget)
            {
                if (getModelObject()) {
                    queryLimitField.setModelObject(kbProperties.getHardMaxResults());
                    queryLimitField.setEnabled(false);
                }
                else {
                    queryLimitField.setEnabled(true);
                }
                aTarget.add(queryLimitField);
            }
        };
    }

    private NumberTextField<Integer> queryLimitField(String id, IModel<Integer> aModel)
    {
        var queryLimit = new NumberTextField<Integer>(id, aModel, Integer.class);
        queryLimit.setOutputMarkupId(true);
        queryLimit.setRequired(true);
        queryLimit.setMinimum(KnowledgeBasePropertiesImpl.HARD_MIN_RESULTS);
        queryLimit.setMaximum(kbProperties.getHardMaxResults());
        queryLimit.add(LambdaBehavior.onConfigure(it -> {
            // If not setting, initialize with default
            if (queryLimit.getModelObject() == null || queryLimit.getModelObject() == 0) {
                queryLimit.setModelObject(kbProperties.getDefaultMaxResults());
            }
            // Cap at local min results
            else if (queryLimit.getModelObject() < KnowledgeBasePropertiesImpl.HARD_MIN_RESULTS) {
                queryLimit.setModelObject(KnowledgeBasePropertiesImpl.HARD_MIN_RESULTS);
            }
            // Cap at local max results
            else if (queryLimit.getModelObject() > kbProperties.getHardMaxResults()) {
                queryLimit.setModelObject(kbProperties.getHardMaxResults());
            }
        }));
        return queryLimit;
    }

    private DropDownChoice<String> ftsField(String aId, String aProperty)
    {
        var choices = IriConstants.FTS_IRIS.stream() //
                .sorted(comparing(i -> getFtsBackendName(i.stringValue()))) //
                .map(IRI::stringValue) //
                .toList();

        var ftsField = new DropDownChoice<String>(aId, kbModel.bind(aProperty), choices);
        ftsField.setChoiceRenderer(new LambdaChoiceRenderer<>(IriConstants::getFtsBackendName));
        ftsField.setOutputMarkupId(true);
        ftsField.setNullValid(true);
        return ftsField;
    }
}
