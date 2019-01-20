/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseAccess implements Serializable
{
    @JsonProperty("access-url")
    private String accessUrl;

    @JsonProperty("full-text-search")
    private IRI fullTextSearchIri;

    @JsonCreator public KnowledgeBaseAccess(@JsonProperty("access-url") String aAccessUrl,
        @JsonProperty("full-text-search") String aFullTestSearchIri)
    {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        accessUrl = aAccessUrl;
        fullTextSearchIri = vf.createIRI(aFullTestSearchIri);
    }

    public KnowledgeBaseAccess() {

    }

    public String getAccessUrl()
    {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl)
    {
        this.accessUrl = accessUrl;
    }

    public IRI getFullTextSearchIri()
    {
        return fullTextSearchIri;
    }

    public void setFullTextSearchIri(IRI aFullTextSearchIri)
    {
        fullTextSearchIri = aFullTextSearchIri;
    }

    @Override public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseAccess that = (KnowledgeBaseAccess) o;
        return Objects.equals(accessUrl, that.accessUrl);
    }

    @Override public int hashCode()
    {
        return Objects.hash(accessUrl);
    }
}
