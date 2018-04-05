/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.automation.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * A persistence object for MIRA template configurations
 */
@Entity
@Table(name = "mira_template", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "trainFeature" }) })
public class MiraTemplate
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "automationStarted")
    private boolean automationStarted = false;

    /**
     * Train {@link TagSet} used for MIRA prediction
     */
    @ManyToOne
    @JoinColumn(name = "trainFeature")
    private AnnotationFeature trainFeature;
    
    /**
     * {@link TagSet} used as a feature for the trainFeature
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<AnnotationFeature> otherFeatures = new HashSet<>();

    @Column(name = "currentLayer")
    private boolean currentLayer = false;// The current training layer for this mira template

    /**
     * Repeat span annotation to the suggestions view 
     */
    @Column(name = "annotateAndPredict")
    private boolean annotateAndPredict = true;
    
    /**
     * Results comprising of the training accuracy and number of examples used
     */
    private String result = "";

    public AnnotationFeature getTrainFeature()
    {
        return trainFeature;
    }

    public void setTrainFeature(AnnotationFeature trainFeature)
    {
        this.trainFeature = trainFeature;
    }

    public Set<AnnotationFeature> getOtherFeatures()
    {
        return otherFeatures;
    }

    public void setOtherFeatures(Set<AnnotationFeature> otherFeatures)
    {
        this.otherFeatures = otherFeatures;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public boolean isAnnotateAndRepeat()
    {
        return annotateAndPredict;
    }

    public void setAnnotateAndRepeat(boolean annotateAndRepeat)
    {
        this.annotateAndPredict = annotateAndRepeat;
    }
    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public boolean isCurrentLayer()
    {
        return currentLayer;
    }

    public void setCurrentLayer(boolean currentLayer)
    {
        this.currentLayer = currentLayer;
    }

    public boolean isAutomationStarted()
    {
        return automationStarted;
    }

    public void setAutomationStarted(boolean automationStarted)
    {
        this.automationStarted = automationStarted;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((trainFeature == null) ? 0 : trainFeature.hashCode());
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
        MiraTemplate other = (MiraTemplate) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        }
        else if (!id.equals(other.id)) {
            return false;
        }
        if (trainFeature == null) {
            if (other.trainFeature != null) {
                return false;
            }
        }
        else if (!trainFeature.equals(other.trainFeature)) {
            return false;
        }
        return true;
    }
}
