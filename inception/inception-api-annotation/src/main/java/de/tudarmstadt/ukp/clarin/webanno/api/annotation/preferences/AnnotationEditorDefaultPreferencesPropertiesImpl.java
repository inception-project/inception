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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("annotation.default-preferences")
public class AnnotationEditorDefaultPreferencesPropertiesImpl
    implements AnnotationEditorDefaultPreferencesProperties
{
    private int pageSize = 10;
    private boolean autoScroll = false;

    @Override
    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int aPageSize)
    {
        pageSize = aPageSize;
    }

    @Override
    public boolean isAutoScroll()
    {
        return autoScroll;
    }

    public void setAutoScroll(boolean aAutoScroll)
    {
        autoScroll = aAutoScroll;
    }
}
