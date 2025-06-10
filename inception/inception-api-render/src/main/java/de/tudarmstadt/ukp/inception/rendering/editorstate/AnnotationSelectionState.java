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

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;

public interface AnnotationSelectionState
{
    Selection getSelection();

    /**
     * Mark a slot in the given feature state as armed. Note that this feature state does not
     * necessarily belong to the feature states for the annotation detail panel (cf.
     * {@link AnnotatorState#getFeatureStates()}) but may belong to some other feature editor
     * elsewhere in the UI.
     * 
     * @param aState
     *            feature to arm
     * @param aIndex
     *            slot index to arm
     */
    void setArmedSlot(FeatureState aState, int aIndex);

    boolean isArmedSlot(FeatureState aState, int aIndex);

    void clearArmedSlot();

    boolean isSlotArmed();

    FeatureState getArmedFeature();

    int getArmedSlot();

    AnchoringMode getAnchoringMode();

    void setAnchoringMode(AnchoringMode aMode);

    void syncAnchoringModeToDefaultLayer(AnchoringModePrefs aAnchoringPrefs);
}
