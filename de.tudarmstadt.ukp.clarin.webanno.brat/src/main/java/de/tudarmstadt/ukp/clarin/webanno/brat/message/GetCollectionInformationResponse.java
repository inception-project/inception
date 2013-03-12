/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.EntityType;

/**
 * Response for the {@code getCollectionInformation} command.
 */
public class GetCollectionInformationResponse
    extends AjaxResponse
{
    public static final String COMMAND = "getCollectionInformation";

    public static final String COLLECTION = "c";
    public static final String DOCUMENT = "d";

    private String description;
    private String parent;

    /**
     * Column headers in the document/collection open dialog.
     */
    private List<String[]> header = new ArrayList<String[]>();

    /**
     * Collections/documents listed in the open dialog.
     */
    private List<String[]> items = new ArrayList<String[]>();

    @JsonProperty("disambiguator_config")
    private List<String> disambiguatorConfig = new ArrayList<String>();

    @JsonProperty("search_config")
    private List<String[]> searchConfig;

    @JsonProperty("ner_taggers")
    private String nerTaggers;

    @JsonProperty("annotation_logging")
    private boolean annotationLogging;

    @JsonProperty("entity_types")
    private Set<EntityType> entityTypes = new HashSet<EntityType>();

    public GetCollectionInformationResponse()
    {
        super(COMMAND);

        header.add(new String[] { "Document", "string" });
        header.add(new String[] { "Modified", "time" });
        header.add(new String[] { "Entities", "int" });
        header.add(new String[] { "Relations", "int" });
        header.add(new String[] { "Events", "int" });
    }

    public void addCollection(String aName)
    {
        items.add(new String[] { COLLECTION, null, aName });
    }

    public void addDocument(String aName)
    {
        items.add(new String[] { DOCUMENT, null, aName });
    }

    public List<String[]> getHeader()
    {
        return header;
    }

    public void setHeader(List<String[]> aHeader)
    {
        header = aHeader;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent(String aParent)
    {
        parent = aParent;
    }

    public List<String[]> getItems()
    {
        return items;
    }

    public void setItems(List<String[]> aItems)
    {
        items = aItems;
    }

    public List<String> getDisambiguatorConfig()
    {
        return disambiguatorConfig;
    }

    public void setDisambiguatorConfig(List<String> aDisambiguatorConfig)
    {
        disambiguatorConfig = aDisambiguatorConfig;
    }

    public List<String[]> getSearchConfig()
    {
        return searchConfig;
    }

    public void setSearchConfig(List<String[]> aSearchConfig)
    {
        searchConfig = aSearchConfig;
    }

    public String getNerTaggers()
    {
        return nerTaggers;
    }

    public void setNerTaggers(String aNerTaggers)
    {
        nerTaggers = aNerTaggers;
    }

    public boolean isAnnotationLogging()
    {
        return annotationLogging;
    }

    public void setAnnotationLogging(boolean aAnnotationLogging)
    {
        annotationLogging = aAnnotationLogging;
    }


    public Set<EntityType> getEntityTypes()
    {
        return entityTypes;
    }

    public void setEntityTypes(Set<EntityType> aEntityTypes)
    {
        entityTypes = aEntityTypes;
    }
}
