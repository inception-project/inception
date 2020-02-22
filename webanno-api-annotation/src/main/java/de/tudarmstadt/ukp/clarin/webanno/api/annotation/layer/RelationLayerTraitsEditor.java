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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class RelationLayerTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;

    private String layerSupportId;
    private CompoundPropertyModel<RelationLayerTraits> traitsModel;
    private CompoundPropertyModel<AnnotationLayer> layerModel;
    private ColoringRulesConfigurationPanel coloringRules;

    public RelationLayerTraitsEditor(String aId, RelationLayerSupport aFS,
            IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayer);

        layerSupportId = aFS.getId();

        layerModel = CompoundPropertyModel.of(aLayer);
        traitsModel = CompoundPropertyModel
                .of(getLayerSupport().readTraits(layerModel.getObject()));

        Form<RelationLayerTraits> form = new Form<RelationLayerTraits>(MID_FORM, traitsModel)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                getLayerSupport().writeTraits(layerModel.getObject(), traitsModel.getObject());
            }
        };

        coloringRules = newColoringRulesConfigurationPanel(aLayer);
        form.add(coloringRules);

        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    private ColoringRulesConfigurationPanel newColoringRulesConfigurationPanel(
            IModel<AnnotationLayer> aFeature)
    {
        ColoringRulesConfigurationPanel panel = new ColoringRulesConfigurationPanel("coloringRules",
                aFeature, traitsModel.bind("coloringRules.rules"));
        panel.setOutputMarkupId(true);
        return panel;
    }

    private RelationLayerSupport getLayerSupport()
    {
        return (RelationLayerSupport) layerSupportRegistry.getLayerSupport(layerSupportId);
    }
}
