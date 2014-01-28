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
package de.tudarmstadt.ukp.clarin.webanno.model.export;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
/**
 * Project permissions information to be exported/imported
 * @author Seid Muhie Yimam
 *
 */
@JsonPropertyOrder(value = { "level", "user"})
public class ProjectPermission
{
    @JsonProperty("level")
    PermissionLevel level;
    @JsonProperty("user")
    String user;
    public PermissionLevel getLevel()
    {
        return level;
    }
    public void setLevel(PermissionLevel level)
    {
        this.level = level;
    }
    public String getUser()
    {
        return user;
    }
    public void setUser(String user)
    {
        this.user = user;
    }

    }
