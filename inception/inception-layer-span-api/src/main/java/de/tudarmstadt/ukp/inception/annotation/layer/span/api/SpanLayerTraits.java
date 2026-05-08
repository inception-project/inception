/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.annotation.layer.span.api;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringRules;

public class SpanLayerTraits
    implements Serializable, ColoringRulesTrait
{
    private static final long serialVersionUID = 3461537626173105320L;

    private ColoringRules coloringRules = new ColoringRules();

    public SpanLayerTraits()
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
