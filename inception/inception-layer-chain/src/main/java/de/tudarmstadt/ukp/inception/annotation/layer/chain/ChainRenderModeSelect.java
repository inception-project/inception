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
package de.tudarmstadt.ukp.inception.annotation.layer.chain;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainRenderMode;

public class ChainRenderModeSelect
    extends DropDownChoice<ChainRenderMode>
{
    private static final long serialVersionUID = -7989904195942992717L;

    public ChainRenderModeSelect(String aId)
    {
        super(aId);
    }

    public ChainRenderModeSelect(String aId, IModel<AnnotationLayer> aLayerModel)
    {
        super(aId);

        setModel(PropertyModel.of(aLayerModel, "renderMode"));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setChoiceRenderer(new EnumChoiceRenderer<>(this));
        setChoices(asList(ChainRenderMode.values()));
    }
}
