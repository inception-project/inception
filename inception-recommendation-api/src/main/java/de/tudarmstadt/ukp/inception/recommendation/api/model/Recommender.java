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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "layer", nullable = false)
    private AnnotationLayer layer;
    
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "feature", nullable = false)
    private AnnotationFeature feature;
    
    private String name;
    
    private String tool;
    
    private double threshold;
    
    private boolean alwaysSelected;

    private boolean skipEvaluation;

    private boolean enabled = true;
    
    private int maxRecommendations;

    /**
     * Only documents that have an annotation state not contained in this list are
     * used for training.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommender_ignored_document_states")
    @Column(name = "name", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<AnnotationDocumentState> statesIgnoredForTraining;

    @Lob
    @Column(length = 64000)
    private String traits;
    
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

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
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
    
    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public void setFeature(AnnotationFeature aFeature)
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

    public double getThreshold()
    {
        return threshold;
    }

    public void setThreshold(double aThreshold)
    {
        threshold = aThreshold;
    }

    public boolean isAlwaysSelected()
    {
        return alwaysSelected;
    }

    public void setAlwaysSelected(boolean aAlwaysSelected)
    {
        alwaysSelected = aAlwaysSelected;
    }

    public boolean isSkipEvaluation()
    {
        return skipEvaluation;
    }

    public void setSkipEvaluation(boolean skipEvaluation)
    {
        this.skipEvaluation = skipEvaluation;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }
    
    public int getMaxRecommendations()
    {
        return maxRecommendations;
    }

    public void setMaxRecommendations(int aMaxRecommendations)
    {
        maxRecommendations = aMaxRecommendations;
    }

    public Set<AnnotationDocumentState> getStatesIgnoredForTraining()
    {
        return statesIgnoredForTraining;
    }

    public void setStatesIgnoredForTraining(Set<AnnotationDocumentState> aStatesIgnoredForTraining)
    {
        statesIgnoredForTraining = aStatesIgnoredForTraining;
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
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Recommender that = (Recommender) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Recommender{");
        sb.append("id=").append(id);
        sb.append(", project=").append(project);
        sb.append(", layer=").append(layer);
        sb.append(", feature='").append(feature).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", tool='").append(tool).append('\'');
        sb.append(", threshold=").append(threshold);
        sb.append(", alwaysSelected=").append(alwaysSelected);
        sb.append(", skipEvaluation=").append(skipEvaluation);
        sb.append(", enabled=").append(enabled);
        sb.append(", maxRecommendations=").append(maxRecommendations);
        sb.append(", statesIgnoredForTraining=").append(statesIgnoredForTraining);
        sb.append(", traits='").append(traits).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
