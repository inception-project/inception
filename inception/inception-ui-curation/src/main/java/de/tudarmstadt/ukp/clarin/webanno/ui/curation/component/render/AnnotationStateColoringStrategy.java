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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render;

import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState.ERROR;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringRules;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;

public class AnnotationStateColoringStrategy
    implements ColoringStrategy
{
    private final Map<VID, AnnotationState> annotationStates;

    public AnnotationStateColoringStrategy(Map<VID, AnnotationState> aAnnotationStates)
    {
        Validate.notNull(aAnnotationStates, "Parameter [aAnnotationStates] must not be null");

        annotationStates = aAnnotationStates;
    }

    @Override
    public String getColor(VObject aVObject, String aLabel, ColoringRules aColoringRules)
    {
        AnnotationState state = annotationStates.getOrDefault(aVObject.getVid(), ERROR);
        return state.getColorCode();
    }
}
