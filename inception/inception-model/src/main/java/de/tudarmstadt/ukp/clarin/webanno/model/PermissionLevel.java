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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;
import de.tudarmstadt.ukp.inception.support.wicket.HasSymbol;

/**
 * Permission levels for a project. {@link PermissionLevel#ANNOTATOR} is an annotator while
 * {@link PermissionLevel#MANAGER} is a project administrator
 */
public enum PermissionLevel
    implements PersistentEnum, Serializable, HasSymbol
{
    // We keep the legacy values for the project export/import for compatibility reasons
    @JsonProperty("USER")
    ANNOTATOR("user", "<i class=\"fas fa-user-tag\"></i>"),

    @JsonProperty("CURATOR")
    CURATOR("curator", "<i class=\"fas fa-user-graduate\"></i>"),

    // We keep the legacy values for the project export/import for compatibility reasons
    @JsonProperty("ADMIN")
    MANAGER("admin", "<i class=\"fas fa-user-tie\"></i>");

    private final String id;
    private final String symbol;

    public String getName()
    {
        return this.name().toLowerCase();
    }

    public String tosString()
    {
        return this.name().toLowerCase();
    }

    PermissionLevel(String aId, String aSymbol)
    {
        id = aId;
        symbol = aSymbol;
    }

    @Override
    public String symbol()
    {
        return symbol;
    }

    @Override
    public String getId()
    {
        return id;
    }
}
