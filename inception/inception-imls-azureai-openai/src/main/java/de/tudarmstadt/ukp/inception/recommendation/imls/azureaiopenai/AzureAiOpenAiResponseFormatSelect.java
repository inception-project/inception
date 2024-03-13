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
package de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai.client.GenerateResponseFormat;

public class AzureAiOpenAiResponseFormatSelect
    extends DropDownChoice<GenerateResponseFormat>
{
    private static final long serialVersionUID = 3115872987735239823L;

    public AzureAiOpenAiResponseFormatSelect(String aId)
    {
        super(aId);
    }

    public AzureAiOpenAiResponseFormatSelect(String aId, IModel<GenerateResponseFormat> aModel)
    {
        super(aId);
        setModel(aModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setChoiceRenderer(new EnumChoiceRenderer<>(this));
        setChoices(asList(GenerateResponseFormat.values()));
        setNullValid(true);
    }
}
