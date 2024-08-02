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

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static java.util.Arrays.asList;

import java.io.Serializable;

import org.apache.uima.jcas.cas.TOP;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
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
 * A persistence object for an annotation layer. Currently, the builtin layers are: {@literal
 *  'pos' as  'span',
 *  'dependency' as 'relation',
 *   'named entity' as 'span',
 *   'coreference type' as 'chain', and
 *   'coreference' as 'chain'
 *  }
 */
@Entity
@Table(name = "annotation_type", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AnnotationLayer
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String uiName;

    @Column(nullable = false)
    private String type;

    @Column(length = 64000)
    private String description;

    private boolean enabled = true;

    @Column(name = "builtIn")
    private boolean builtIn = false;

    private boolean readonly = false;

    @Column(length = 64000)
    private String onClickJavascriptAction;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    // @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    @JoinColumn(name = "annotation_type", //
            foreignKey = @ForeignKey(name = "none", value = NO_CONSTRAINT))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private AnnotationLayer attachType;

    @ManyToOne
    @JoinColumn(name = "annotation_feature", //
            foreignKey = @ForeignKey(name = "none", value = NO_CONSTRAINT))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private AnnotationFeature attachFeature;

    @ManyToOne
    @JoinColumn(name = "project")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Project project;

    @Column(name = "crossSentence")
    private boolean crossSentence;

    @Column(name = "showTextInHover")
    private boolean showTextInHover = true;

    @Column(name = "linkedListBehavior")
    private boolean linkedListBehavior;

    @Column(name = "anchoring_mode")
    @Type(AnchoringModeType.class)
    private AnchoringMode anchoringMode = AnchoringMode.TOKENS;

    @Column(name = "overlap_mode")
    @Type(OverlapModeType.class)
    private OverlapMode overlapMode = OverlapMode.NO_OVERLAP;

    @Column(name = "validation_mode")
    @Type(ValidationModeType.class)
    private ValidationMode validationMode = ValidationMode.ALWAYS;

    @Column(length = 64000)
    private String traits;

    public AnnotationLayer()
    {
        // Required
    }

    public AnnotationLayer(String aName, String aUiName, String aType, Project aProject,
            boolean aBuiltIn, AnchoringMode aAnchoringMode, OverlapMode aOverlapMode)
    {
        setName(aName);
        setUiName(aUiName);
        setProject(aProject);
        setBuiltIn(aBuiltIn);
        setType(aType);
        setAnchoringMode(aAnchoringMode);
        setOverlapMode(aOverlapMode);
    }

    private AnnotationLayer(Builder builder)
    {
        this.id = builder.id;
        this.uiName = builder.uiName;
        this.type = builder.type;
        this.description = builder.description;
        this.enabled = builder.enabled;
        this.builtIn = builder.builtIn;
        this.readonly = builder.readonly;
        this.onClickJavascriptAction = builder.onClickJavascriptAction;
        this.name = builder.name;
        this.attachType = builder.attachType;
        this.attachFeature = builder.attachFeature;
        this.project = builder.project;
        this.crossSentence = builder.crossSentence;
        this.showTextInHover = builder.showTextInHover;
        this.linkedListBehavior = builder.linkedListBehavior;
        this.anchoringMode = builder.anchoringMode;
        this.overlapMode = builder.overlapMode;
        this.validationMode = builder.validationMode;
        this.traits = builder.traits;
    }

    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     * 
     * @return the id.
     */
    public Long getId()
    {
        return id;
    }

    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     * 
     * @param typeId
     *            the id.
     */
    public void setId(Long typeId)
    {
        this.id = typeId;
    }

    /**
     * The type of the annotation, either span, relation or chain
     * 
     * @return the type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * The type of the annotation, either span, relation or chain
     * 
     * @param aType
     *            the type.
     */
    public void setType(String aType)
    {
        type = aType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    /**
     * The name displayed to the user in the UI.
     * 
     * @return the displayed name.
     */
    public String getUiName()
    {
        return uiName;
    }

    /**
     * The name displayed to the user in the UI.
     * 
     * @param uiName
     *            the displayed name.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings).
     * 
     * @return whether the type is enabled.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings).
     * 
     * @param enabled
     *            if the type is enabled.
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     * 
     * @return if the type is built-in.
     */
    public boolean isBuiltIn()
    {
        return builtIn;
    }

    /**
     * Whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     * 
     * @param builtIn
     *            if the type is built-in.
     */
    public void setBuiltIn(boolean builtIn)
    {
        this.builtIn = builtIn;
    }

    /**
     * The name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     * 
     * @return the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * The name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     * 
     * @param annotationTypeName
     *            the type name.
     */
    public void setName(String annotationTypeName)
    {
        this.name = annotationTypeName;
    }

    /**
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     * 
     * @return the attach type name.
     */
    public AnnotationLayer getAttachType()
    {
        return attachType;
    }

    /**
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     * 
     * @param attachType
     *            the attach type name.
     */

    public void setAttachType(AnnotationLayer attachType)
    {
        this.attachType = attachType;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     *
     * @return the attach feature.
     */
    public AnnotationFeature getAttachFeature()
    {
        return attachFeature;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     * 
     * @param attachFeature
     *            the attach feature.
     */
    public void setAttachFeature(AnnotationFeature attachFeature)
    {
        this.attachFeature = attachFeature;
    }

    /**
     * the project id where this type belongs to
     * 
     * @return the project.
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * the project id where this type belongs to
     * 
     * @param project
     *            the project.
     */
    public void setProject(Project project)
    {
        this.project = project;
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
        AnnotationLayer other = (AnnotationLayer) obj;
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

    public AnchoringMode getAnchoringMode()
    {
        return anchoringMode;
    }

    public void setAnchoringMode(AnchoringMode aAnchoringMode)
    {
        anchoringMode = aAnchoringMode;
    }

    public void setAnchoringMode(boolean aLockToTokenOffset, boolean aMultipleTokens)
    {
        if (!aLockToTokenOffset && !aMultipleTokens) {
            anchoringMode = AnchoringMode.CHARACTERS;
        }
        else if (aLockToTokenOffset && !aMultipleTokens) {
            anchoringMode = AnchoringMode.SINGLE_TOKEN;
        }
        else if (aLockToTokenOffset && aMultipleTokens) {
            anchoringMode = AnchoringMode.TOKENS;
        }
        else if (!aLockToTokenOffset && aMultipleTokens) {
            anchoringMode = AnchoringMode.TOKENS;
        }
    }

    public ValidationMode getValidationMode()
    {
        return validationMode;
    }

    public void setValidationMode(ValidationMode aValidationMode)
    {
        validationMode = aValidationMode;
    }

    public boolean isAllowStacking()
    {
        return asList(OverlapMode.ANY_OVERLAP, OverlapMode.STACKING_ONLY).contains(overlapMode);
    }

    public OverlapMode getOverlapMode()
    {
        return overlapMode;
    }

    public void setOverlapMode(OverlapMode aOverlapMode)
    {
        overlapMode = aOverlapMode;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isShowTextInHover()
    {
        return showTextInHover;
    }

    public void setShowTextInHover(boolean showTextInHover)
    {
        this.showTextInHover = showTextInHover;
    }

    public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }

    /**
     * Controls whether the chain behaves like a linked list or like a set. When operating as a set,
     * chains are automatically threaded and no arrows and labels are displayed on arcs. When
     * operating as a linked list, chains are not threaded and arrows and labels are displayed on
     * arcs.
     *
     * @param aLinkedListBehavior
     *            whether to behave like a set.
     */
    public void setLinkedListBehavior(boolean aLinkedListBehavior)
    {
        linkedListBehavior = aLinkedListBehavior;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean aReadonly)
    {
        readonly = aReadonly;
    }

    public String getOnClickJavascriptAction()
    {
        return onClickJavascriptAction;
    }

    public void setOnClickJavascriptAction(String onClickAction)
    {
        this.onClickJavascriptAction = onClickAction;
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
        return "[" + name + "](" + id + ")";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @SuppressWarnings("hiding")
    public static final class Builder
    {
        private Long id;
        private String uiName;
        private String type;
        private String description;
        private boolean enabled = true;
        private boolean builtIn = false;
        private boolean readonly = false;
        private String onClickJavascriptAction;
        private String name;
        private AnnotationLayer attachType;
        private AnnotationFeature attachFeature;
        private Project project;
        private boolean crossSentence;
        private boolean showTextInHover = true;
        private boolean linkedListBehavior;
        private AnchoringMode anchoringMode = AnchoringMode.TOKENS;
        private OverlapMode overlapMode = OverlapMode.NO_OVERLAP;
        private ValidationMode validationMode = ValidationMode.ALWAYS;
        private String traits;

        private Builder()
        {
            // Nothing to do
        }

        public Builder forJCasClass(Class<? extends TOP> aClazz)
        {
            withName(aClazz.getName());
            withUiName(aClazz.getSimpleName());
            return this;
        }

        public Builder forUimaType(org.apache.uima.cas.Type aType)
        {
            withName(aType.getName());
            withUiName(aType.getShortName());
            return this;
        }

        public Builder withId(Long id)
        {
            this.id = id;
            return this;
        }

        public Builder withUiName(String uiName)
        {
            this.uiName = uiName;
            return this;
        }

        public Builder withType(String type)
        {
            this.type = type;
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

        public Builder withBuiltIn(boolean builtIn)
        {
            this.builtIn = builtIn;
            return this;
        }

        public Builder withReadonly(boolean readonly)
        {
            this.readonly = readonly;
            return this;
        }

        public Builder withOnClickJavascriptAction(String onClickJavascriptAction)
        {
            this.onClickJavascriptAction = onClickJavascriptAction;
            return this;
        }

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder withAttachType(AnnotationLayer attachType)
        {
            this.attachType = attachType;
            return this;
        }

        public Builder withAttachFeature(AnnotationFeature attachFeature)
        {
            this.attachType = attachFeature.getLayer();
            this.attachFeature = attachFeature;
            return this;
        }

        public Builder withProject(Project project)
        {
            this.project = project;
            return this;
        }

        public Builder withCrossSentence(boolean crossSentence)
        {
            this.crossSentence = crossSentence;
            return this;
        }

        public Builder withShowTextInHover(boolean showTextInHover)
        {
            this.showTextInHover = showTextInHover;
            return this;
        }

        public Builder withLinkedListBehavior(boolean linkedListBehavior)
        {
            this.linkedListBehavior = linkedListBehavior;
            return this;
        }

        public Builder withAnchoringMode(AnchoringMode anchoringMode)
        {
            this.anchoringMode = anchoringMode;
            return this;
        }

        public Builder withOverlapMode(OverlapMode overlapMode)
        {
            this.overlapMode = overlapMode;
            return this;
        }

        public Builder withValidationMode(ValidationMode validationMode)
        {
            this.validationMode = validationMode;
            return this;
        }

        public Builder withTraits(String traits)
        {
            this.traits = traits;
            return this;
        }

        public AnnotationLayer build()
        {
            return new AnnotationLayer(this);
        }
    }
}
