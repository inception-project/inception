/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrainingRequest {

    @JsonProperty("typeSystem")
    private String typeSystem;

    @JsonProperty("documents")
    private List<Document> documents;

    @JsonProperty("config")
    private Metadata metadata;

    public String getTypeSystem()
    {
        return typeSystem;
    }

    public void setTypeSystem(String aTypeSystem)
    {
        typeSystem = aTypeSystem;
    }

    public List<Document> getDocuments()
    {
        return documents;
    }

    public void setDocuments(List<Document> aDocuments)
    {
        documents = aDocuments;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public void setMetadata(Metadata aMetadata) {
        metadata = aMetadata;
    }
}
