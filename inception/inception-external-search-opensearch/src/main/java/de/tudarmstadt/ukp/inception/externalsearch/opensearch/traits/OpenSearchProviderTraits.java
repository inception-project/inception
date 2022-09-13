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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchProviderTraits
    implements Serializable
{
    private static final long serialVersionUID = -3109239605741337123L;

    private static final int ARBITRARY_FIXED_SEED = 5;

    private String remoteUrl = "https://localhost:9200";

    private String indexName = "common-crawl-en";

    private String searchPath = "_search";

    private String defaultField = "doc.text";

    /**
     * Number of results retrieved from the server
     */
    private int resultSize = 1000;

    private boolean randomOrder = false;

    private boolean sslVerification = true;

    private AuthenticationType authenticationType;

    private AuthenticationTraits authentication;

    private int seed = ARBITRARY_FIXED_SEED;

    public String getRemoteUrl()
    {
        return remoteUrl;
    }

    public void setRemoteUrl(String aRemoteUrl)
    {
        remoteUrl = aRemoteUrl;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public void setIndexName(String aIndexName)
    {
        indexName = aIndexName;
    }

    public String getSearchPath()
    {
        return searchPath;
    }

    public void setSearchPath(String aSearchPath)
    {
        searchPath = aSearchPath;
    }

    public String getDefaultField()
    {
        return defaultField;
    }

    public void setDefaultField(String aDefaultField)
    {
        defaultField = aDefaultField;
    }

    public int getResultSize()
    {
        return resultSize;
    }

    public void setResultSize(int aResultSize)
    {
        resultSize = aResultSize;
    }

    public boolean isRandomOrder()
    {
        return randomOrder;
    }

    public void setRandomOrder(boolean aRandomOrder)
    {
        randomOrder = aRandomOrder;
    }

    public int getSeed()
    {
        return seed;
    }

    public void setSeed(int seed)
    {
        this.seed = seed;
    }

    public void setSslVerification(boolean aSslVerification)
    {
        sslVerification = aSslVerification;
    }

    public boolean isSslVerification()
    {
        return sslVerification;
    }

    public void setAuthenticationType(AuthenticationType aAuthenticationType)
    {
        authenticationType = aAuthenticationType;
    }

    public AuthenticationType getAuthenticationType()
    {
        return authenticationType;
    }

    public AuthenticationTraits getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication(AuthenticationTraits aAuthentication)
    {
        authentication = aAuthentication;
    }
}
