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
package de.tudarmstadt.ukp.inception.io.xmi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link UimaFormatsAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("format")
public class UimaFormatsPropertiesImpl
    implements UimaFormatsProperties
{
    private XmiFormatProperties uimaXmi = new XmiFormatProperties();
    private XmiFormatProperties uimaXmiXml1_1 = new XmiFormatProperties();

    @Override
    public XmiFormatProperties getUimaXmi()
    {
        return uimaXmi;
    }

    public void setUimaXmi(XmiFormatProperties aUimaXmi)
    {
        uimaXmi = aUimaXmi;
    }

    @Override
    public XmiFormatProperties getUimaXmiXml1_1()
    {
        return uimaXmiXml1_1;
    }

    public void setUimaXmiXml1_1(XmiFormatProperties aUimaXmiXml1_1)
    {
        uimaXmiXml1_1 = aUimaXmiXml1_1;
    }

    public static class XmiFormatProperties
    {
        private boolean sanitizeIllegalCharacters = true;

        public void setSanitizeIllegalCharacters(boolean aSanitizeIllegalCharacters)
        {
            sanitizeIllegalCharacters = aSanitizeIllegalCharacters;
        }

        public boolean isSanitizeIllegalCharacters()
        {
            return sanitizeIllegalCharacters;
        }

    }
}
