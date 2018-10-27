package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import org.springframework.beans.factory.BeanNameAware;

public interface StatementColoringStrategy extends BeanNameAware
{

    /**
     * @return id
     */
    String getId();

    String getBackgroundColor();

    default String getTextColor() {
        String backgroundColor = getBackgroundColor();
        int r = Integer.parseInt(backgroundColor.substring(0, 2), 16);
        int b = Integer.parseInt(backgroundColor.substring(2, 4), 16);
        int g = Integer.parseInt(backgroundColor.substring(4, 6), 16);

        int yiq = (r * 299 + g * 587 + b * 114) / 1000;
        return (yiq >= 128) ? "000000" : "ffffff";
    }

    String getFrameColor();

    boolean acceptsProperty(String aPropertyIdentifier);
}
