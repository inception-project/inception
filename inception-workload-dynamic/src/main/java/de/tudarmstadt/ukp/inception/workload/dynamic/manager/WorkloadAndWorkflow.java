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
package de.tudarmstadt.ukp.inception.workload.dynamic.manager;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * A persistence object for the workflow and workload properties of each project.
 */
@Entity
@Table(name = "workloadandworkflow")
public class WorkloadAndWorkflow implements Serializable
{
    private static final long serialVersionUID = -9087395004474377523L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @Lob
    @Column(length = 64000)
    private String traits;

    private String workflowmanager;

    private String workloadmanager;

    private String workflow;

    private int defaultnumberofannotations;

    public WorkloadAndWorkflow()
    {
        workflowmanager = "default";
        workloadmanager = "default";
        workflow = "default";
        defaultnumberofannotations = 6;
    }

    public WorkloadAndWorkflow(Project aProject, String aWorkflowManager,
        String aWorkloadManager, String aWorkflow, int aDefaultNumberOfAnnotations)
    {
        super();
        project = aProject;
        workflowmanager = aWorkflowManager;
        workloadmanager = aWorkloadManager;
        workflow = aWorkflow;
        defaultnumberofannotations = aDefaultNumberOfAnnotations;
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
        return workflowmanager;
    }

    public void setWorkflowmanager(String aWorkflowmanager)
    {
        workflowmanager = aWorkflowmanager;
    }

    public String getWorkloadmanager()
    {
        return workloadmanager;
    }

    public void setWorkloadmanager(String aWorkloadmanager)
    {
        workloadmanager = aWorkloadmanager;
    }

    public int getDefaultnumberofannotations()
    {
        return defaultnumberofannotations;
    }

    public void setDefaultnumberofannotations(int aDefaultnumberofannotations)
    {
        this.defaultnumberofannotations = aDefaultnumberofannotations;
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
        return workflowmanager;
    }

    public void setWorkflowManager(String aWorkflowManager)
    {
        workflowmanager = aWorkflowManager;
    }

    public String getWorkloadManager()
    {
        return workloadmanager;
    }

    public void setWorkloadManager(String aWorkloadManager) {
        workloadmanager = aWorkloadManager;
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
        return defaultnumberofannotations;
    }

    public void setDefaultNumberOfAnnotations(int aDefaultNumberOfAnnotations)
    {
        defaultnumberofannotations = aDefaultNumberOfAnnotations;
    }
}
