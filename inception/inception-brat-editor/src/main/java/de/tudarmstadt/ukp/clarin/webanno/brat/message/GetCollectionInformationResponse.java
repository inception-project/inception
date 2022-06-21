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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.brat.schema.model.EntityType;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;

/**
 * Response for the {@code getCollectionInformation} command.
 */
public class GetCollectionInformationResponse
    extends AjaxResponse
{
    public static final String COMMAND = "getCollectionInformation";

    @JsonProperty("entity_types")
    private Set<EntityType> entityTypes = new HashSet<>();

    @JsonProperty("visual_options")
    private VisualOptions visualOptions = new VisualOptions();

    public GetCollectionInformationResponse()
    {
        super(COMMAND);
    }

    public Set<EntityType> getEntityTypes()
    {
        return entityTypes;
    }

    public void setEntityTypes(Set<EntityType> aEntityTypes)
    {
        entityTypes = aEntityTypes;
    }

    public VisualOptions getVisualOptions()
    {
        return visualOptions;
    }

    public void setVisualOptions(VisualOptions aVisualOptions)
    {
        visualOptions = aVisualOptions;
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
