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
package de.tudarmstadt.ukp.inception.ui.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("style.header")
public class HeaderLinkPropertiesImpl
    implements HeaderLinkSettings
{
    /**
     * Icons/links to display in the page header. Each entry is keyed by an identifier and contains
     * a {@code linkUrl} (target page) and an {@code imageUrl} (icon image). Images are
     * automatically resized via CSS; point to a reasonably small image to keep loading times low.
     * The order of the icons is controlled by the ID, not by the order in the configuration file.
     * {@code imageUrl} values may use a special {@code file:} prefix to refer to an image file
     * placed under the {@code ${inception.home}/public} directory.
     */
    private Map<String, HeaderLink> icon = new HashMap<>();

    @Override
    public Map<String, HeaderLink> getIcon()
    {
        return icon;
    }

    @Override
    public void setIcon(Map<String, HeaderLink> aIcon)
    {
        icon = aIcon;
    }
}
