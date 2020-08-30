/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.workload.model;

import java.io.Serializable;

import javax.persistence.*;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * A persistence object for the workflow and workload properties of each project.
 */
@Entity
@Table(name = "workload_manager")
public class Workload_Manager implements Serializable
{
    private static final long serialVersionUID = -3289504168531309833L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @Column(columnDefinition = "VARCHAR(50)")
    private String extensionPointID;

    @Lob
    @Column(length = 64000)
    private String traits;

    public Workload_Manager()
    {

    }

    public Workload_Manager(Project aProject, String aExtensionPointID, String aTraits)
    {
        project = aProject;
        extensionPointID = aExtensionPointID;
        traits = aTraits;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public String getExtensionPointID() {
        return extensionPointID;
    }

    public void setExtensionPointID(String aExtensionPointID) {
        extensionPointID = aExtensionPointID;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

}
