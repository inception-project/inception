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
package de.tudarmstadt.ukp.inception.curation.sidebar;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link CurationServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("curation.sidebar")
public class CurationSidebarPropertiesImpl
    implements CurationSidebarProperties
{
    private boolean enabled;

    private boolean ownUserCurationTargetEnabled;

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    @Override
    public boolean isOwnUserCurationTargetEnabled()
    {
        return ownUserCurationTargetEnabled;
    }

    public void setOwnUserCurationTargetEnabled(boolean aOwnUserCurationTargetEnabled)
    {
        ownUserCurationTargetEnabled = aOwnUserCurationTargetEnabled;
    }
}
