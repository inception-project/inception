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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getUimaTypeName;

import java.io.Serializable;

import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.CommonPrimitiveArray;
import org.apache.uima.jcas.cas.TOP;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A persistence object for an annotation feature. One or more features can be defined per
 * {@link AnnotationLayer}. At least one feature must be defined which serves as the “label
 * feature”. Additional features may be defined. Features have a type which can either be String,
 * integer, float, or boolean. To control the values that a String feature assumes, it can be
 * associated with a tagset. If the feature is defined on a span type, it is also possible to add a
 * feature of another span type which then serves as a label type for the first one
 */
@Entity
@Table(name = "annotation_feature", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "annotation_type", "name", "project" }) })
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    @JoinColumn(name = "annotation_type", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private AnnotationLayer layer;

    @ManyToOne
    @JoinColumn(name = "project")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "tag_set", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private TagSet tagset;

    @Column(nullable = false)
    private String uiName;

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
    @Type(MultiValueModeType.class)
    private MultiValueMode multiValueMode = MultiValueMode.NONE;

    @Column(name = "link_mode")
    @Type(LinkModeType.class)
    private LinkMode linkMode = LinkMode.NONE;

    @Column(name = "link_type_name")
    private String linkTypeName;

    @Column(name = "link_type_role_feature_name")
    private String linkTypeRoleFeatureName;

    @Column(name = "link_type_target_feature_name")
    private String linkTypeTargetFeatureName;

    @Column(length = 64000)
    private String traits;

    private boolean curatable = true;

    @Column(nullable = false)
    private int rank;

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

    private AnnotationFeature(Builder builder)
    {
        this.id = builder.id;
        this.type = builder.type;
        this.layer = builder.layer;
        this.project = builder.project;
        this.tagset = builder.tagset;
        this.uiName = builder.uiName;
        this.description = builder.description;
        this.enabled = builder.enabled;
        this.name = builder.name;
        this.visible = builder.visible;
        this.includeInHover = builder.includeInHover;
        this.remember = builder.remember;
        this.hideUnconstraintFeature = builder.hideUnconstraintFeature;
        this.required = builder.required;
        this.multiValueMode = builder.multiValueMode;
        this.linkMode = builder.linkMode;
        this.linkTypeName = builder.linkTypeName;
        this.linkTypeRoleFeatureName = builder.linkTypeRoleFeatureName;
        this.linkTypeTargetFeatureName = builder.linkTypeTargetFeatureName;
        this.traits = builder.traits;
        this.curatable = builder.curatable;
        this.rank = builder.rank;
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
     * The type of feature (string, integer, float, boolean, or a span type used as a label). Must
     * be a UIMA type name such as {@code uima.cas.String} or the name of a custom type.
     * 
     * @param type
     *            the type of feature.
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
     * @param layer
     *            the layer.
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
     * @param project
     *            the project.
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
     * @param uiName
     *            the name displayed in the UI.
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
     * @param description
     *            the description.
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
     * @param enabled
     *            if the layer is enabled.
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
     * @param name
     *            the UIMA type name.
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
     * @param visible
     *            if the feature value is rendered in the label.
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
     * The tagset which is used for this layer. If this is null, the label can be freely set (text
     * input field), otherwise only values from the tagset can be used as labels.
     * 
     * @param tagset
     *            the tagset.
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

    /**
     * @param aMode
     *            used to control if a feature can have multiple values and how these are
     *            represented.
     * 
     */
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

    /**
     * @param aLinkMode
     *            indicates what kind of relation if the feature is a link to another feature
     *            structure, e.g. {@link LinkMode#NONE}, {@link LinkMode#SIMPLE},
     *            {@link LinkMode#WITH_ROLE}.
     */
    public void setLinkMode(LinkMode aLinkMode)
    {
        linkMode = aLinkMode;
    }

    public String getLinkTypeName()
    {
        return linkTypeName;
    }

    /**
     * @param aLinkTypeName
     *            indicates the UIMA type that bears the role feature if a
     *            {@link LinkMode#WITH_ROLE} type is used.
     */
    public void setLinkTypeName(String aLinkTypeName)
    {
        linkTypeName = aLinkTypeName;
    }

    public String getLinkTypeRoleFeatureName()
    {
        return linkTypeRoleFeatureName;
    }

    /**
     * @param aLinkTypeRoleFeatureName
     *            the name of the feature bearing the role.
     */
    public void setLinkTypeRoleFeatureName(String aLinkTypeRoleFeatureName)
    {
        linkTypeRoleFeatureName = aLinkTypeRoleFeatureName;
    }

    public String getLinkTypeTargetFeatureName()
    {
        return linkTypeTargetFeatureName;
    }

    /**
     * @param aLinkTypeTargetFeatureName
     *            the name of the feature pointing to the target.
     * 
     */
    public void setLinkTypeTargetFeatureName(String aLinkTypeTargetFeatureName)
    {
        linkTypeTargetFeatureName = aLinkTypeTargetFeatureName;
    }

    public boolean isRemember()
    {
        return remember;
    }

    /**
     * @param aRemember
     *            whether the annotation detail editor should carry values of this feature over when
     *            creating a new annotation of the same type. This can be useful when creating many
     *            annotations of the same type in a row.
     * 
     */
    public void setRemember(boolean aRemember)
    {
        remember = aRemember;
    }

    public boolean isHideUnconstraintFeature()
    {
        return hideUnconstraintFeature;
    }

    /**
     * @param aHideUnconstraintFeature
     *            whether the feature should be showed if constraints rules are enabled and based on
     *            the evaluation of constraint rules on a feature.
     * 
     */
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
     * @return {@code true} if this is not a plain UIMA feature type but a "virtual" feature that
     *         must be mapped to a plain UIMA type (usually to String).
     */
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

    public boolean isCuratable()
    {
        return curatable;
    }

    public void setCuratable(boolean aCuratable)
    {
        curatable = aCuratable;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int aRank)
    {
        rank = aRank;
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
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

    public static Builder builder()
    {
        return new Builder();
    }

    @SuppressWarnings("hiding")
    public static final class Builder
    {
        private Long id;
        private String type;
        private AnnotationLayer layer;
        private Project project;
        private TagSet tagset;
        private String uiName;
        private String description;
        private boolean enabled = true;
        private String name;
        private boolean visible = true;
        private boolean includeInHover = false;
        private boolean remember;
        private boolean hideUnconstraintFeature;
        private boolean required;
        private MultiValueMode multiValueMode = MultiValueMode.NONE;
        private LinkMode linkMode = LinkMode.NONE;
        private String linkTypeName;
        private String linkTypeRoleFeatureName;
        private String linkTypeTargetFeatureName;
        private String traits;
        private boolean curatable = true;
        private int rank = 0;

        private Builder()
        {
        }

        public Builder forFeature(Feature aFeature)
        {
            withType(aFeature.getRange().getName());
            withName(aFeature.getShortName());
            withUiName(aFeature.getShortName());
            return this;
        }

        public Builder withId(Long id)
        {
            this.id = id;
            return this;
        }

        public Builder withType(String type)
        {
            this.type = type;
            return this;
        }

        public Builder withRange(String type)
        {
            return withType(type);
        }

        public Builder withRange(Class<? extends TOP> aClazz)
        {
            if (CommonPrimitiveArray.class.isAssignableFrom(aClazz)) {
                withMultiValueMode(ARRAY);
            }
            return withType(getUimaTypeName(aClazz));
        }

        public Builder withLayer(AnnotationLayer layer)
        {
            this.layer = layer;
            this.project = layer.getProject();
            return this;
        }

        public Builder withProject(Project project)
        {
            this.project = project;
            return this;
        }

        public Builder withTagset(TagSet tagset)
        {
            this.tagset = tagset;
            return this;
        }

        public Builder withUiName(String uiName)
        {
            this.uiName = uiName;
            return this;
        }

        public Builder withDescription(String description)
        {
            this.description = description;
            return this;
        }

        public Builder withEnabled(boolean enabled)
        {
            this.enabled = enabled;
            return this;
        }

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder withVisible(boolean visible)
        {
            this.visible = visible;
            return this;
        }

        public Builder withIncludeInHover(boolean includeInHover)
        {
            this.includeInHover = includeInHover;
            return this;
        }

        public Builder withRemember(boolean remember)
        {
            this.remember = remember;
            return this;
        }

        public Builder withHideUnconstraintFeature(boolean hideUnconstraintFeature)
        {
            this.hideUnconstraintFeature = hideUnconstraintFeature;
            return this;
        }

        public Builder withRequired(boolean required)
        {
            this.required = required;
            return this;
        }

        public Builder withMultiValueMode(MultiValueMode multiValueMode)
        {
            this.multiValueMode = multiValueMode;
            return this;
        }

        public Builder withLinkMode(LinkMode linkMode)
        {
            this.linkMode = linkMode;
            return this;
        }

        public Builder withLinkTypeName(String linkTypeName)
        {
            this.linkTypeName = linkTypeName;
            return this;
        }

        public Builder withLinkTypeRoleFeatureName(String linkTypeRoleFeatureName)
        {
            this.linkTypeRoleFeatureName = linkTypeRoleFeatureName;
            return this;
        }

        public Builder withLinkTypeTargetFeatureName(String linkTypeTargetFeatureName)
        {
            this.linkTypeTargetFeatureName = linkTypeTargetFeatureName;
            return this;
        }

        public Builder withTraits(String traits)
        {
            this.traits = traits;
            return this;
        }

        public Builder withCuratable(boolean curatable)
        {
            this.curatable = curatable;
            return this;
        }

        public Builder withRank(int aRank)
        {
            this.rank = aRank;
            return this;
        }

        public AnnotationFeature build()
        {
            return new AnnotationFeature(this);
        }
    }
}
