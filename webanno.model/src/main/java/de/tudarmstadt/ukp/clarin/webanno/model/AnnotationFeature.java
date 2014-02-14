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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A persistence object for an annotation feature. One or more features can be defined per
 * {@link AnnotationType}. At least one feature must be defined which serves as the “label feature”.
 * Additional features may be defined. Features have a type which can either be String, integer,
 * float, or boolean. To control the values that a String feature assumes, it can be associated with
 * a tagset. If the feature is defined on a span type, it is also possible to add a feature of
 * another span type which then serves as a label type for the first one
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
@Entity
@Table(name = "annotation_feature", uniqueConstraints = { @UniqueConstraint(columnNames = { "type",
        "name", "tagset" }) })
public class AnnotationFeature
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "type")
    private AnnotationType type;
    @ManyToOne
    @JoinColumn(name = "tagset")
    private TagSet tagSet;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @Column(nullable = false)
    private String uiName;

    @Lob
    private String description;

    private boolean enabled;

    @Column(nullable = false)
    private String name;

    private String featureType;

    /**
     * the type with which the feature is associated.
     */
    public long getId()
    {
        return id;
    }

    /**
     * the type with which the feature is associated.
     */
    public void setId(long id)
    {
        this.id = id;
    }

    public AnnotationType getType()
    {
        return type;
    }

    public void setType(AnnotationType type)
    {
        this.type = type;
    }

    public TagSet getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(TagSet tagSet)
    {
        this.tagSet = tagSet;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     * the name of the feature as displayed in the UI.
     */
    public String getUiName()
    {
        return uiName;
    }

    /**
     * the name of the feature as displayed in the UI.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     *
     * a description of the feature.
     */

    public String getDescription()
    {
        return description;
    }

    /**
     *
     * a description of the feature.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     *
     * whether the type is available in the UI (outside of the project settings)
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     *
     * whether the type is available in the UI (outside of the project settings)
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * the name of the feature in the UIMA type system.
     *
     */

    public String getName()
    {
        return name;
    }

    /**
     * the name of the feature in the UIMA type system.
     *
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     *
     * the type of feature (string, integer, float, boolean, or a span type used as a label)
     */
    public String getFeatureType()
    {
        return featureType;
    }

    /**
     *
     * the type of feature (string, integer, float, boolean, or a span type used as a label)
     */
    public void setFeatureType(String featureType)
    {
        this.featureType = featureType;
    }

}
