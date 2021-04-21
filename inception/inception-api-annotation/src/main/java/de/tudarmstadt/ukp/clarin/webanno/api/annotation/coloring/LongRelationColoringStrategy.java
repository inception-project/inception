package de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_TAB20;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;

public class LongRelationColoringStrategy
    implements ColoringStrategy
{
    private final String[] palette;

    public LongRelationColoringStrategy(String... aPalette)
    {
        palette = aPalette;
    }

    @Override
    public String getColor(VObject aVObject, String aLabel, ColoringRules aRules)
    {
        if (aVObject.getColorHint() != null) {
            return aVObject.getColorHint();
        }

        if (aVObject instanceof VSpan) {
            if (aVObject.getEquivalenceSet() >= 0) {
                // We do not render relation arcs, but have the span in the same color
                return PALETTE_TAB20[aVObject.getEquivalenceSet() % PALETTE_TAB20.length];
            }
            else {
                return "#ffffff";
            }
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
