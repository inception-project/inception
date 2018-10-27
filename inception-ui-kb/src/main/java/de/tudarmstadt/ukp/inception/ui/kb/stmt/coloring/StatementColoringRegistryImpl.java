package de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class StatementColoringRegistryImpl implements StatementColoringRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<StatementColoringStrategy> statementColoringStrategiesProxy;

    private List<StatementColoringStrategy> statementColoringStrategies;

    public StatementColoringRegistryImpl(@Lazy @Autowired(required = false)
        List<StatementColoringStrategy> aStatementColoringStrategies)
    {
        statementColoringStrategiesProxy = aStatementColoringStrategies;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    public void init()
    {
        List<StatementColoringStrategy> fsp = new ArrayList<>();

        if (statementColoringStrategiesProxy != null) {
            fsp.addAll(statementColoringStrategiesProxy);
            AnnotationAwareOrderComparator.sort(fsp);

            for (StatementColoringStrategy fs : fsp) {
                log.info("Found value type support: {}",
                    ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        statementColoringStrategies = Collections.unmodifiableList(fsp);
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
