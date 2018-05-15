/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.model;

import static de.tudarmstadt.ukp.inception.kb.reification.Reification.NONE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.eclipse.rdf4j.model.IRI;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@Entity
@Table(name = "knowledgebase",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "project", "name",  }) })
@NamedQueries({
    @NamedQuery(name = "KnowledgeBase.getByProject",
    query = "from KnowledgeBase kb where kb.project = :project order by lower(kb.name)"),
    @NamedQuery(name = "KnowledgeBase.getByName",
        query = "from KnowledgeBase kb where kb.project = :project and kb.name = :name "),
    @NamedQuery(name = "KnowledgeBase.getByProjectWhereEnabledTrue",
    query = "from KnowledgeBase kb where kb.project = :project and kb.enabled = true "
            + "order by lower(kb.name)") 
})
public class KnowledgeBase
    implements Serializable
{
    private static final long serialVersionUID = 5578346420963281980L;

    // set after being added to a repository
    @Id
    @Column(nullable = false)
    private String repositoryId;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    // Although "name" is basically included in "repositoryId", it is part of hashCode/equals.
    // Otherwise, two KBs not yet added to a repository could not be distinguished (because both
    // their repositoryIds are null, even though their names aren't).
    @Column
    private String name;

    @Enumerated
    private RepositoryType type;

    /**
     * The IRI for a object describing A being of type class, e.g. rdfs:Class, owl:Class or
     * entity (Q35120) in Wikidata
     */
    @Column(nullable = false)
    private IRI classIri;

    /**
     * The IRI for a property describing A being a subclass B, e.g. rdfs:subClassOf or
     * subclass of (P279) in Wikidata
     */
    @Column(nullable = false)
    private IRI subclassIri;

    /**
     * The IRI for a property describing A being of type B, e.g. rdfs:type or
     * instance of (P31) in Wikidata
     */
    @Column(nullable = false)
    private IRI typeIri;

    /**
     * The IRI for a property describing B being a description of A, e.g. schema:description
     */
    @Column(nullable = false)
    private IRI descriptionIri;

    @Column(nullable = false)
    private boolean readOnly;

    /**
     * Whether the kb is available in the UI (outside of the project settings).
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Reification reification = NONE;
    
    @Column(name = "supportConceptLinking", nullable = false)
    private boolean supportConceptLinking = false;
    
    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public RepositoryType getType()
    {
        return type;
    }

    public void setType(RepositoryType type)
    {
        this.type = type;
    }

    public IRI getClassIri()
    {
        return classIri;
    }

    public void setClassIri(IRI aClassIri)
    {
        classIri = aClassIri;
    }

    public IRI getSubclassIri()
    {
        return subclassIri;
    }

    public void setSubclassIri(IRI aSubclassIri)
    {
        subclassIri = aSubclassIri;
    }

    public IRI getTypeIri()
    {
        return typeIri;
    }

    public void setTypeIri(IRI aTypeIri)
    {
        typeIri = aTypeIri;
    }

    public IRI getDescriptionIri()
    {
        return descriptionIri;
    }

    public void setDescriptionIri(IRI aDescriptionIri)
    {
        descriptionIri = aDescriptionIri;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean isReadOnly)
    {
        readOnly = isReadOnly;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean isEnabled)
    {
        enabled = isEnabled;
    }

    public Reification getReification()
    {
        return reification;
    }

    public void setReification(Reification aReification)
    {
        reification = aReification;
    }

    /**
     * @return {@code true} if this knowledge base has a repository id, i.e. it is conceptually
     *         linked to a {@link Project} and is managed by an RDF4J repository.
     */
    public boolean isManagedRepository()
    {
        return !(repositoryId == null || isEmpty(repositoryId));
    }
    
    public void setSupportConceptLinking(boolean aSupportConceptLinking) {
        supportConceptLinking = aSupportConceptLinking;
    }
    
    public boolean isSupportConceptLinking() {
        return supportConceptLinking;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("KnowledgeBase [");
        if (isManagedRepository()) {
            builder.append("id=");
            builder.append(repositoryId);
        }
        else {
            builder.append(project);
            builder.append(", name=");
            builder.append(name);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBase that = (KnowledgeBase) o;
        return Objects.equals(repositoryId, that.repositoryId) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, name);
    }
}
