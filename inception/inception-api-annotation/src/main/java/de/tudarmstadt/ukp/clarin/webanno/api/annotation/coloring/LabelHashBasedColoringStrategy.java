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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_NORMAL_FILTERED;

import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringRules;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;

public class LabelHashBasedColoringStrategy
    implements ColoringStrategy
{
    private final String[] palette;

    public LabelHashBasedColoringStrategy(String... aPalette)
    {
        palette = aPalette;
    }

    @Override
    public String getColor(VObject aVObject, String aLabel, ColoringRules aRules)
    {
        if (aVObject.getColorHint() != null) {
            return aVObject.getColorHint();
        }

        if (aVObject.getEquivalenceSet() >= 0) {
            // Every chain is supposed to have a different color
            return PALETTE_NORMAL_FILTERED[aVObject.getEquivalenceSet()
                    % PALETTE_NORMAL_FILTERED.length];
        }

        if (aRules != null) {
            String ruleBasedColor = aRules.findColor(aLabel);
            if (ruleBasedColor != null) {
                return ruleBasedColor;
            }
        }

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
        return palette[colorIndex % palette.length];
    }
}
