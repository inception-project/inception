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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A persistence object for project permission. A user can have one or multiple permissions on a
 * project. Project permissions include {@code admin}, {@code user} for (annotator) and
 * {@code curator}
 */
@Entity
@Table(name = "project_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user", "level", "project" }) })
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ProjectPermission
    implements Serializable
{
    private static final long serialVersionUID = -1490540239189868920L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Type(PermissionLevelType.class)
    private PermissionLevel level;

    private String user;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    public ProjectPermission()
    {
        // Required for JPA
    }

    public ProjectPermission(Project aProject, String aUser, PermissionLevel aLevel)
    {
        project = aProject;
        user = aUser;
        level = aLevel;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

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

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    @Override
    public String toString()
    {
        return "[" + user + "] is [" + level + "] in " + project;
    }
}
