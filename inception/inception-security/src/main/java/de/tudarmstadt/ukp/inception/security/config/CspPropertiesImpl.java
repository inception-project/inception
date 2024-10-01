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
package de.tudarmstadt.ukp.inception.security.config;

import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.csp")
public class CspPropertiesImpl
    implements CspProperties
{
    private List<String> allowedImageSources;
    private List<String> allowedMediaSources;

    private List<String> allowedFrameAncestors;

    @Override
    public List<String> getAllowedImageSources()
    {
        if (allowedImageSources == null) {
            return emptyList();
        }

        return allowedImageSources;
    }

    public void setAllowedImageSources(List<String> aAllowedSources)
    {
        allowedImageSources = aAllowedSources;
    }

    @Override
    public List<String> getAllowedMediaSources()
    {
        if (allowedMediaSources == null) {
            return emptyList();
        }

        return allowedMediaSources;
    }

    public void setAllowedMediaSources(List<String> aAllowedSources)
    {
        allowedMediaSources = aAllowedSources;
    }

    @Override
    public List<String> getAllowedFrameAncestors()
    {
        if (allowedFrameAncestors == null) {
            return emptyList();
        }

        return allowedFrameAncestors;
    }

    public void setAllowedFrameAncestors(List<String> aAllowedFrameAncestors)
    {
        allowedFrameAncestors = aAllowedFrameAncestors;
    }
}
