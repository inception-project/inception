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

import java.util.ArrayList;
import java.util.List;

public class ColoringUtils
{
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

    /**
     * Filter out too light colors from the palette - those that do not show properly on a light
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
}
