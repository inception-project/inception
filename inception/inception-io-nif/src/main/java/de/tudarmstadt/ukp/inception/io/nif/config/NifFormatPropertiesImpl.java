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
package de.tudarmstadt.ukp.inception.io.nif.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("format.nif")
public class NifFormatPropertiesImpl
    implements NifFormatProperties
{
    private static final String DEFAULT_PREFIX = "urn:inception:";

    private String defaultClassIri = DEFAULT_PREFIX;
    private String defaultIdentifierIri = DEFAULT_PREFIX;
    private String stripClassIri = DEFAULT_PREFIX;
    private String stripIdentifierIri = DEFAULT_PREFIX;

    @Override
    public String getDefaultClassIri()
    {
        return defaultClassIri;
    }

    public void setDefaultClassIri(String aDefaultClassIri)
    {
        defaultClassIri = aDefaultClassIri;
    }

    @Override
    public String getDefaultIdentifierIri()
    {
        return defaultIdentifierIri;
    }

    public void setDefaultIdentifierIri(String aDefaultIdentifierIri)
    {
        defaultIdentifierIri = aDefaultIdentifierIri;
    }

    @Override
    public String getStripClassIri()
    {
        return stripClassIri;
    }

    public void setStripClassIri(String aStripClassIri)
    {
        stripClassIri = aStripClassIri;
    }

    @Override
    public String getStripIdentifierIri()
    {
        return stripIdentifierIri;
    }

    public void setStripIdentifierIri(String aStripIdentifierIri)
    {
        stripIdentifierIri = aStripIdentifierIri;
    }
}
