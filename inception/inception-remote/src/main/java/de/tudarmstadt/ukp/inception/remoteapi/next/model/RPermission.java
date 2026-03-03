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
package de.tudarmstadt.ukp.inception.remoteapi.next.model;

import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;

public class RPermission
{
    public long project;
    public String user;
    public String role;

    public RPermission(ProjectPermission aPermission)
    {
        project = aPermission.getProject().getId();
        user = aPermission.getUser();
        role = aPermission.getLevel().name();
    }

    public RPermission(long aProject, String aUser, String aRole)
    {
        project = aProject;
        user = aUser;
        role = aRole;
    }
}
