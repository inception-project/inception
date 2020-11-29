/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRules;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;

public class RelationLayerTraits
    implements Serializable, ColoringRulesTrait
{
    private static final long serialVersionUID = 3461537626173105320L;

    private ColoringRules coloringRules = new ColoringRules();

    public RelationLayerTraits()
    {
        // Nothing to do
    }

    @Override
    public ColoringRules getColoringRules()
    {
        return coloringRules;
    }

    @Override
    public void setColoringRules(ColoringRules aColoringRules)
    {
        coloringRules = aColoringRules;
    }
}
