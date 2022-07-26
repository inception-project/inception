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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringRules;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationAutoConfiguration#colorRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_COLORS)
public class ColorRenderer
    implements RenderStep
{
    public static final String ID = "ColorRenderer";

    private final AnnotationSchemaService schemaService;
    private final ColoringService coloringService;

    public ColorRenderer(AnnotationSchemaService aSchemaService, ColoringService aColoringService)
    {
        schemaService = aSchemaService;
        coloringService = aColoringService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aRequest.getAllLayers()) {
            ColoringStrategy coloringStrategy = aRequest.getColoringStrategyOverride()
                    .orElse(coloringService.getStrategy(layer, aRequest.getState().getPreferences(),
                            colorQueues));

            // If the layer is not included in the rendering, then we skip here - but only after
            // we have obtained a coloring strategy for this layer and thus secured the layer
            // color. This ensures that the layer colors do not change depending on the number
            // of visible layers.
            if (!aVDoc.getAnnotationLayers().contains(layer)) {
                continue;
            }

            TypeAdapter typeAdapter = schemaService.getAdapter(layer);

            ColoringRules coloringRules = typeAdapter.getTraits(ColoringRulesTrait.class)
                    .map(ColoringRulesTrait::getColoringRules) //
                    .orElse(null);

            for (VObject vobj : aVDoc.objects(layer.getId())) {
                vobj.setColorHint(
                        coloringStrategy.getColor(vobj, vobj.getLabelHint(), coloringRules));
            }
        }
    }
}
