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
    private TagSet tagset;

    @Column(nullable = false)
    private String uiName;

    @Lob
    private String description;

    private boolean enabled = true;

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
     * Get the type of feature (string, integer, float, boolean, or a span type used as a label)
     * 
     * @return the type of feature.
     */
    public String getType()
    {
        return type;
    }

    /**
     * The type of feature (string, integer, float, boolean, or a span type used as a label)
     * 
     * @param type the type of feature.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * Get the layer with which the feature is associated.
     * 
     * @return the layer.
     */
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    /**
     * The layer with which the feature is associated.
     * 
     * @param layer the layer.
     */
    public void setLayer(AnnotationLayer layer)
    {
        this.layer = layer;
    }

    public Project getProject()
    {
        return project;
    }

    /**
     * @param project the project.
     */
    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     * The name of the feature as displayed in the UI.
     * 
     * @return the name displayed in the UI.
     */
    public String getUiName()
    {
        return uiName;
    }

    /**
     * The name of the feature as displayed in the UI.
     * 
     * @param uiName the name displayed in the UI.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     * A description of the feature.
     * 
     * @return the description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * A description of the feature.
     * 
     * @param description the description.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings)
     * 
     * @return if the layer is enabled.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings)
     * 
     * @param enabled if the layer is enabled.
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * The name of the feature in the UIMA type system.
     *
     * @return the UIMA type name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * The name of the feature in the UIMA type system.
     * 
     * @param name the UIMA type name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return if the feature value is rendered in the label.
     */
    public boolean isVisible()
    {
        return visible;
    }

    /**
     * @param visible if the feature value is rendered in the label.
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    /**
     * @return the tagset.
     */
    public TagSet getTagset()
    {
        return tagset;
    }

    /**
     * @param tagset the tagset.
     */
    public void setTagset(TagSet tagset)
    {
        this.tagset = tagset;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layer == null) ? 0 : layer.hashCode());
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
        if (layer == null) {
            if (other.layer != null) {
                return false;
            }
        }
        else if (!layer.equals(other.layer)) {
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
