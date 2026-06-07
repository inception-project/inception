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
package de.tudarmstadt.ukp.inception.pivot.api.model;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pivot_report", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class PivotReport
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(length = 64000)
    private String description;

    @Column(length = 64000)
    private String definition;

    @Column(name = "created", nullable = true)
    private Date created;

    @Column(name = "updated", nullable = true)
    private Date updated;

    public PivotReport()
    {
        // Required by JPA
    }

    public PivotReport(Project aProject, String aName)
    {
        project = aProject;
        name = aName;
    }

    @PrePersist
    protected void onCreate()
    {
        if (created == null) {
            created = new Date();
            updated = created;
        }
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
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

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getDefinition()
    {
        return definition;
    }

    public void setDefinition(String aDefinition)
    {
        definition = aDefinition;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }
}
