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
package de.tudarmstadt.ukp.inception.ui.curation.editor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitCurationEditorLayoutState
    implements PreferenceValue
{
    public static final PreferenceKey<SplitCurationEditorLayoutState> KEY_SPLIT_CURATION_EDITOR_LAYOUT_STATE = //
            new PreferenceKey<>(SplitCurationEditorLayoutState.class,
                    "annotation/editor/split-curation");

    private static final long serialVersionUID = -6799745092881862176L;

    public static final int EDITOR_SIZE_MIN = 10;
    public static final int EDITOR_SIZE_MAX = 90;
    public static final int EDITOR_SIZE_DEFAULT = 50;

    private double editorSize;

    public double getEditorSize()
    {
        if (editorSize < EDITOR_SIZE_MIN || editorSize > EDITOR_SIZE_MAX) {
            return EDITOR_SIZE_DEFAULT;
        }

        return editorSize;
    }

    public void setEditorSize(double aSize)
    {
        double clamped;
        if (aSize > EDITOR_SIZE_MAX) {
            clamped = EDITOR_SIZE_MAX;
        }
        else if (aSize < EDITOR_SIZE_MIN) {
            clamped = EDITOR_SIZE_MIN;
        }
        else {
            clamped = aSize;
        }

        editorSize = Math.round(clamped * 10_000d) / 10_000d;
    }
}
