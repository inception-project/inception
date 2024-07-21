/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.curation.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@IdClass(CurationSettingsId.class)
@Table(name = "curation_settings")
public class CurationSettings
{
    @Id
    @Column(nullable = false)
    private Long projectId;

    @Id
    @Column(nullable = false)
    private String username;

    @ElementCollection
    @CollectionTable(name = "curationSettings_users", joinColumns = {
            @JoinColumn(name = "settings_projectId", referencedColumnName = "projectId"),
            @JoinColumn(name = "settings_username", referencedColumnName = "username") })
    @Column(name = "selectedUsername", nullable = true, updatable = true)
    private Set<String> selectedUserNames = new HashSet<>();

    @Column(nullable = false)
    private String curationUsername;

    protected CurationSettings()
    {
        // constructor for JPA
    }

    public CurationSettings(String aUser, Long aProjectId, String aCurationUser)
    {
        username = aUser;
        projectId = aProjectId;
        curationUsername = aCurationUser;
    }

    public CurationSettings(String aUsername, Long aProjectId, String aCurationName,
            Set<String> aUsernames)
    {
        username = aUsername;
        projectId = aProjectId;
        curationUsername = aCurationName;
        if (aUsernames == null) {
            selectedUserNames = new HashSet<>();
        }
        else {
            selectedUserNames = aUsernames;
        }
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long aProjectId)
    {
        projectId = aProjectId;
    }

    public String getUserName()
    {
        return username;
    }

    public void setUserName(String aUser)
    {
        username = aUser;
    }

    public Set<String> getSelectedUserNames()
    {
        return selectedUserNames;
    }

    public void setSelectedUserNames(Set<String> aSelectedUserNames)
    {
        selectedUserNames = aSelectedUserNames;
    }

    public String getCurationUserName()
    {
        return curationUsername;
    }

    public void setCurationUserName(String aCurationUserName)
    {
        curationUsername = aCurationUserName;
    }
}
