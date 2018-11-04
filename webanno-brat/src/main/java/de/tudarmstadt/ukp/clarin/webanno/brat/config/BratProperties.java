/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("ui.brat")
public class BratProperties
{
    private boolean singleClickSelection = false;
    private int pageSize = 5;
    private boolean autoScroll = true;
    private boolean rememberLayer = false;

    public boolean isSingleClickSelection()
    {
        return singleClickSelection;
    }

    public void setSingleClickSelection(boolean aSingleClickSelection)
    {
        singleClickSelection = aSingleClickSelection;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int aPageSize)
    {
        pageSize = aPageSize;
    }

    public boolean isAutoScroll()
    {
        return autoScroll;
    }

    public void setAutoScroll(boolean aAutoScroll)
    {
        autoScroll = aAutoScroll;
    }

    public boolean isRememberLayer()
    {
        return rememberLayer;
    }

    public void setRememberLayer(boolean aRememberLayer)
    {
        rememberLayer = aRememberLayer;
    }
}
