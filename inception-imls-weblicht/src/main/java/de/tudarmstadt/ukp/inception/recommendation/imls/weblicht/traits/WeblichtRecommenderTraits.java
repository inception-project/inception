/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits;

import java.io.Serializable;
import java.util.Date;

public class WeblichtRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = -8510745169922718787L;

    private String url;
    
    private String apiKey;
    
    private Date lastKeyUpdate;
    
    private WeblichtFormat chainInputFormat;

    private String chainInputLanguage;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String aApiKey)
    {
        apiKey = aApiKey;
    }

    public Date getLastKeyUpdate()
    {
        return lastKeyUpdate;
    }

    public void setLastKeyUpdate(Date aLastKeyUpdate)
    {
        lastKeyUpdate = aLastKeyUpdate;
    }

    public WeblichtFormat getChainInputFormat()
    {
        return chainInputFormat;
    }

    public void setChainInputFormat(WeblichtFormat aChainInputFormat)
    {
        chainInputFormat = aChainInputFormat;
    }

    public String getChainInputLanguage()
    {
        return chainInputLanguage;
    }

    public void setChainInputLanguage(String aChainInputLanguage)
    {
        chainInputLanguage = aChainInputLanguage;
    }
}
