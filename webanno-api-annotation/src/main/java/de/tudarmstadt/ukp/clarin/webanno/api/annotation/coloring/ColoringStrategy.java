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
    public enum ReadonlyColoringBehaviour
    {
        LEGACY("legacy " + ColoringStrategyType.GRAY.getDescriptiveName(),
                ColoringStrategyType.GRAY), 
        NORMAL("normal", null), 
        GRAY(ColoringStrategyType.GRAY.getDescriptiveName(), 
                ColoringStrategyType.GRAY),
        // here could be more
        ;

        private String descriptiveName;
        private ColoringStrategyType t;

        private ReadonlyColoringBehaviour(String descriptiveName, ColoringStrategyType t)
        {
            this.descriptiveName = descriptiveName;
            this.t = t;
        }

        public ColoringStrategyType getColoringStrategy()
        {
            return t;
        }

        public String getDescriptiveName()
        {
            return descriptiveName;
        }

    }

    public enum ColoringStrategyType
    {

        STATIC("static"), STATIC_PASTELLE("static pastelle"),

        DYNAMIC("dynamic"), DYNAMIC_PASTELLE("dynamic pastelle"),

        GRAY("static gray"),

        LEGACY("legacy"),

        ;

        private String descriptiveName;

        private ColoringStrategyType(String descriptiveName)
        {
            this.descriptiveName = descriptiveName;
        }

        public String getDescriptiveName()
        {
            return descriptiveName;
        }

    }

    public static ColoringStrategy staticColor(final String aColor)
    {
        return new ColoringStrategy()
        {
            @Override
            public String getColor(VID aVid, String aLabel)
            {
                return aColor;
            }
        };
    }
    
    public static final ColoringStrategy labelHashBasedColor(final String... aPalette)
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
                    // Math.abs(Integer.MIN_VALUE) = Integer.MIN_VALUE - we need to catch this
                    // case here.
                    colorIndex = 0;
                }
                return aPalette[colorIndex % aPalette.length];
            }
        };
    }

    public static ColoringStrategy getStrategy(AnnotationSchemaService aService,
            AnnotationLayer aLayer, AnnotationPreference aPreferences,
            Map<String[], Queue<String>> aColorQueues)
    {
        ColoringStrategyType t = aPreferences.getColorPerLayer().get(aLayer.getId());
        ReadonlyColoringBehaviour rt = aPreferences.getReadonlyLayerColoringBehaviour();
        if (aLayer.isReadonly() && rt != ReadonlyColoringBehaviour.NORMAL) {
            t = rt.t;
        }
        if (t == null || t == ColoringStrategyType.LEGACY) {
            t = getBestInitialStrategy(aService, aLayer, aPreferences);
        }
        return getStrategy(aService, aLayer, t, aColorQueues);
    }

    public static ColoringStrategy getStrategy(AnnotationSchemaService aService,
            AnnotationLayer aLayer, ColoringStrategyType colortype,
            Map<String[], Queue<String>> aColorQueues)
    {
        // Decide on coloring strategy for the current layer
        switch (colortype) {
        case STATIC_PASTELLE: // ignore for the moment and fall through
        case STATIC:
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
            return labelHashBasedColor(nextPaletteEntry(PALETTE_PASTEL, aColorQueues, threshold));
        case DYNAMIC_PASTELLE:
        case DYNAMIC:
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
            return labelHashBasedColor(palette);
        case GRAY:
        default:
            return labelHashBasedColor(DISABLED);
        }
    }

    public static ColoringStrategyType getBestInitialStrategy(AnnotationSchemaService aService,
            AnnotationLayer aLayer, AnnotationPreference aPreferences)
    {
        // Decide on coloring strategy for the current layer
        ColoringStrategyType coloringStrategy;
        if (aLayer.isReadonly()) {
            coloringStrategy = ColoringStrategyType.GRAY;
        }
        else if (aPreferences.isStaticColor()) {
            coloringStrategy = ColoringStrategyType.STATIC_PASTELLE;
        }
        else {
            coloringStrategy = ColoringStrategyType.DYNAMIC;
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

    public final static String[] PALETTE_PASTEL = { 
            // The names behind each color are approximations obtained via 
            // http://chir.ag/projects/name-that-color
            "#8dd3c7",   // Monte Carlo - bluish
            "#ffffb3",   // Portafino - yellowish
            "#bebada",   // Lavender Gray
            "#fb8072",   // Salmon
            "#80b1d3",   // Half Baked - bluish
            "#fdb462",   // Koromiko - peachy
            "#b3de69",   // Yellow Green
            "#fccde5",   // Classic Rose
            // Grey colors reserved for special purposes, e.g. for read-only
            // "#d9d9d9",   // Alto - grey
            "#bc80bd",   // Wisteria - purpleish
            "#ccebc5",   // Peppermint
            "#ffed6f" }; // Kournikova - yellowish
    

    // public final static String[] PALETTE_PASTEL_FILTERED = filterLightColors(PALETTE_PASTEL,
    // LIGHTNESS_FILTER_THRESHOLD);

    public final static String[] PALETTE_NORMAL = { 
            "#a6cee3",   // Regent St Blue
            "#1f78b4",   // Matisse - dark bluish
            "#b2df8a",   // Feijoa - greenish
            "#33a02c",   // Forest Green
            "#fb9a99",   // Sweet Pink
            "#e31a1c",   // Alizarin Crimson
            "#fdbf6f",   // Macaroni and Cheese - peachy
            "#ff7f00",   // Flush Orange
            "#cab2d6",   // Lavender Gray
            "#6a3d9a",   // Royal Purple
            "#ffff99",   // Pale Canary
            "#b15928" }; // Paarl - brownish

    public final static String[] PALETTE_NORMAL_FILTERED = filterLightColors(PALETTE_NORMAL,
            LIGHTNESS_FILTER_THRESHOLD);

    public abstract String getColor(VID aVid, String aLabel);
}
