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
package de.tudarmstadt.ukp.inception.curation.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "curation_workflow", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project" }) })
public class CurationWorkflow
    implements Serializable
{
    private static final long serialVersionUID = 4947721452713215915L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    private String mergeStrategy;

    @Column(length = 64000)
    private String mergeStrategyTraits;

    public CurationWorkflow()
    {
        // Required by Hibernate / (de)serialiszation
    }

    public CurationWorkflow(Project aProject, String aMergeStrategy, String aMergeStrategyTraits)
    {
        project = aProject;
        mergeStrategy = aMergeStrategy;
        mergeStrategyTraits = aMergeStrategyTraits;
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

    public void setMergeStrategy(String aMergeStrategy)
    {
        mergeStrategy = aMergeStrategy;
    }

    public String getMergeStrategy()
    {
        return mergeStrategy;
    }

    public String getMergeStrategyTraits()
    {
        return mergeStrategyTraits;
    }

    public void setMergeStrategyTraits(String aMergeStrategyTraits)
    {
        mergeStrategyTraits = aMergeStrategyTraits;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof CurationWorkflow)) {
            return false;
        }
        CurationWorkflow castOther = (CurationWorkflow) other;
        return new EqualsBuilder().append(project, castOther.project).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(project).toHashCode();
    }
}
