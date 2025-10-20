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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.page;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationEditorManagerPrefs
    implements PreferenceValue
{
    private static final long serialVersionUID = -1637731874872789592L;

    public static final PreferenceKey<AnnotationEditorManagerPrefs> KEY_ANNOTATION_EDITOR_MANAGER_PREFS = new PreferenceKey<>(
            AnnotationEditorManagerPrefs.class, "annotation/editor");

    private String defaultEditor;

    private boolean preferencesAccessAllowed = true;

    private boolean showDeleteAnnotationConfirmation = true;

    public boolean isShowDeleteAnnotationConfirmation()
    {
        return showDeleteAnnotationConfirmation;
    }

    public void setShowDeleteAnnotationConfirmation(boolean aShowDeleteAnnotationConfirmation)
    {
        showDeleteAnnotationConfirmation = aShowDeleteAnnotationConfirmation;
    }

    public String getDefaultEditor()
    {
        return defaultEditor;
    }

    public void setDefaultEditor(String aEditorId)
    {
        defaultEditor = aEditorId;
    }

    public boolean isPreferencesAccessAllowed()
    {
        return preferencesAccessAllowed;
    }

    public void setPreferencesAccessAllowed(boolean aPreferencesAccessAllowed)
    {
        preferencesAccessAllowed = aPreferencesAccessAllowed;
    }
}
