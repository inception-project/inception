package de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;

public class RelationColoringStrategy
    implements ColoringStrategy
{
    private final String color;

    public RelationColoringStrategy(String aColor)
    {
        super();
        color = aColor;
    }

    @Override
    public String getColor(VObject aVObject, String aLabel, ColoringRules aRules)
    {
        if (aVObject instanceof VArc) {
            VArc arc = (VArc) aVObject;
            arc.getSource();
            arc.getTarget();
        }

        return color;
    }

}
