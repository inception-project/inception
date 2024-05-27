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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectPermissionsChangedEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class ProjectPermissionsChangedEventAdapter
    implements EventLoggingAdapter<ProjectPermissionsChangedEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return ProjectPermissionsChangedEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getProject(ProjectPermissionsChangedEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override
    public String getDetails(ProjectPermissionsChangedEvent aEvent) throws IOException
    {
        var permissionChangesByUser = new LinkedHashMap<String, PermissionChanges>();

        for (var p : aEvent.getAddedPermissions()) {
            var changes = permissionChangesByUser.computeIfAbsent(p.getUser(),
                    _key -> new PermissionChanges());
            changes.granted.add(p.getLevel());
        }

        for (var p : aEvent.getRemovedPermissions()) {
            var changes = permissionChangesByUser.computeIfAbsent(p.getUser(),
                    _key -> new PermissionChanges());
            changes.revoked.add(p.getLevel());
        }

        return JSONUtil.toJsonString(permissionChangesByUser);
    }

    public static class PermissionChanges
    {
        public List<PermissionLevel> granted = new ArrayList<>();
        public List<PermissionLevel> revoked = new ArrayList<>();
    }
}
