/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

/**
 * Permission levels for a project. {@link PermissionLevel#USER} is an annotator while
 * {@link PermissionLevel#ADMIN} is a project administrator
 *
 * @author Seid Muhie Yimam
 *
 */
public enum PermissionLevel
    implements PersistentEnum
{
    USER("user"), CURATOR("curator"), ADMIN("admin");
    public String getName()
    {
        return this.name().toLowerCase();
    }

    public String tosString()
    {
        return this.name().toLowerCase();
    }

    PermissionLevel(String aId)
    {
        this.id = aId;
    }

    private final String id;

    @Override
    public String getId()
    {
        return id;
    }
}
