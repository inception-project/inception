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
package de.tudarmstadt.ukp.inception.ui.refdoc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

/**
 * Per-user/project layout preferences for the {@link ReferenceDocumentSidebar}. Persisted via the
 * {@link de.tudarmstadt.ukp.inception.preferences.PreferencesService} so the sidebar keeps its
 * state across page reloads and sessions - the counterpart of the main editor's
 * {@code AnnotationPageLayoutState}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceDocumentSidebarState
    implements PreferenceValue
{
    public static final PreferenceKey<ReferenceDocumentSidebarState> KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE = //
            new PreferenceKey<>(ReferenceDocumentSidebarState.class,
                    "annotation/editor/reference-document-sidebar");

    private static final long serialVersionUID = -6155195746441156846L;

    private boolean actionBarCollapsed;

    private boolean scrollSyncEnabled;

    public boolean isActionBarCollapsed()
    {
        return actionBarCollapsed;
    }

    public void setActionBarCollapsed(boolean aActionBarCollapsed)
    {
        actionBarCollapsed = aActionBarCollapsed;
    }

    public boolean isScrollSyncEnabled()
    {
        return scrollSyncEnabled;
    }

    public void setScrollSyncEnabled(boolean aScrollSyncEnabled)
    {
        scrollSyncEnabled = aScrollSyncEnabled;
    }
}
