/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "gazeteer", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "recommender" }) })
public class Gazeteer
    implements Serializable
{
    private static final long serialVersionUID = 7310223920449064425L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "recommender")
    private Recommender recommender;

    public Gazeteer()
    {
        // Required for JPA
    }

    public Gazeteer(String aName, Recommender aRecommender)
    {
        name = aName;
        recommender = aRecommender;
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

    public Recommender getRecommender()
    {
        return recommender;
    }

    public void setRecommender(Recommender aRecommender)
    {
        recommender = aRecommender;
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Gazeteer)) {
            return false;
        }
        Gazeteer castOther = (Gazeteer) other;
        return new EqualsBuilder().append(name, castOther.name)
                .append(recommender, castOther.recommender).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(name).append(recommender).toHashCode();
    }
}
