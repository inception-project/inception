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
package de.tudarmstadt.ukp.inception.sharing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "project_invite")
public class ProjectInvite
{
    @Id
    @Column(nullable = false)
    private Long projectId;

    private String inviteId;

    private long expirationDate;

    public ProjectInvite(Long aProjectId, String aInviteId, long aExpirationDate)
    {
        super();
        projectId = aProjectId;
        inviteId = aInviteId;
        expirationDate = aExpirationDate;
    }

    protected ProjectInvite()
    {
        // constructor for JPA
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long aProjectId)
    {
        projectId = aProjectId;
    }

    public String getInviteId()
    {
        return inviteId;
    }

    public void setInviteId(String aInviteId)
    {
        inviteId = aInviteId;
    }

    public long getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(long aExpirationDate)
    {
        expirationDate = aExpirationDate;
    }

}
