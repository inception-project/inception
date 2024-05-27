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
package de.tudarmstadt.ukp.inception.project.api.event;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;

public class ProjectPermissionsChangedEvent
    extends ProjectPermissionsEvent
{
    private static final long serialVersionUID = -5906708649928817973L;

    private final List<ProjectPermission> added;
    private final List<ProjectPermission> removed;

    public ProjectPermissionsChangedEvent(Object aSource, Project aProject,
            Collection<ProjectPermission> aAdded, Collection<ProjectPermission> aRemoved)
    {
        super(aSource, aProject);
        added = unmodifiableList(new ArrayList<>(aAdded));
        removed = unmodifiableList(new ArrayList<>(aRemoved));
    }

    public List<ProjectPermission> getAddedPermissions()
    {
        return added;
    }

    public List<ProjectPermission> getRemovedPermissions()
    {
        return removed;
    }
}
