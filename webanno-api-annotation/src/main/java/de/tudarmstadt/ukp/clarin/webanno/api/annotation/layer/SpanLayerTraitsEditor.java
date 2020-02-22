/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;

public class SpanLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<SpanLayerTraits, SpanLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    public SpanLayerTraitsEditor(String aId, SpanLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }
    
    @Override
    protected void initializeForm(Form<SpanLayerTraits> aForm)
    {
        aForm.add(new ColoringRulesConfigurationPanel("coloringRules",
                getLayerModel(), getTraitsModel().bind("coloringRules.rules")));
        
        DropDownChoice<AnchoringMode> anchoringMode = new BootstrapSelect<AnchoringMode>(
                "anchoringMode");
        anchoringMode.setModel(PropertyModel.of(getLayerModel(), "anchoringMode"));
        anchoringMode.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        anchoringMode.setChoices(asList(AnchoringMode.values()));
        anchoringMode.add(LambdaBehavior.onConfigure(_this -> {
            AnnotationLayer layer = getLayerModelObject();
            _this.setEnabled(
                    // Surface form must be locked to token boundaries for CONLL-U writer to work.
                    !SurfaceForm.class.getName().equals(layer.getName()) &&
                    // Not configurable for layers that attach to tokens (currently
                    // that is the only layer on which we use the attach feature)
                    layer.getAttachFeature() == null);
        }));
        aForm.add(anchoringMode);
    }
}
