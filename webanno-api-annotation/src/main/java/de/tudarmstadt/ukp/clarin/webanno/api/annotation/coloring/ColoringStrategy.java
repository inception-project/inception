/*
 * Copyright 2014
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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;

public abstract class ColoringStrategy
{
    public static ColoringStrategy labelHashBasedColor(final String[] aPalette)
    {
        return new ColoringStrategy()
        {
            @Override
            public String getColor(VID aVid, String aLabel)
            {
                // If each tag should get a separate color, we currently have no chance other than
                // to derive the color from the actual label text because at this point, we cannot
                // access the tagset information. If we could do that, we could calculate a position
                // within the tag space - at least for those layers that have *only* features with
                // tagsets. For layers that have features without tagsets, again, we can only use
                // the actual label value...
                int colorIndex = Math.abs(aLabel.hashCode());
                if (colorIndex == Integer.MIN_VALUE) {
                    colorIndex = 0;
                }
                return aPalette[colorIndex % aPalette.length];
            }
        };
    }

    public static ColoringStrategy staticColor(final String aColor) {
        return new ColoringStrategy() {
            @Override
            public String getColor(VID aVid, String aLabel)
            {
                return aColor;
            }
        };
    }

    public static ColoringStrategy getBestStrategy(AnnotationSchemaService aService,
            AnnotationLayer aLayer, AnnotationPreference aPreferences,
            Map<String[], Queue<String>> aColorQueues)
    {
        // Decide on coloring strategy for the current layer
        ColoringStrategy coloringStrategy;
        if (aLayer.isReadonly()) {
            coloringStrategy = staticColor(DISABLED);
        }
        else if (aPreferences.isStaticColor()) {
            int threshold;

            if (WebAnnoConst.SPAN_TYPE.equals(aLayer.getType())
                    && !hasLinkFeature(aService, aLayer)) {
                threshold = Integer.MAX_VALUE; // No filtering
            }
            else {
                // Chains and arcs contain relations that are rendered as lines on the light
                // window background - need to make sure there is some contrast, so we cannot use
                // the full palette.
                threshold = LIGHTNESS_FILTER_THRESHOLD;
            }

            coloringStrategy = staticColor(
                    nextPaletteEntry(PALETTE_PASTEL, aColorQueues, threshold));
        }
        else {
            String[] palette;

            if (WebAnnoConst.SPAN_TYPE.equals(aLayer.getType())
                    && !hasLinkFeature(aService, aLayer)) {
                palette = PALETTE_NORMAL;
            }
            else {
                // Chains and arcs contain relations that are rendered as lines on the light
                // window background - need to make sure there is some contrast, so we cannot use
                // the full palette.
                palette = PALETTE_NORMAL_FILTERED;
            }

            coloringStrategy = labelHashBasedColor(palette);
        }
        return coloringStrategy;
    }

    private static boolean hasLinkFeature(AnnotationSchemaService aService, AnnotationLayer aLayer)
    {
        for (AnnotationFeature feature : aService.listAnnotationFeature(aLayer)) {
            if (!LinkMode.NONE.equals(feature.getLinkMode())) {
                return true;
            }
        }
        return false;
    }

    private static String nextPaletteEntry(String[] aPalette,
            Map<String[], Queue<String>> aPaletteCursors, int aThreshold)
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

    /**
     * Filter out too light colors from the palette - those that do not show propely on a ligth
     * background. The threshold controls what to filter.
     *
     * @param aPalette
     *            the palette.
     * @param aThreshold
     *            the lightness threshold (0 = black, 255 = white)
     * @return the filtered palette.
     */
    public static String[] filterLightColors(String[] aPalette, int aThreshold)
    {
        List<String> filtered = new ArrayList<>();
        for (String color : aPalette) {
            if (!isTooLight(color, aThreshold)) {
                filtered.add(color);
            }
        }
        return filtered.toArray(new String[filtered.size()]);
    }

    public static boolean isTooLight(String aColor, int aThreshold)
    {
        // http://24ways.org/2010/calculating-color-contrast/
        // http://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
        int r = Integer.valueOf(aColor.substring(1, 3), 16);
        int g = Integer.valueOf(aColor.substring(3, 5), 16);
        int b = Integer.valueOf(aColor.substring(5, 7), 16);
        int yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return yiq > aThreshold;
    }

    private final static int LIGHTNESS_FILTER_THRESHOLD = 180;

    public final static String DISABLED = "#bebebe";

    public final static String[] PALETTE_PASTEL = { "#8dd3c7", "#ffffb3", "#bebada", "#fb8072",
            "#80b1d3", "#fdb462", "#b3de69", "#fccde5", "#d9d9d9", "#bc80bd", "#ccebc5",
            "#ffed6f" };

    // public final static String[] PALETTE_PASTEL_FILTERED = filterLightColors(PALETTE_PASTEL,
    // LIGHTNESS_FILTER_THRESHOLD);

    public final static String[] PALETTE_NORMAL = { "#a6cee3", "#1f78b4", "#b2df8a", "#33a02c",
            "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99",
            "#b15928" };

    public final static String[] PALETTE_NORMAL_FILTERED = filterLightColors(PALETTE_NORMAL,
            LIGHTNESS_FILTER_THRESHOLD);

    public abstract String getColor(VID aVid, String aLabel);
}
