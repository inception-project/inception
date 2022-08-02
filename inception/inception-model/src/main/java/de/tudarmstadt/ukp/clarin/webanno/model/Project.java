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

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.containsNone;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Type;

/**
 * A persistence object for a Project.
 */
@Entity
@Table(name = "project", uniqueConstraints = { @UniqueConstraint(columnNames = { "slug" }) })
public class Project
    implements Serializable
{
    private static final long serialVersionUID = -5426914078691460011L;

    public static final String PROJECT_NAME_ILLEGAL_CHARACTERS = "^/\\&*?+$![]";
    public static final int MIN_PROJECT_SLUG_LENGTH = 3;
    public static final int MAX_PROJECT_SLUG_LENGTH = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String slug;

    @Lob
    @Column(length = 64000)
    private String description;

    @Deprecated
    @Column(nullable = false)
    private String mode = "annotation";

    // version of the project
    private int version = 1;

    // Disable users from exporting annotation documents
    private boolean disableExport = false;

    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirectionType")
    private ScriptDirection scriptDirection;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

    @Column(nullable = true)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ProjectStateType")
    private ProjectState state;

    @Column(nullable = false)
    private boolean anonymousCuration;

    public Project()
    {
        // Nothing to do
    }

    /**
     * Constructor used for testing purposes. Set the {@link #name} to the same value as the
     * {@link #slug}.
     * 
     * @param aSlug
     *            the project's URL slug
     */
    public Project(String aSlug)
    {
        super();
        setName(aSlug);
        setSlug(aSlug);
        mode = "annotation";
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
        Validate.isTrue(isValidProjectName(aName), "Invalid project name: [%s]", aName);
        name = aName;
    }

    public void setSlug(String aSlug)
    {
        Validate.isTrue(isBlank(aSlug) || isValidProjectSlug(aSlug),
                format("Invalid project URL slug: [%s]", aSlug));
        slug = aSlug;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public boolean isDisableExport()
    {
        return disableExport;
    }

    public void setDisableExport(boolean disableExport)
    {
        this.disableExport = disableExport;
    }

    public ScriptDirection getScriptDirection()
    {
        // If unset, default to LTR - property was not present in older WebAnno versions
        if (scriptDirection == null) {
            return ScriptDirection.LTR;
        }
        else {
            return scriptDirection;
        }
    }

    public void setScriptDirection(ScriptDirection scriptDirection)
    {
        this.scriptDirection = scriptDirection;
    }

    @Deprecated
    public String getMode()
    {
        return mode;
    }

    @Deprecated
    public void setMode(String aMode)
    {
        this.mode = aMode;
    }

    @PrePersist
    protected void onCreate()
    {
        // When we import data, we set the fields via setters and don't want these to be
        // overwritten by this event handler.
        if (created == null) {
            created = new Date();
            updated = created;
        }
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }

    public ProjectState getState()
    {
        return state;
    }

    public void setState(ProjectState aState)
    {
        state = aState;
    }

    public boolean isAnonymousCuration()
    {
        return anonymousCuration;
    }

    public void setAnonymousCuration(boolean aAnonymousCuration)
    {
        anonymousCuration = aAnonymousCuration;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((slug == null) ? 0 : slug.hashCode());
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
        Project other = (Project) obj;
        if (slug == null) {
            if (other.slug != null) {
                return false;
            }
        }
        else if (!slug.equals(other.slug)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
    }

    /**
     * Check if the name is valid, SPecial characters are not allowed as a project/user name as it
     * will conflict with file naming system
     * 
     * @param aName
     *            a name.
     * @return if the name is valid.
     */
    public static boolean isValidProjectName(String aName)
    {
        return aName != null && containsNone(aName, PROJECT_NAME_ILLEGAL_CHARACTERS);
    }

    public static boolean isValidProjectSlug(String aSlug)
    {
        if (isEmpty(aSlug)) {
            return false;
        }

        if (aSlug.length() < MIN_PROJECT_SLUG_LENGTH || aSlug.length() > MAX_PROJECT_SLUG_LENGTH) {
            return false;
        }

        // Must start with a letter character
        if (!isValidProjectSlugInitialCharacter(aSlug.charAt(0))) {
            return false;
        }

        // Must consist only of valid characters
        for (int i = 0; i < aSlug.length(); i++) {
            if (!isValidProjectSlugCharacter(aSlug.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidProjectSlugInitialCharacter(char aChar)
    {
        return ('a' <= aChar && aChar <= 'z');
    }

    public static boolean isValidProjectSlugCharacter(char aChar)
    {
        return ('0' <= aChar && aChar <= '9') || ('a' <= aChar && aChar <= 'z') || aChar == '-'
                || aChar == '_';
    }
}
