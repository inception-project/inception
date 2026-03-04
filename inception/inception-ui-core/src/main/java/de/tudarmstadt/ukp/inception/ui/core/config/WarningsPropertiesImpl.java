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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("warnings")
public class WarningsPropertiesImpl
    implements WarningsProperties
{
    /**
     * If true the embedded database warning is shown. Default true to preserve current behavior.
     */
    private boolean embeddedDatabase = true;

    /**
     * If true the unsupported browser warning is shown. Default true to preserve current behavior.
     */
    private boolean unsupportedBrowser = true;

    @Override
    public boolean isEmbeddedDatabase()
    {
        return embeddedDatabase;
    }

    public void setEmbeddedDatabase(boolean aEmbeddedDatabase)
    {
        embeddedDatabase = aEmbeddedDatabase;
    }

    @Override
    public boolean isUnsupportedBrowser()
    {
        return unsupportedBrowser;
    }

    public void setUnsupportedBrowser(boolean aUnsupportedBrowser)
    {
        unsupportedBrowser = aUnsupportedBrowser;
    }
}
