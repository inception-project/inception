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
package de.tudarmstadt.ukp.inception.workload.dynamic.model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

import javax.persistence.*;
import java.io.Serializable;

import static de.tudarmstadt.ukp.inception.workload.dynamic.api.WorkloadConst.*;

/**
 * A persistence object for the workflow and workload properties of each project.
 */
@Entity
@Table(name = "workload_assignment")
public class WorkloadAssignment implements Serializable
{
    private static final long serialVersionUID = -9087395004474377523L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @Lob
    @Column(length = 64000)
    private String traits;

    private String workflowManagerType;

    private String workloadManagerType;

    private String workflow;

    private int numberOfAnnotations;

    public WorkloadAssignment()
    {
        workflowManagerType = DEFAULT_WORKFLOW;
        workloadManagerType = DEFAULT_MONITORING;
        workflow = DEFAULT_WORKFLOW_TYPE;
        numberOfAnnotations = 3;
    }

    public WorkloadAssignment(Project aProject, String aWorkflowManager,
                              String aWorkloadManager, String aWorkflow,
                              int aDefaultNumberOfAnnotations)
    {
        project = aProject;
        workflowManagerType = aWorkflowManager;
        workloadManagerType = aWorkloadManager;
        workflow = aWorkflow;
        numberOfAnnotations = aDefaultNumberOfAnnotations;
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

    public String getWorkflowmanager()
    {
        return workflowManagerType;
    }

    public void setWorkflowmanager(String aWorkflowmanager)
    {
        workflowManagerType = aWorkflowmanager;
    }

    public String getWorkloadmanager()
    {
        return workloadManagerType;
    }

    public void setWorkloadmanager(String aWorkloadmanager)
    {
        workloadManagerType = aWorkloadmanager;
    }

    public int getDefaultnumberofannotations()
    {
        return numberOfAnnotations;
    }

    public void setDefaultnumberofannotations(int aDefaultnumberofannotations)
    {
        numberOfAnnotations = aDefaultnumberofannotations;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    public String getWorkflowManager()
    {
        return workflowManagerType;
    }

    public void setWorkflowManager(String aWorkflowManager)
    {
        workflowManagerType = aWorkflowManager;
    }

    public String getWorkloadManager()
    {
        return workloadManagerType;
    }

    public void setWorkloadManager(String aWorkloadManager) {
        workloadManagerType = aWorkloadManager;
    }

    public String getWorkflow()
    {
        return workflow;
    }

    public void setWorkflow(String aWorkflow)
    {
        workflow = aWorkflow;
    }

    public int getDefaultNumberOfAnnotations()
    {
        return numberOfAnnotations;
    }

    public void setDefaultNumberOfAnnotations(int aDefaultNumberOfAnnotations)
    {
        numberOfAnnotations = aDefaultNumberOfAnnotations;
    }
}
