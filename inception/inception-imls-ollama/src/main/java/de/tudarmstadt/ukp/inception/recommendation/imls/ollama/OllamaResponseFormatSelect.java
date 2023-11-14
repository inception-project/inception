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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateResponseFormat;

public class OllamaResponseFormatSelect
    extends DropDownChoice<OllamaGenerateResponseFormat>
{
    private static final long serialVersionUID = 3115872987735239823L;

    public OllamaResponseFormatSelect(String aId)
    {
        super(aId);
    }

    public OllamaResponseFormatSelect(String aId, IModel<OllamaGenerateResponseFormat> aModel)
    {
        super(aId);
        setModel(aModel);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setChoiceRenderer(new EnumChoiceRenderer<>(this));
        setChoices(asList(OllamaGenerateResponseFormat.values()));
        setNullValid(true);
    }
}
