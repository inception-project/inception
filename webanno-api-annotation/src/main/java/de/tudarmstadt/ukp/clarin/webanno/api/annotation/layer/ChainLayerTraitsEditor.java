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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.behaviors.AnchoringModeSelect;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class ChainLayerTraitsEditor
    extends LayerTraitsEditor_ImplBase<ChainLayerTraits, ChainLayerSupport>
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final Logger LOG = LoggerFactory.getLogger(ChainLayerTraitsEditor.class);

    public ChainLayerTraitsEditor(String aId, ChainLayerSupport aLayerSupport,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayerSupport, aLayer);
    }

    @Override
    protected void initializeForm(Form<ChainLayerTraits> aForm)
    {
        aForm.add(new AnchoringModeSelect("anchoringMode", getLayerModel()));
        
        CheckBox linkedListBehavior = new CheckBox("linkedListBehavior");
        linkedListBehavior.setModel(PropertyModel.of(getLayerModel(), "linkedListBehavior"));
        linkedListBehavior.add(AjaxFormComponentUpdatingBehavior.onUpdate("change", _target -> {
            try {
                @SuppressWarnings("unchecked")
                Component layerDetailForm = findParent((Class<Component>) Class.forName(
                        "de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.ProjectLayersPanel"));
                _target.add(layerDetailForm.get("featureSelectionForm"));
                _target.add(layerDetailForm.get("featureDetailForm"));
            }
            catch (ClassNotFoundException e) {
                LOG.error("Unable to update feature selection and detail forms", e);
            }
        }));
        aForm.add(linkedListBehavior);
    }
}
