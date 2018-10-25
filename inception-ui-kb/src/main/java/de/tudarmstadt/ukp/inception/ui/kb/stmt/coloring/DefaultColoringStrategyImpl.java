package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import org.springframework.stereotype.Component;

@Component
public class DefaultColoringStrategyImpl implements StatementColoringStrategy
{

    private String coloringStrategyId;

    @Override
    public String getId()
    {
        return coloringStrategyId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        coloringStrategyId = aBeanName;
    }

    @Override
    public String getBackgroundColor()
    {
        return "ffffff";
    }

    @Override
    public String getFrameColor() {
        return "f0f8ff";
    }

    @Override
    public boolean acceptsProperty(String aPropertyIdentifier)
    {
        return true;
    }

}
