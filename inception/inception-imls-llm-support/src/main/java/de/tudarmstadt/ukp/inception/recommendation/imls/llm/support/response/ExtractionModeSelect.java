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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static java.util.Arrays.asList;

import java.util.Collections;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class ExtractionModeSelect
    extends DropDownChoice<ExtractionMode>
{
    private static final long serialVersionUID = 1789605828488016006L;

    private IModel<Recommender> recommender;

    public ExtractionModeSelect(String aId, IModel<ExtractionMode> aModel,
            IModel<Recommender> aRecommender)
    {
        super(aId);
        setModel(aModel);
        recommender = aRecommender;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        setChoiceRenderer(new EnumChoiceRenderer<>(this));
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        if (!recommender.isPresent().getObject()) {
            setChoices(Collections.emptyList());
            return;
        }

        var validChoices = asList(ExtractionMode.values()).stream() //
                .filter(e -> e.accepts(recommender.getObject().getLayer())) //
                .toList();
        setChoices(validChoices);

        if (validChoices.size() == 1) {
            setModelObject(validChoices.get(0));
        }

        setVisible(validChoices.size() > 1);
    }
}
