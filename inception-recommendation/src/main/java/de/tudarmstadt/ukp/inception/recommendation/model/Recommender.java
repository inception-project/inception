/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Entity
@Table(name = "recommender", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class Recommender
    implements Serializable
{
    private static final long serialVersionUID = 7748907568404136301L;
    
    @Id
    @GeneratedValue
    private int id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "layer", nullable = false)
    private AnnotationLayer layer;
    
    private String feature;
    
    private String name;
    
    private String tool;
    
    private float threshold;
    
    private boolean alwaysSelected;
    
    public Recommender()
    {
        // Nothing to do
    }
    
    public Recommender(String aName, AnnotationLayer aLayer)
    {
        name = aName;
        project = aLayer.getProject();
        layer = aLayer;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
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

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public void setLayer(AnnotationLayer aLayer)
    {
        layer = aLayer;
    }
    
    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getTool()
    {
        return tool;
    }

    public void setTool(String aTool)
    {
        tool = aTool;
    }

    public float getThreshold()
    {
        return threshold;
    }

    public void setThreshold(float aThreshold)
    {
        threshold = aThreshold;
    }

    public boolean isAlwaysSelected()
    {
        return alwaysSelected;
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
        Recommender other = (Recommender) obj;
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
}
