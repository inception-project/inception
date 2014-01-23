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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

/**
 * A persistence object for meta-data of source documents. The content of the source document is
 * stored in the file system.
 *
 * @author Seid Muhie Yimam
 *
 */
@Entity
@Table(name = "source_document", uniqueConstraints = { @UniqueConstraint(columnNames = { "name",
        "project" }) })
public class SourceDocument
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "project")
    Project project;

    private String format;

    @Column(nullable = false)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateType")
    private SourceDocumentState state = SourceDocumentState.NEW;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    private int sentenceAccessed = 0;

    private boolean trainingDocument = false;
    private boolean processed = false;

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String aFormat)
    {
        format = aFormat;
    }

    public SourceDocumentState getState()
    {
        return state;
    }

    public void setState(SourceDocumentState aState)
    {
        state = aState;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getSentenceAccessed()
    {
        return sentenceAccessed;
    }

    public void setSentenceAccessed(int sentenceAccessed)
    {
        this.sentenceAccessed = sentenceAccessed;
    }
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SourceDocument other = (SourceDocument) obj;
        if (id != other.id) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public boolean isTrainingDocument()
    {
        return trainingDocument;
    }

    public void setTrainingDocument(boolean trainingDocument)
    {
        this.trainingDocument = trainingDocument;
    }

    public boolean isProcessed()
    {
        return processed;
    }

    public void setProcessed(boolean processed)
    {
        this.processed = processed;
    }


}
