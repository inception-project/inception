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

public class DocSum
{
    private static final String ITEM_SOURCE = "Source";
    private static final String ITEM_PUB_DATE = "PubDate";
    private static final String ITEM_TITLE = "Title";

    @JacksonXmlProperty(localName = "Id")
    @JsonProperty("Id")
    private int id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Item")
    @JsonProperty("Item")
    private final List<Item> items = new ArrayList<>();

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
    {
        id = aId;
    }

    public List<Item> getItems()
    {
        return items;
    }

    public void setItems(List<Item> aItems)
    {
        items.clear();
        if (aItems != null) {
            items.addAll(aItems);
        }
    }

    public String source()
    {
        return getItems().stream() //
                .filter(i -> ITEM_SOURCE.equals(i.getName())) //
                .findFirst() //
                .map(Item::getValue) //
                .orElse(null);
    }

    public String title()
    {
        return getItems().stream() //
                .filter(i -> ITEM_TITLE.equals(i.getName())) //
                .findFirst() //
                .map(Item::getValue) //
                .orElse(Integer.toString(getId()));
    }

    public String date()
    {
        return getItems().stream() //
                .filter(i -> ITEM_PUB_DATE.equals(i.getName())) //
                .findFirst() //
                .map(Item::getValue) //
                .orElse(null);
    }
}
