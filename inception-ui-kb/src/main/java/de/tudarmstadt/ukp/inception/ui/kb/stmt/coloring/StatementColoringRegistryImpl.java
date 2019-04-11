/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

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
        String aPropertyIdentifier, KnowledgeBase aKB, List<String> aLabelProperties)
    {
        for (StatementColoringStrategy coloringStrategy : getStatementColoringStrategies()) {
            if (coloringStrategy.acceptsProperty(aPropertyIdentifier, aKB, aLabelProperties)
                && !(coloringStrategy instanceof DefaultColoringStrategyImpl)) {
                return coloringStrategy;
            }
        }
        return new DefaultColoringStrategyImpl();
    }
}
