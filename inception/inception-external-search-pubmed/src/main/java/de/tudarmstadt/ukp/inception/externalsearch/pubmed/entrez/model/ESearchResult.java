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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "eSearchResult")
public class ESearchResult
{
    @JacksonXmlProperty(localName = "Count")
    @JsonProperty("Count")
    private int count;

    @JacksonXmlProperty(localName = "RetMax")
    @JsonProperty("RetMax")
    private int retMax;

    @JacksonXmlProperty(localName = "RetStart")
    @JsonProperty("RetStart")
    private int retStart;

    @JacksonXmlElementWrapper(localName = "IdList")
    @JacksonXmlProperty(localName = "ID")
    @JsonProperty("IdList")
    private final List<Integer> idList = new ArrayList<>();

    public int getCount()
    {
        return count;
    }

    public void setCount(int aCount)
    {
        this.count = aCount;
    }

    public int getRetMax()
    {
        return retMax;
    }

    public void setRetMax(int aRetMax)
    {
        this.retMax = aRetMax;
    }

    public int getRetStart()
    {
        return retStart;
    }

    public void setRetStart(int aRetStart)
    {
        this.retStart = aRetStart;
    }

    public List<Integer> getIdList()
    {
        return idList;
    }

    public void setIdList(List<Integer> aIdList)
    {
        idList.clear();
        if (aIdList != null) {
            idList.addAll(aIdList);
        }
    }
}
