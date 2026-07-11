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
package de.tudarmstadt.ukp.inception.curation.model;

import static java.util.Collections.unmodifiableSet;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

/**
 * Persistent, per-user, per-project state of a curation session. Stored as JSON traits via the
 * preferences subsystem, which replaces the former {@code CurationSettings} entity (and its
 * {@code curation_settings} / {@code curationSettings_users} tables). A user with no stored
 * preferences therefore starts with an empty {@link #deselectedDataOwners} set, i.e. all annotators
 * selected by default.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurationSessionPreferences
    implements PreferenceValue
{
    private static final long serialVersionUID = 4783653871283404200L;

    public static final PreferenceKey<CurationSessionPreferences> KEY_CURATION_SESSION = //
            new PreferenceKey<>(CurationSessionPreferences.class, "curation/session");

    private String curationTarget;

    /**
     * The data owners the session owner has explicitly deselected. Everything not listed here
     * counts as selected, so an empty set means "curate all annotators".
     */
    private final Set<String> deselectedDataOwners = new LinkedHashSet<>();

    public String getCurationTarget()
    {
        return curationTarget;
    }

    public void setCurationTarget(String aCurationTarget)
    {
        curationTarget = aCurationTarget;
    }

    public Set<String> getDeselectedDataOwners()
    {
        return unmodifiableSet(deselectedDataOwners);
    }

    public void setDeselectedDataOwners(Set<String> aDeselectedDataOwners)
    {
        deselectedDataOwners.clear();
        if (aDeselectedDataOwners != null) {
            deselectedDataOwners.addAll(aDeselectedDataOwners);
        }
    }
}
