/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

/**
 * A persistence object for an annotation feature. One or more features can be defined per
 * {@link AnnotationLayer}. At least one feature must be defined which serves as the “label
 * feature”. Additional features may be defined. Features have a type which can either be String,
 * integer, float, or boolean. To control the values that a String feature assumes, it can be
 * associated with a tagset. If the feature is defined on a span type, it is also possible to add a
 * feature of another span type which then serves as a label type for the first one
 */
@Entity
@Table(name = "annotation_feature", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "annotation_type", "name", "project" }) })
public class AnnotationFeature
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String type;

    @ManyToOne
    @JoinColumn(name = "annotation_type", 
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private AnnotationLayer layer;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "tag_set", 
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private TagSet tagset;

    @Column(nullable = false)
    private String uiName;

    @Lob
    @Column(length = 64000)
    private String description;

    private boolean enabled = true;

    @Column(nullable = false)
    private String name;

    private boolean visible = true;
    
    @Column(name = "includeInHover")
    private boolean includeInHover = false;
    
    private boolean remember;
    
    @Column(name = "hideUnconstraintFeature")
    private boolean hideUnconstraintFeature;
    
    private boolean required;

    @Column(name = "multi_value_mode")
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.MultiValueModeType")
    private MultiValueMode multiValueMode = MultiValueMode.NONE;

    @Column(name = "link_mode")
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.LinkModeType")
    private LinkMode linkMode = LinkMode.NONE;

    @Column(name = "link_type_name")
    private String linkTypeName;
    
    @Column(name = "link_type_role_feature_name")
    private String linkTypeRoleFeatureName;
    
    @Column(name = "link_type_target_feature_name")
    private String linkTypeTargetFeatureName;
    
    @Lob
    @Column(length = 64000)
    private String traits;
    
    public AnnotationFeature()
    {
        // Nothing to do
    }

    // Visible for testing
    public AnnotationFeature(String aName, String aType)
    {
        name = aName;
        uiName = aName;
        type = aType;
    }

    // Visible for testing
    public AnnotationFeature(long aId, AnnotationLayer aLayer, String aName, String aType)
    {
        id = aId;
        layer = aLayer;
        project = aLayer.getProject();
        name = aName;
        uiName = aName;
        type = aType;
    }
    
    
    public AnnotationFeature(Project aProject, AnnotationLayer aLayer, String aName, String aUiName,
            String aType)
    {
        project = aProject;
        layer = aLayer;
        name = aName;
        uiName = aUiName;
        type = aType;
    }

    public AnnotationFeature(Project aProject, AnnotationLayer aLayer, String aName, String aUiName,
            String aType, String aDescription, TagSet aTagSet)
    {
        project = aProject;
        layer = aLayer;
        name = aName;
        uiName = aUiName;
        type = aType;
        description = aDescription;
        tagset = aTagSet;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
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

    public boolean isIncludeInHover()
    {
        return includeInHover;
    }

    public void setIncludeInHover(boolean includeInHover)
    {
        this.includeInHover = includeInHover;
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

    public MultiValueMode getMultiValueMode()
    {
        if (multiValueMode == null) {
            return MultiValueMode.NONE;
        }
        else {
            return multiValueMode;
        }
    }

    public void setMode(MultiValueMode aMode)
    {
        multiValueMode = aMode;
    }
    
    public LinkMode getLinkMode()
    {
        if (linkMode == null) {
            return LinkMode.NONE;
        }
        else {
            return linkMode;
        }
    }

    public void setLinkMode(LinkMode aLinkMode)
    {
        linkMode = aLinkMode;
    }

    public String getLinkTypeName()
    {
        return linkTypeName;
    }

    public void setLinkTypeName(String aLinkTypeName)
    {
        linkTypeName = aLinkTypeName;
    }

    public String getLinkTypeRoleFeatureName()
    {
        return linkTypeRoleFeatureName;
    }

    public void setLinkTypeRoleFeatureName(String aLinkTypeRoleFeatureName)
    {
        linkTypeRoleFeatureName = aLinkTypeRoleFeatureName;
    }

    public String getLinkTypeTargetFeatureName()
    {
        return linkTypeTargetFeatureName;
    }

    public void setLinkTypeTargetFeatureName(String aLinkTypeTargetFeatureName)
    {
        linkTypeTargetFeatureName = aLinkTypeTargetFeatureName;
    }

    public boolean isRemember()
    {
        return remember;
    }

    public void setRemember(boolean aRemember)
    {
        remember = aRemember;
    }

    public boolean isHideUnconstraintFeature()
    {
        return hideUnconstraintFeature;
    }

    public void setHideUnconstraintFeature(boolean aHideUnconstraintFeature)
    {
        hideUnconstraintFeature = aHideUnconstraintFeature;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean aRequired)
    {
        required = aRequired;
    }
    
    /**
     * Returns {@code true} if this is not a plain UIMA feature type but a "virtual" feature that
     * must be mapped to a plain UIMA type (usually to String).
     * 
     * @deprecated This method should no longer be used. There is no direct replacement.
     */
    @Deprecated
    public boolean isVirtualFeature()
    {
        return getType().contains(":");
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
    public String toString()
    {
        return new ToStringBuilder(this)
                .append("name", name)
                .build();
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
