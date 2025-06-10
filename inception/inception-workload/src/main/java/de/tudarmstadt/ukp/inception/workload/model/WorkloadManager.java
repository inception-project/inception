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
package de.tudarmstadt.ukp.inception.workload.model;

import static org.hibernate.annotations.OnDeleteAction.CASCADE;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.OnDelete;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagementAutoConfiguration#workloadManager}. A persistence object for the
 * workflow and workload properties of each project
 * </p>
 */
@Entity
@Table(name = "workload_manager", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project", "workloadType" }) })
public class WorkloadManager
    implements Serializable
{
    private static final long serialVersionUID = -3289504168531309833L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    @OnDelete(action = CASCADE)
    private Project project;

    @Column(columnDefinition = "VARCHAR(255)")
    private String workloadType;

    @Column(length = 64000)
    private String traits;

    public WorkloadManager()
    {
        // Required for serialization
    }

    public WorkloadManager(Project aProject, String aWorkloadType, String aTraits)
    {
        project = aProject;
        workloadType = aWorkloadType;
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

    public String getType()
    {
        return workloadType;
    }

    public void setType(String aWorkloadType)
    {
        workloadType = aWorkloadType;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof WorkloadManager)) {
            return false;
        }
        WorkloadManager castOther = (WorkloadManager) other;
        return Objects.equals(project, castOther.project)
                && Objects.equals(workloadType, castOther.workloadType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project, workloadType);
    }
}
