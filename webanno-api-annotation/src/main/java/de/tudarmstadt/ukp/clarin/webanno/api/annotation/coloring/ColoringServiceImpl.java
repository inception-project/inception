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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategyType.DYNAMIC;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategyType.GRAY;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategyType.LEGACY;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategyType.STATIC_PASTELLE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringUtils.isTooLight;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.DISABLED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.LIGHTNESS_FILTER_THRESHOLD;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_NORMAL;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_NORMAL_FILTERED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_PASTEL;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ReadonlyColoringBehaviour.NORMAL;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;

@Component
public class ColoringServiceImpl
    implements ColoringService
{
    private final AnnotationSchemaService schemaService;

    @Autowired
    public ColoringServiceImpl(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public ColoringStrategy getStrategy(AnnotationLayer aLayer, AnnotationPreference aPreferences,
            Map<String[], Queue<String>> aColorQueues)
    {
        ColoringStrategyType t = aPreferences.getColorPerLayer().get(aLayer.getId());
        ReadonlyColoringBehaviour rt = aPreferences.getReadonlyLayerColoringBehaviour();

        if (aLayer.isReadonly() && rt != NORMAL) {
            t = rt.t;
        }

        if (t == null || t == LEGACY) {
            t = getBestInitialStrategy(aLayer, aPreferences);
        }

        return getStrategy(aLayer, t, aColorQueues);
    }

    private ColoringStrategy getStrategy(AnnotationLayer aLayer, ColoringStrategyType colortype,
            Map<String[], Queue<String>> aColorQueues)
    {
        // Decide on coloring strategy for the current layer
        switch (colortype) {
        case STATIC_PASTELLE: // ignore for the moment and fall through
        case STATIC:
            int threshold;
            if (SPAN_TYPE.equals(aLayer.getType()) && !hasLinkFeature(aLayer)) {
                threshold = MAX_VALUE; // No filtering
            }
            else {
                // Chains and arcs contain relations that are rendered as lines on the light
                // window background - need to make sure there is some contrast, so we cannot use
                // the full palette.
                threshold = LIGHTNESS_FILTER_THRESHOLD;
            }
            String[] aPalette = { nextPaletteEntry(PALETTE_PASTEL, aColorQueues, threshold) };
            return new LabelHashBasedColoringStrategy(aPalette);
        case DYNAMIC_PASTELLE:
        case DYNAMIC:
            String[] palette;
            if (SPAN_TYPE.equals(aLayer.getType()) && !hasLinkFeature(aLayer)) {
                palette = PALETTE_NORMAL;
            }
            else {
                // Chains and arcs contain relations that are rendered as lines on the light
                // window background - need to make sure there is some contrast, so we cannot use
                // the full palette.
                palette = PALETTE_NORMAL_FILTERED;
            }
            final String[] aPalette1 = palette;
            return new LabelHashBasedColoringStrategy(aPalette1);
        case GRAY:
        default:
            String[] aPalette2 = { DISABLED };
            return new LabelHashBasedColoringStrategy(aPalette2);
        }
    }

    @Override
    public ColoringStrategyType getBestInitialStrategy(AnnotationLayer aLayer,
            AnnotationPreference aPreferences)
    {
        // Decide on coloring strategy for the current layer
        ColoringStrategyType coloringStrategy;
        if (aLayer.isReadonly()) {
            coloringStrategy = GRAY;
        }
        else if (aPreferences.isStaticColor()) {
            coloringStrategy = STATIC_PASTELLE;
        }
        else {
            coloringStrategy = DYNAMIC;
        }
        return coloringStrategy;
    }

    private boolean hasLinkFeature(AnnotationLayer aLayer)
    {
        for (AnnotationFeature feature : schemaService.listAnnotationFeature(aLayer)) {
            if (!LinkMode.NONE.equals(feature.getLinkMode())) {
                return true;
            }
        }
        return false;
    }

    private String nextPaletteEntry(String[] aPalette, Map<String[], Queue<String>> aPaletteCursors,
            int aThreshold)
    {
        // Initialize the color queue if not already done so
        Queue<String> colorQueue = aPaletteCursors.get(aPalette);
        if (colorQueue == null) {
            colorQueue = new LinkedList<>(asList(aPalette));
            aPaletteCursors.put(aPalette, colorQueue);
        }

        // Look for a suitable color
        String color = colorQueue.poll();
        String firstColor = color;
        while (isTooLight(color, aThreshold)) {
            colorQueue.add(color);
            color = colorQueue.poll();
            // Check if we have seen the same color already (object equality!)
            if (color == firstColor) {
                throw new IllegalStateException("Palette out of colors!");
            }
        }
        colorQueue.add(color);

        return color;
    }

}
