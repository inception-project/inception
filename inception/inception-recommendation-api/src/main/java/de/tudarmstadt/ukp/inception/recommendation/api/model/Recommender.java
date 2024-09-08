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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateType;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
     * Only documents that have an annotation state not contained in this list are used for
     * training.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommender_ignored_document_states")
    @Column(name = "name", nullable = false)
    @Type(AnnotationDocumentStateType.class)
    private Set<AnnotationDocumentState> statesIgnoredForTraining;

    @Column(length = 64000)
    private String traits;

    private Recommender(Builder builder)
    {
        this.id = builder.id;
        this.project = builder.project;
        this.layer = builder.layer;
        this.feature = builder.feature;
        this.name = builder.name;
        this.tool = builder.tool;
        this.threshold = builder.threshold;
        this.alwaysSelected = builder.alwaysSelected;
        this.skipEvaluation = builder.skipEvaluation;
        this.enabled = builder.enabled;
        this.maxRecommendations = builder.maxRecommendations;
        this.statesIgnoredForTraining = builder.statesIgnoredForTraining;
        this.traits = builder.traits;
    }

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

    /**
     * Activation score threshold.
     * 
     * @param aThreshold
     */
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
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name);
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Long id;
        private Project project;
        private AnnotationLayer layer;
        private AnnotationFeature feature;
        private String name;
        private String tool;
        private double threshold;
        private boolean alwaysSelected;
        private boolean skipEvaluation;
        private boolean enabled = true;
        private int maxRecommendations;
        private Set<AnnotationDocumentState> statesIgnoredForTraining = Collections.emptySet();
        private String traits;

        private Builder()
        {
        }

        public Builder withId(Long aId)
        {
            id = aId;
            return this;
        }

        public Builder withProject(Project aProject)
        {
            project = aProject;
            return this;
        }

        public Builder withLayer(AnnotationLayer aLayer)
        {
            layer = aLayer;
            return this;
        }

        public Builder withFeature(AnnotationFeature aFeature)
        {
            feature = aFeature;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withTool(String aTool)
        {
            tool = aTool;
            return this;
        }

        public Builder withThreshold(double aThreshold)
        {
            threshold = aThreshold;
            return this;
        }

        public Builder withAlwaysSelected(boolean aAlwaysSelected)
        {
            alwaysSelected = aAlwaysSelected;
            return this;
        }

        public Builder withSkipEvaluation(boolean aSkipEvaluation)
        {
            skipEvaluation = aSkipEvaluation;
            return this;
        }

        public Builder withEnabled(boolean aEnabled)
        {
            enabled = aEnabled;
            return this;
        }

        public Builder withMaxRecommendations(int aMaxRecommendations)
        {
            maxRecommendations = aMaxRecommendations;
            return this;
        }

        public Builder withStatesIgnoredForTraining(
                Set<AnnotationDocumentState> aStatesIgnoredForTraining)
        {
            statesIgnoredForTraining = aStatesIgnoredForTraining;
            return this;
        }

        public Builder withTraits(String aTraits)
        {
            traits = aTraits;
            return this;
        }

        public Recommender build()
        {
            return new Recommender(this);
        }
    }
}
