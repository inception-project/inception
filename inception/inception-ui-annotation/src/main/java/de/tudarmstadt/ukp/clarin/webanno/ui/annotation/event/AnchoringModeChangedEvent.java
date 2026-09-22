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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs;

/**
 * The user changed the anchoring mode for a layer.
 */
public class AnchoringModeChangedEvent
{
    private final AnnotationLayer layer;
    private final AnchoringMode mode;
    private final AnchoringModePrefs prefs;

    public AnchoringModeChangedEvent(AnnotationLayer aLayer, AnchoringMode aMode,
            AnchoringModePrefs aPrefs)
    {
        layer = aLayer;
        mode = aMode;
        prefs = aPrefs;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public AnchoringMode getMode()
    {
        return mode;
    }

    /**
     * @return the freshly saved preferences, so consumers can sync their state without re-reading
     *         them from the database.
     */
    public AnchoringModePrefs getPrefs()
    {
        return prefs;
    }
}
