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

public class SpanLayerTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;

    private String layerSupportId;
    private IModel<AnnotationLayer> feature;
    private CompoundPropertyModel<SpanLayerTraits> traits;
    private ColoringRulesConfigurationPanel coloringRules;

    public SpanLayerTraitsEditor(String aId, SpanLayerSupport aFS, IModel<AnnotationLayer> aLayer)
    {
        super(aId, aLayer);

        layerSupportId = aFS.getId();
        feature = aLayer;

        traits = CompoundPropertyModel.of(getLayerSupport().readTraits(feature.getObject()));

        Form<SpanLayerTraits> form = new Form<SpanLayerTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                getLayerSupport().writeTraits(feature.getObject(), traits.getObject());
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
                aFeature, traits.bind("coloringRules.rules"));
        panel.setOutputMarkupId(true);
        return panel;
    }

    private SpanLayerSupport getLayerSupport()
    {
        return (SpanLayerSupport) layerSupportRegistry.getLayerSupport(layerSupportId);
    }
}
