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
import static java.util.Comparator.comparing;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class QuerySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -1594852739133649866L;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    private final TextField<Integer> queryLimitField;
    private final CheckBox maxQueryLimitCheckBox;
    private final CheckBox useFuzzyCheckBox;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

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
