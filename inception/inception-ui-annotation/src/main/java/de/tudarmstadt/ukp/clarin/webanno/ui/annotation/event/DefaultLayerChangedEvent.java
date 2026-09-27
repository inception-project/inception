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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnchoringModePrefs;

public class DefaultLayerChangedEvent
{
    private final AnnotationLayer layer;
    private final AnchoringModePrefs anchoringPrefs;

    public DefaultLayerChangedEvent(AnnotationLayer aLayer, AnchoringModePrefs aAnchoringPrefs)
    {
        super();
        layer = aLayer;
        anchoringPrefs = aAnchoringPrefs;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    /**
     * @return the anchoring mode preferences, so consumers can pick the mode for the new layer
     *         without re-reading them from the database. {@code null} if no layer was chosen.
     */
    public AnchoringModePrefs getAnchoringPrefs()
    {
        return anchoringPrefs;
    }
}
