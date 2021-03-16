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
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;

@Component
public class ColoringServiceImpl
    implements ColoringService
{
    private final AnnotationSchemaService schemaService;

    private LoadingCache<AnnotationLayer, Boolean> hasLinkFeatureCache;

    @Autowired
    public ColoringServiceImpl(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;

        hasLinkFeatureCache = Caffeine.newBuilder().expireAfterAccess(5, MINUTES)
                .maximumSize(10 * 1024).build(this::loadHasLinkFeature);
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
        case STATIC_PASTELLE: { // ignore for the moment and fall through
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

            // Limit the palette available to the coloring strategy to a single color - this
            // way we get per-layer coloring instead of per-label coloring.
            String[] aPalette = { nextPaletteEntry(PALETTE_PASTEL, aColorQueues, threshold) };
            return new LabelHashBasedColoringStrategy(aPalette);
        }
        case STATIC: {
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

            // Limit the palette available to the coloring strategy to a single color - this
            // way we get per-layer coloring instead of per-label coloring.
            String[] aPalette = { nextPaletteEntry(PALETTE_NORMAL, aColorQueues, threshold) };
            return new LabelHashBasedColoringStrategy(aPalette);
        }
        case DYNAMIC_PASTELLE: {
            if (SPAN_TYPE.equals(aLayer.getType()) && !hasLinkFeature(aLayer)) {
                return new LabelHashBasedColoringStrategy(PALETTE_PASTEL);
            }

            // Chains and arcs contain relations that are rendered as lines on the light
            // window background - need to make sure there is some contrast, so we cannot use
            // the full palette.
            return new LabelHashBasedColoringStrategy(PALETTE_NORMAL_FILTERED);
        }
        case DYNAMIC: {
            if (SPAN_TYPE.equals(aLayer.getType()) && !hasLinkFeature(aLayer)) {
                return new LabelHashBasedColoringStrategy(PALETTE_NORMAL);
            }

            // Chains and arcs contain relations that are rendered as lines on the light
            // window background - need to make sure there is some contrast, so we cannot use
            // the full palette.
            return new LabelHashBasedColoringStrategy(PALETTE_NORMAL_FILTERED);
        }
        case GRAY:
        default:
            return new LabelHashBasedColoringStrategy(DISABLED);
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
        return hasLinkFeatureCache.get(aLayer);
    }

    private boolean loadHasLinkFeature(AnnotationLayer aLayer)
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

    @EventListener
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        hasLinkFeatureCache.asMap().keySet().removeIf(
                key -> Objects.equals(key.getProject().getId(), aEvent.getProject().getId()));
    }
}
