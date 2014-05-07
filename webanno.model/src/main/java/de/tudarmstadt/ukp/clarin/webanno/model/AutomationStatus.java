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
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

/**
 * Different Automation statistics such as number of training documents, state of the automation
 * such as, generating Train Document, generating classifier....
 *
 * @author Seid Muhie Yimam
 *
 */
@Entity
@Table(name = "auto_stat")
public class AutomationStatus
    implements Serializable
{
    private static final long serialVersionUID = -4018754250597200168L;

    @Id
    @GeneratedValue
    private long id;

    @ManyToOne
    @JoinColumn(name = "template")
    MiraTemplate template;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    int trainDocs;

    int annoDocs;
    int totalDocs;
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.StatusType")
    private Status status = Status.NOT_STARTED;
    public long getId()
    {
        return id;
    }
    public void setId(long id)
    {
        this.id = id;
    }
    public MiraTemplate getTemplate()
    {
        return template;
    }
    public void setTemplate(MiraTemplate template)
    {
        this.template = template;
    }
    public Date getStartime()
    {
        return startime;
    }
    public void setStartime(Date startime)
    {
        this.startime = startime;
    }
    public Date getEndTime()
    {
        return endTime;
    }
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }
    public int getTrainDocs()
    {
        return trainDocs;
    }
    public void setTrainDocs(int trainDocs)
    {
        this.trainDocs = trainDocs;
    }
    public int getAnnoDocs()
    {
        return annoDocs;
    }
    public void setAnnoDocs(int annoDocs)
    {
        this.annoDocs = annoDocs;
    }
    public Status getStatus()
    {
        return status;
    }
    public void setStatus(Status status)
    {
        this.status = status;
    }
    public int getTotalDocs()
    {
        return totalDocs;
    }
    public void setTotalDocs(int totalDocs)
    {
        this.totalDocs = totalDocs;
    }


}
