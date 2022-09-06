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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringUtils.filterLightColors;

public class Palette
{
    public final static int LIGHTNESS_FILTER_THRESHOLD = 180;

    public final static String DISABLED = "#bebebe";

    public final static String[] PALETTE_PASTEL = {
            // The names behind each color are approximations obtained via
            // http://chir.ag/projects/name-that-color
            "#8dd3c7", // Monte Carlo - bluish
            "#ffffb3", // Portafino - yellowish
            "#bebada", // Lavender Gray
            "#fb8072", // Salmon
            "#80b1d3", // Half Baked - bluish
            // "#fdb462", // Koromiko - peachy - too close to the FOCUS marker color
            "#b3de69", // Yellow Green
            "#fccde5", // Classic Rose
            // Grey colors reserved for special purposes, e.g. for read-only
            // "#d9d9d9", // Alto - grey
            "#bc80bd", // Wisteria - purpleish
            "#ccebc5", // Peppermint
            "#ffed6f" }; // Kournikova - yellowish

    // public final static String[] PALETTE_PASTEL_FILTERED = filterLightColors(PALETTE_PASTEL,
    // LIGHTNESS_FILTER_THRESHOLD);

    public final static String[] PALETTE_NORMAL = { //
            "#a6cee3", // Regent St Blue
            "#1f78b4", // Matisse - dark bluish
            "#b2df8a", // Feijoa - greenish
            "#33a02c", // Forest Green
            "#fb9a99", // Sweet Pink
            "#e31a1c", // Alizarin Crimson
            "#fdbf6f", // Macaroni and Cheese - peachy
            // "#ff7f00", // Flush Orange - too close to the FOCUS marker color
            "#cab2d6", // Lavender Gray
            "#6a3d9a", // Royal Purple
            "#ffff99", // Pale Canary
            "#b15928" }; // Paarl - brownish

    public final static String[] PALETTE_NORMAL_FILTERED = filterLightColors(PALETTE_NORMAL,
            LIGHTNESS_FILTER_THRESHOLD);
}
