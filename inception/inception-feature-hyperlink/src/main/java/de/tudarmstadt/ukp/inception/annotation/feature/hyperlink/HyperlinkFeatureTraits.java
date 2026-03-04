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
package de.tudarmstadt.ukp.inception.annotation.feature.hyperlink;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Traits for hyperlink features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperlinkFeatureTraits
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private boolean enabled = false; // Whether hyperlink mode is enabled for this feature
    private boolean allowRelativeUrls = false;
    private boolean requireProtocol = true;
    private Set<String> allowedProtocols = new HashSet<>(Arrays.asList("http", "https"));
    private boolean openInNewTab = true;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isAllowRelativeUrls()
    {
        return allowRelativeUrls;
    }

    public void setAllowRelativeUrls(boolean allowRelativeUrls)
    {
        this.allowRelativeUrls = allowRelativeUrls;
    }

    public boolean isRequireProtocol()
    {
        return requireProtocol;
    }

    public void setRequireProtocol(boolean requireProtocol)
    {
        this.requireProtocol = requireProtocol;
    }

    public Set<String> getAllowedProtocols()
    {
        return allowedProtocols;
    }

    public void setAllowedProtocols(Set<String> allowedProtocols)
    {
        this.allowedProtocols = allowedProtocols;
    }

    public boolean isOpenInNewTab()
    {
        return openInNewTab;
    }

    public void setOpenInNewTab(boolean openInNewTab)
    {
        this.openInNewTab = openInNewTab;
    }
}
