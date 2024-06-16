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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UIState
    implements PreferenceValue
{
    public static final PreferenceKey<UIState> KEY_UI = new PreferenceKey<>(UIState.class,
            "global/ui");

    public static final String LIGHT_THEME = "light";
    public static final String DARK_THEME = "dark";
    public static final String DEFAULT_THEME = LIGHT_THEME;

    private String theme;

    public UIState()
    {
        theme = DEFAULT_THEME;
    }

    public UIState(String aTheme)
    {
        theme = aTheme;
    }

    public String getTheme()
    {
        return theme;
    }

    public void setTheme(String aTheme)
    {
        theme = aTheme;
    }

    @Override
    public String toString()
    {
        return "UIState{" + "theme=" + theme + '}';
    }
}
