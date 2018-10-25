package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.springframework.stereotype.Component;

@Component
public class LabelColoringStrategyImpl implements StatementColoringStrategy
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
        return "EF4444";
    }

    @Override
    public String getFrameColor() {
        return "f0ffff";
    }

    @Override
    public boolean acceptsProperty(String aPropertyIdentifier)
    {
        return aPropertyIdentifier.equals(RDFS.LABEL.stringValue());
    }
}
