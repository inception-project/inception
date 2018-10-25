package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import de.tudarmstadt.ukp.inception.ui.kb.value.ValueTypeSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatementColoringRegistryImpl implements StatementColoringRegistry
{
    List<StatementColoringStrategy> statementColoringStrategies;

    public StatementColoringRegistryImpl(@Lazy @Autowired(required = false)
        List<StatementColoringStrategy> aStatementColoringStrategies)
    {
        statementColoringStrategies = aStatementColoringStrategies;
    }

    @Override
    public List<StatementColoringStrategy> getStatementColoringStrategies()
    {
        return statementColoringStrategies;
    }

    @Override
    public StatementColoringStrategy getStatementColoringStrategy(
        String aPropertyIdentifier)
    {
        for (StatementColoringStrategy coloringStrategy : getStatementColoringStrategies()) {
            if (coloringStrategy.acceptsProperty(aPropertyIdentifier)
                && !(coloringStrategy instanceof DefaultColoringStrategyImpl)) {
                return coloringStrategy;
            }
        }
        return new DefaultColoringStrategyImpl();
    }
}
