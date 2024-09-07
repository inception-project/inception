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
package de.tudarmstadt.ukp.clarin.webanno.brat.preferences;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.AnnotationEditorDefaultPreferencesProperties;
import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BratAnnotationEditorManagerPrefs
    implements PreferenceValue
{
    private static final long serialVersionUID = 4740468259864549184L;

    private boolean changingScriptDirectionAllowed = false;

    private int defaultPageSize;

    public static final PreferenceKey<BratAnnotationEditorManagerPrefs> KEY_BRAT_EDITOR_MANAGER_PREFS = //
            new PreferenceKey<>(BratAnnotationEditorManagerPrefs.class,
                    "annotation/editor/brat/manager");

    public BratAnnotationEditorManagerPrefs()
    {
        AnnotationEditorDefaultPreferencesProperties defaults = ApplicationContextProvider
                .getApplicationContext()
                .getBean(AnnotationEditorDefaultPreferencesProperties.class);
        defaultPageSize = defaults.getPageSize();
    }

    public boolean isChangingScriptDirectionAllowed()
    {
        return changingScriptDirectionAllowed;
    }

    public void setChangingScriptDirectionAllowed(boolean aChangingScriptDirectionAllowed)
    {
        changingScriptDirectionAllowed = aChangingScriptDirectionAllowed;
    }

    public int getDefaultPageSize()
    {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int aDefaultPageSize)
    {
        defaultPageSize = aDefaultPageSize;
    }
}
