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

import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_DEFAULT;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_MAX;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_MIN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationPageLayoutState
    implements PreferenceValue
{
    public static final PreferenceKey<AnnotationPageLayoutState> KEY_LAYOUT_STATE = new PreferenceKey<>(
            AnnotationPageLayoutState.class, "annotation/layout");

    private static final long serialVersionUID = 4751157181917255392L;

    private int sidebarSizeLeft;
    private int sidebarSizeRight;

    public int getSidebarSizeLeft()
    {
        if (sidebarSizeLeft < SIDEBAR_SIZE_MIN || sidebarSizeLeft > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSizeLeft;
        }
    }

    public void setSidebarSizeLeft(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSizeLeft = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSizeLeft = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSizeLeft = aSidebarSize;
        }
    }

    public int getSidebarSizeRight()
    {
        if (sidebarSizeRight < SIDEBAR_SIZE_MIN || sidebarSizeRight > SIDEBAR_SIZE_MAX) {
            return SIDEBAR_SIZE_DEFAULT;
        }
        else {
            return sidebarSizeRight;
        }
    }

    public void setSidebarSizeRight(int aSidebarSize)
    {
        if (aSidebarSize > SIDEBAR_SIZE_MAX) {
            sidebarSizeRight = SIDEBAR_SIZE_MAX;
        }
        else if (aSidebarSize < SIDEBAR_SIZE_MIN) {
            sidebarSizeRight = SIDEBAR_SIZE_MIN;
        }
        else {
            sidebarSizeRight = aSidebarSize;
        }
    }
}
