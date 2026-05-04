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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dashboard.admin-settings")
public class SettingsPagePropertiesImpl
    implements SettingsPageProperties
{
    /**
     * Namespaces hidden from the admin Settings page tree. Useful for suppressing properties that
     * are framework- or infrastructure-related rather than application configuration. An entry can
     * be a single segment (e.g. {@code "spring"}) to hide everything below it, or a dotted
     * multi-segment prefix (e.g. {@code "telemetry.matomo"}) to hide only that subtree. A property
     * is hidden when its name equals an entry, or starts with an entry followed by a dot. Matching
     * is case-sensitive.
     */
    // Default declared in META-INF/additional-spring-configuration-metadata.json
    // because the metadata processor cannot evaluate the LinkedHashSet/List.of expression.
    private Set<String> hiddenNamespaces = new LinkedHashSet<>(List.of( //
            "spring", //
            "springdoc", //
            "wicket", //
            "sentry", //
            "management", //
            "server", //
            "endpoints", //
            "logging", //
            "telemetry.matomo"));

    @Override
    public Set<String> getHiddenNamespaces()
    {
        return hiddenNamespaces;
    }

    public void setHiddenNamespaces(Set<String> aHiddenNamespaces)
    {
        hiddenNamespaces = aHiddenNamespaces != null ? aHiddenNamespaces : new LinkedHashSet<>();
    }
}
