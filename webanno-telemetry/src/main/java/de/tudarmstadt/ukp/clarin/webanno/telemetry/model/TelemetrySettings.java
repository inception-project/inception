/*
 * Copyright 2019
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
 */
package de.tudarmstadt.ukp.clarin.webanno.telemetry.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetrySupport;

@Entity
@Table(name = "telemetry_settings")
public class TelemetrySettings
    implements Serializable
{
    private static final long serialVersionUID = -8944000690183645340L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(nullable = false)
    private String support;

    @Column(nullable = false)
    private int version;
    
    @Lob
    @Column(length = 64000)
    private String traits;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;
    
    public TelemetrySettings()
    {
        // For serialization and persistence
    }
    
    public <T> TelemetrySettings(TelemetrySupport<T> aSupport)
    {
        support = aSupport.getId();
        version = aSupport.getVersion();
    }
    

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public String getSupport()
    {
        return support;
    }

    public void setSupport(String aSupport)
    {
        support = aSupport;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int aVersion)
    {
        version = aVersion;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    @PrePersist
    protected void onCreate()
    {
        // When we import data, we set the fields via setters and don't want these to be 
        // overwritten by this event handler.
        if (created != null) {
            created = new Date();
            updated = created;
        }
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
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
