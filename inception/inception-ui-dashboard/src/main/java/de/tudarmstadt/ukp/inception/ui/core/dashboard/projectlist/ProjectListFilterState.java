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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;

public class ProjectListFilterState
    implements Serializable
{
    private static final long serialVersionUID = -6340607235253080789L;

    private String query;
    private final List<PermissionLevel> roles = new ArrayList<>();
    private final List<ProjectState> states = new ArrayList<>();

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String aQuery)
    {
        query = aQuery;
    }

    public List<PermissionLevel> getRoles()
    {
        return roles;
    }

    public void setRoles(List<PermissionLevel> aRoles)
    {
        roles.clear();
        if (aRoles != null) {
            roles.addAll(aRoles);
        }
    }

    public List<ProjectState> getStates()
    {
        return states;
    }

    public void setStates(List<ProjectState> aStates)
    {
        states.clear();
        if (aStates != null) {
            states.addAll(aStates);
        }
    }
}
