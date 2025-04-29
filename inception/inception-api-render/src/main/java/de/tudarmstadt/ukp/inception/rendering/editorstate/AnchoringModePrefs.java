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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnchoringModePrefs
    implements PreferenceValue
{
    private static final long serialVersionUID = 8420554954400084375L;

    public static final PreferenceKey<AnchoringModePrefs> KEY_ANCHORING_MODE = new PreferenceKey<>(
            AnchoringModePrefs.class, "annotation/layer-anchoring-mode");

    private final Map<Long, AnchoringMode> anchoringModes = new LinkedHashMap<>();

    public void setAnchoringModes(AnnotationLayer aLayer, AnchoringMode aMode)
    {
        anchoringModes.put(aLayer.getId(), aMode);
    }

    public Optional<AnchoringMode> getAnchoringMode(AnnotationLayer aLayer)
    {
        return Optional.ofNullable(anchoringModes.get(aLayer.getId()));
    }

    public void setAnchoringModes(Map<Long, AnchoringMode> aModes)
    {
        anchoringModes.clear();
        if (aModes != null) {
            anchoringModes.putAll(aModes);
        }
    }

    public Map<Long, AnchoringMode> getAnchoringModes()
    {
        return anchoringModes;
    }
}
