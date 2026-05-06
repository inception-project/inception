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

import java.util.Map;

/**
 * Configuration contract for header icon links displayed in the page header.
 */
public interface HeaderLinkSettings
{
    /**
     * Map of icon id -> header link specification.
     *
     * @return map of icons
     */
    Map<String, HeaderLink> getIcon();

    /**
     * Set the icon map.
     *
     * @param aIcon
     *            the icons map
     */
    void setIcon(Map<String, HeaderLink> aIcon);

    /**
     * Simple DTO describing a header icon link.
     *
     * <ul>
     * <li>{@code linkUrl} - target URL when the icon is clicked</li>
     * <li>{@code imageUrl} - URL or path to the icon image</li>
     * </ul>
     */
    public static class HeaderLink
    {
        private String linkUrl;
        private String imageUrl;

        /**
         * @return the target link URL (may be absolute or relative)
         */
        public String getLinkUrl()
        {
            return linkUrl;
        }

        /**
         * @param aLinkUrl
         *            the target link URL to set
         */
        public void setLinkUrl(String aLinkUrl)
        {
            linkUrl = aLinkUrl;
        }

        /**
         * @return the image URL or path for the icon
         */
        public String getImageUrl()
        {
            return imageUrl;
        }

        /**
         * @param aImageUrl
         *            the image URL or path to set
         */
        public void setImageUrl(String aImageUrl)
        {
            imageUrl = aImageUrl;
        }
    }
}
