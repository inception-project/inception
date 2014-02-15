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
 * A persistence object for an annotation type. Currently, the types are:
 * {@literal
 *  'pos' as  'span',
 *  'dependency' as 'relation',
 *   'named entity' as 'span',
 *   'coreference type' as 'span', and
 *   'coreference' as 'relation'
 *  }
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
@Entity
@Table(name = "annotation_type", uniqueConstraints = { @UniqueConstraint(columnNames = { "type",
        "name", "project" }) })
public class AnnotationType
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @Column(nullable = false)
    private String uiName;

    @Column(nullable = false)
    private String type;

    @Lob
    private String description;

    private boolean enabled = true;

    private String labelFeatureName;

    private boolean builtIn = false;

    private boolean deletable = false;

    @Column(name = "name", nullable = false)
    private String name;

    private AnnotationType attachType;

    private AnnotationFeature attachFeature;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;
    

    private boolean lockToTokenOffset = true;
    
    private boolean allowSTacking;
    
    private boolean crossSentence;
    
    private boolean multipleTokens;

    /**
     *
     * a short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     */
    public long getId()
    {
        return id;
    }

    /**
     *
     * a short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     */
    public void setId(long typeId)
    {
        this.id = typeId;
    }

    /**
     *
     * The type of the annotation, either span, relation or chain
     */
    public String getType()
    {
        return type;
    }

    /**
     *
     * The type of the annotation, either span, relation or chain
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
     *
     * the name displayed to the user in the UI.
     */
    public String getUiName()
    {
        return uiName;
    }

    /**
     *
     * the name displayed to the user in the UI.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     *
     * whether the type is available in the UI (outside of the project settings).
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     *
     * whether the type is available in the UI (outside of the project settings).
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     *
     * the name of a feature of the annotation type whose value is used to represent an annotation
     * in the UI. E.g. for the POS type, this would be „PosValue“. In the future, an annotation type
     * can have additional features, but these are only shown in the UI after extra interaction,
     * e.g. when opening the annotation editing dialog or as a tooltip. In the future, this may be
     * an expression which allows showing multiple feature values, e.g. „${PosValue} ${begin}
     * ${end}“. See also section on feature definition below.
     */
    public String getLabelFeatureName()
    {
        return labelFeatureName;
    }

    /**
     *
     * the name of a feature of the annotation type whose value is used to represent an annotation
     * in the UI. E.g. for the POS type, this would be „PosValue“. In the future, an annotation type
     * can have additional features, but these are only shown in the UI after extra interaction,
     * e.g. when opening the annotation editing dialog or as a tooltip. In the future, this may be
     * an expression which allows showing multiple feature values, e.g. „${PosValue} ${begin}
     * ${end}“. See also section on feature definition below.
     */
    public void setLabelFeatureName(String labelFeatureName)
    {
        this.labelFeatureName = labelFeatureName;
    }

    /**
     *
     * whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     */

    public boolean isBuiltIn()
    {
        return builtIn;
    }

    /**
     *
     * whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     */

    public void setBuiltIn(boolean builtIn)
    {
        this.builtIn = builtIn;
    }

    /**
     *
     * whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     */
    public boolean isDeletable()
    {
        return deletable;
    }

    /**
     *
     * whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     */
    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    /**
     *
     * the name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     */
    public String getName()
    {
        return name;
    }

    /**
     *
     * the name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     */
    public void setName(String annotationTypeName)
    {
        this.name = annotationTypeName;
    }

    /**
     *
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     */
    public AnnotationType getAttachType()
    {
        return attachType;
    }

    /**
     *
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     */

    public void setAttachType(AnnotationType attachType)
    {
        this.attachType = attachType;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     *
     */
    public AnnotationFeature getAttachFeature()
    {
        return attachFeature;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     */
    public void setAttachFeature(AnnotationFeature attachFeature)
    {
        this.attachFeature = attachFeature;
    }

    /**
     *
     * the project id where this type belongs to
     */
    public Project getProject()
    {
        return project;
    }

    /**
     *
     * the project id where this type belongs to
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
        AnnotationType other = (AnnotationType) obj;
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

    public boolean isLockToTokenOffset()
    {
        return lockToTokenOffset;
    }

    public void setLockToTokenOffset(boolean lockToTokenOffset)
    {
        this.lockToTokenOffset = lockToTokenOffset;
    }

    public boolean isAllowSTacking()
    {
        return allowSTacking;
    }

    public void setAllowSTacking(boolean allowSTacking)
    {
        this.allowSTacking = allowSTacking;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isMultipleTokens()
    {
        return multipleTokens;
    }

    public void setMultipleTokens(boolean multipleTokens)
    {
        this.multipleTokens = multipleTokens;
    }

}
