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
package de.tudarmstadt.ukp.inception.assistant.sidebar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssistantSidebarPrefs
    implements PreferenceValue
{
    private static final long serialVersionUID = -5879519353175794875L;

    public static final PreferenceKey<AssistantSidebarPrefs> KEY_ASSISTANT_SIDEBAR_PREFS = //
            new PreferenceKey<>(AssistantSidebarPrefs.class, "annotation/editor/assistant-sidebar");

    private boolean watchMode;

    public boolean isWatchMode()
    {
        return watchMode;
    }

    public void setWatchMode(boolean aWatchMode)
    {
        watchMode = aWatchMode;
    }
}
