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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseAccess
    implements Serializable
{
    private static final long serialVersionUID = 3854241099411320320L;

    @JsonProperty("access-url")
    private String accessUrl;

    @JsonProperty("full-text-search")
    private String fullTextSearchIri;

    @JsonCreator
    public KnowledgeBaseAccess(@JsonProperty("access-url") String aAccessUrl,
            @JsonProperty("full-text-search") String aFullTestSearchIri)
    {
        accessUrl = aAccessUrl;
        fullTextSearchIri = aFullTestSearchIri;
    }

    public KnowledgeBaseAccess()
    {

    }

    public String getAccessUrl()
    {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl)
    {
        this.accessUrl = accessUrl;
    }

    public String getFullTextSearchIri()
    {
        return fullTextSearchIri;
    }

    public void setFullTextSearchIri(String aFullTextSearchIri)
    {
        fullTextSearchIri = aFullTextSearchIri;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseAccess that = (KnowledgeBaseAccess) o;
        return Objects.equals(accessUrl, that.accessUrl)
                && Objects.equals(fullTextSearchIri, that.fullTextSearchIri);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accessUrl, fullTextSearchIri);
    }
}
