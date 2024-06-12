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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.renderer.TextRenderer;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelCard;

public class HfModelSearchField
    extends AutoCompleteTextField<HfModelCard>
{
    private static final long serialVersionUID = -483058932604036488L;

    private @SpringBean HfHubClient hfHubClient;

    protected HfModelSearchField(String aId, IModel<HfModelCard> aModel)
    {
        super(aId, aModel, new TextRenderer<>("modelId"));
    }

    @Override
    protected List<HfModelCard> getChoices(String aUserInput)
    {
        try {
            return asList(hfHubClient.listModels(aUserInput));
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
