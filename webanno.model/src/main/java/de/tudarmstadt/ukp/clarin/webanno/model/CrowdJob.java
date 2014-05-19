/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
/**
 * A persistence object for crowd source task.
 * @author Seid Muhie Yimam
 *
 */
@Entity
@Table(name = "crowd_job", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "name","project" }) })
public class CrowdJob
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    private static final String CROWDFLOWER_JOBLINK = "https://crowdflower.com/jobs/";

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String name;


    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name = "crowd_job_source_document")
    private Set<SourceDocument> documents = new HashSet<SourceDocument>();

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name = "crowd_job_gold_document")
    private Set<SourceDocument> goldDocuments = new HashSet<SourceDocument>();


    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @JoinColumn(name = "api_key")
    private String apiKey;

    @JoinColumn(name = "link")
    private String link;

    @JoinColumn(name = "status")
    private String status;

    @JoinColumn(name = "task1_id")
    private String task1Id;

    @JoinColumn(name = "task2_id")
    private String task2Id;

    int useSents =-1;
    int useGoldSents =-1;


    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Set<SourceDocument> getDocuments()
    {
        return documents;
    }

    public void setDocuments(Set<SourceDocument> documents)
    {
        this.documents = documents;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getLink()
    {
        return link;
    }

    /**
     * Gets called from Crowdflower page to determine URL for a given job
     *
     * @param jobID
     * @return
     */

    private String getURLforID(String jobID)
    {
        if(jobID == null)
        {
            return "";
        }else
        {
            return  CROWDFLOWER_JOBLINK + jobID + "/";
        }
    }

    public String getLink1()
    {
        return getURLforID(task1Id);
    }

    public String getLink2()
    {
        return getURLforID(task2Id);
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getTask1Id()
    {
        return task1Id;
    }

    public void setTask1Id(String task1Id)
    {
        this.task1Id = task1Id;
    }

    public String getTask2Id()
    {
        return task2Id;
    }

    public void setTask2Id(String task2Id)
    {
        this.task2Id = task2Id;
    }

    public Set<SourceDocument> getGoldDocuments()
    {
        return goldDocuments;
    }

    public int getUseSents()
    {
        return useSents;
    }

    public void setUseSents(int useSents)
    {
        this.useSents = useSents;
    }

    public int getUseGoldSents()
    {
        return useGoldSents;
    }

    public void setUseGoldSents(int useGoldSents)
    {
        this.useGoldSents = useGoldSents;
    }

    @PersistenceContext(type=PersistenceContextType.EXTENDED)
    public void setGoldDocuments(Set<SourceDocument> goldDocuments)
    {
        this.goldDocuments = goldDocuments;
    }



}
