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
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("annotation.default-preferences")
public class AnnotationEditorPropertiesImpl
    implements AnnotationEditorProperties
{
    private int pageSize = 5;
    private boolean autoScroll = true;
    private boolean rememberLayer = false;
    private boolean rememberLayerEnabled = false;
    private boolean forwardAnnotationEnabled = false;

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

    @Override
    public boolean isRememberLayer()
    {
        if (!rememberLayerEnabled) {
            return true;
        }

        return rememberLayer;
    }

    public void setRememberLayer(boolean aRememberLayer)
    {
        rememberLayer = aRememberLayer;
    }

    @Override
    public boolean isForwardAnnotationEnabled()
    {
        return forwardAnnotationEnabled;
    }

    public void setForwardAnnotationEnabled(boolean aForwardAnnotationEnabled)
    {
        forwardAnnotationEnabled = aForwardAnnotationEnabled;
    }

    @Override
    public boolean isRememberLayerEnabled()
    {
        return rememberLayerEnabled;
    }

    public void setRememberLayerEnabled(boolean aRememberLayerEnabled)
    {
        rememberLayerEnabled = aRememberLayerEnabled;
    }
}
