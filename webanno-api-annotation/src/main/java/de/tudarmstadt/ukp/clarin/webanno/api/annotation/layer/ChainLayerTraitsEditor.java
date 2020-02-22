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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class ChainLayerTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final Logger LOG = LoggerFactory.getLogger(ChainLayerTraitsEditor.class);

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;

    private String layerSupportId;
    private CompoundPropertyModel<ChainLayerTraits> traitsModel;
    private IModel<AnnotationLayer> layerModel;
    private DropDownChoice<AnchoringMode> anchoringMode;
    private CheckBox linkedListBehavior;

    public ChainLayerTraitsEditor(String aId, ChainLayerSupport aFS,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayer);

        layerSupportId = aFS.getId();

        layerModel = aLayer;
        traitsModel = CompoundPropertyModel
                .of(getLayerSupport().readTraits(layerModel.getObject()));

        Form<ChainLayerTraits> form = new Form<ChainLayerTraits>(MID_FORM, traitsModel)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                getLayerSupport().writeTraits(layerModel.getObject(), traitsModel.getObject());
            }
        };

        form.add(anchoringMode = new BootstrapSelect<AnchoringMode>("anchoringMode"));
        anchoringMode.setModel(PropertyModel.of(layerModel, "anchoringMode"));
        anchoringMode.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        anchoringMode.setChoices(asList(AnchoringMode.values()));
        
        form.add(linkedListBehavior = new CheckBox("linkedListBehavior"));
        linkedListBehavior.setModel(PropertyModel.of(layerModel, "linkedListBehavior"));
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
        
        
        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    private ChainLayerSupport getLayerSupport()
    {
        return (ChainLayerSupport) layerSupportRegistry.getLayerSupport(layerSupportId);
    }
}
