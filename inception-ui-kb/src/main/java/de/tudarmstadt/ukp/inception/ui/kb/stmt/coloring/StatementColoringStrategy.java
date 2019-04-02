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

import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface StatementColoringStrategy
    extends BeanNameAware
{
    /**
     * @return id
     */
    String getId();

    String getBackgroundColor();

    default String getTextColor()
    {
        String backgroundColor = getBackgroundColor();
        int r = Integer.parseInt(backgroundColor.substring(0, 2), 16);
        int b = Integer.parseInt(backgroundColor.substring(2, 4), 16);
        int g = Integer.parseInt(backgroundColor.substring(4, 6), 16);

        int yiq = (r * 299 + g * 587 + b * 114) / 1000;
        return (yiq >= 128) ? "000000" : "ffffff";
    }

    String getFrameColor();

    boolean acceptsProperty(String aPropertyIdentifier, KnowledgeBase aKB,
            List<String> aLabelProperties);
}
