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
package de.tudarmstadt.ukp.inception.htmleditor.annotatorjs;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getUiLabelText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRules;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Range;

public class AnnotatorJsRenderer
{
    private final ColoringService coloringService;
    private final AnnotationSchemaService annotationService;

    public AnnotatorJsRenderer(ColoringService aColoringService,
            AnnotationSchemaService aAnnotationService)
    {
        coloringService = aColoringService;
        annotationService = aAnnotationService;
    }

    public List<Annotation> render(AnnotatorState aState, VDocument aVDoc, CAS aCas,
            ColoringStrategy aColoringStrategy)
    {
        List<Annotation> annotations = new ArrayList<>();

        // Render visible (custom) layers
        Map<String[], Queue<String>> colorQueues = new HashMap<>();
        for (AnnotationLayer layer : aState.getAllAnnotationLayers()) {
            ColoringStrategy coloringStrategy = aColoringStrategy != null ? aColoringStrategy
                    : coloringService.getStrategy(layer, aState.getPreferences(), colorQueues);

            // If the layer is not included in the rendering, then we skip here - but only after
            // we have obtained a coloring strategy for this layer and thus secured the layer
            // color. This ensures that the layer colors do not change depending on the number
            // of visible layers.
            if (!aVDoc.getAnnotationLayers().contains(layer)) {
                continue;
            }

            TypeAdapter typeAdapter = annotationService.getAdapter(layer);

            ColoringRules coloringRules = typeAdapter.getTraits(ColoringRulesTrait.class)
                    .map(ColoringRulesTrait::getColoringRules).orElse(null);

            for (VSpan vspan : aVDoc.spans(layer.getId())) {
                String labelText = getUiLabelText(typeAdapter, vspan);
                String color = coloringStrategy.getColor(vspan, labelText, coloringRules);

                Annotation anno = new Annotation();
                anno.setId(vspan.getVid().toString());
                anno.setText(labelText);
                anno.setColor(color);
                // Looks like the "quote" is not really required for AnnotatorJS to render the
                // annotation.
                anno.setQuote("");
                anno.setRanges(toRanges(vspan.getRanges()));
                annotations.add(anno);
            }
        }

        return annotations;
    }

    private List<Range> toRanges(List<VRange> aRanges)
    {
        return aRanges.stream().map(r -> new Range(r.getBegin(), r.getEnd()))
                .collect(Collectors.toList());
    }
}
