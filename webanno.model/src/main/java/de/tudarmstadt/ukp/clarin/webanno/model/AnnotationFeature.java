/*******************************************************************************
 * Copyright 2014
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

import org.hibernate.annotations.ForeignKey;

/**
 * A persistence object for an annotation feature. One or more features can be defined per
 * {@link AnnotationLayer}. At least one feature must be defined which serves as the “label feature”.
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
@Table(name = "annotation_feature", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "annotation_type", "name", "project" }) })
public class AnnotationFeature
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    private String type;

    @ManyToOne
    @ForeignKey(name = "none")
    @JoinColumn(name = "annotation_type")
    private AnnotationLayer layer;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @ManyToOne
    @ForeignKey(name = "none")
    @JoinColumn(name = "tag_set")
    TagSet tagset;

    @Column(nullable = false)
    private String uiName;

    @Lob
    private String description;

    private boolean enabled;

    @Column(nullable = false)
    private String name;

    private boolean visible = true;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    /**
     *
     * the type of feature (string, integer, float, boolean, or a span type used as a label)
     */
    public String getType()
    {
        return type;
    }

    /**
     *
     * the type of feature (string, integer, float, boolean, or a span type used as a label)
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * the type with which the feature is associated.
     */
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    /**
     * the type with which the feature is associated.
     */
    public void setLayer(AnnotationLayer layer)
    {
        this.layer = layer;
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

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public TagSet getTagset()
    {
        return tagset;
    }

    public void setTagset(TagSet tagset)
    {
        this.tagset = tagset;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        AnnotationFeature other = (AnnotationFeature) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        }
        else if (!project.equals(other.project)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        }
        else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}
