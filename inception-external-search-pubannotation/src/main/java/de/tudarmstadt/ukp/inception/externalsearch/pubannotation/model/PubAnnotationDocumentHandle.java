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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PubAnnotationDocumentHandle
{
    @JsonProperty("sourcedb")
    private String sourceDb;

    @JsonProperty("sourceid")
    private String sourceId;

    @JsonProperty("url")
    private String url;

    @JsonProperty("text")
    private List<String> highlights;

    public String getSourceDb()
    {
        return sourceDb;
    }

    public void setSourceDb(String aSourceDb)
    {
        sourceDb = aSourceDb;
    }

    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String aSourceId)
    {
        sourceId = aSourceId;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public List<String> getHighlights()
    {
        return highlights != null ? highlights : emptyList();
    }

    public void setHighlights(List<String> aHighlights)
    {
        if (aHighlights == null || aHighlights.isEmpty()) {
            highlights = null;
        }
        else {
            highlights = aHighlights;
        }
    }
}
