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

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubAnnotationDocumentSection
{
    @JsonProperty("text")
    private String text;

    @JsonProperty("sourcedb")
    private String sourceDb;

    @JsonProperty("sourceid")
    private String sourceId;

    @JsonProperty("divid")
    private int divId;

    @JsonProperty("section")
    private String section;

    @JsonProperty("source_url")
    private String sourceUrl;

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
    }

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

    public int getDivId()
    {
        return divId;
    }

    public void setDivId(int aDivId)
    {
        divId = aDivId;
    }

    public String getSection()
    {
        return section;
    }

    public void setSection(String aSection)
    {
        section = aSection;
    }

    public String getSourceUrl()
    {
        return sourceUrl;
    }

    public void setSourceUrl(String aSourceUrl)
    {
        sourceUrl = aSourceUrl;
    }

    public static final TypeReference<List<PubAnnotationDocumentSection>> //
    JACKSON_LIST_TYPE_REF = //
            new TypeReference<List<PubAnnotationDocumentSection>>()
            {
            };

    public static final ParameterizedTypeReference<List<PubAnnotationDocumentSection>> //
    SPRING_LIST_TYPE_REF = //
            new ParameterizedTypeReference<List<PubAnnotationDocumentSection>>()
            {
            };
}
