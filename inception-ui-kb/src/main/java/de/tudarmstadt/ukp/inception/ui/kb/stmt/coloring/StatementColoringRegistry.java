package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import java.util.List;

public interface StatementColoringRegistry
{
    List<StatementColoringStrategy> getStatementColoringStrategies();

    StatementColoringStrategy getStatementColoringStrategy(String aPropertyIdentifier);
}
