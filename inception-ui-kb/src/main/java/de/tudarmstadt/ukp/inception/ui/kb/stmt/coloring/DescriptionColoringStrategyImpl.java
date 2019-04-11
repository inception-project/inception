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

import java.util.List;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component
public class DescriptionColoringStrategyImpl
    implements StatementColoringStrategy
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
    public String getFrameColor()
    {
        return "f0e68c";
    }

    @Override
    public boolean acceptsProperty(String aPropertyIdentifier, KnowledgeBase aKB,
            List<String> aLabelProperties)
    {
        return aPropertyIdentifier.equals(aKB.getDescriptionIri().stringValue());
    }
}
