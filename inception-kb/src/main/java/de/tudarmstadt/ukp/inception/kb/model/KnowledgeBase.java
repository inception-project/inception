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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;

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

    /**
     * The IRI used for full text search, e.g. bif:contains or http://www.openrdf.org/contrib/lucenesail#
     */
    @Column
    private IRI fullTextSearchIri;

    /**
     * The IRI for a property describing B being a label for A, e.g. rdfs:label 
     */
    @Column(nullable = false)
    private IRI labelIri;
    
    /**
     * The IRI for an object describing A is of type propertyType, e.g. rdf:Property 
     */
    @Column(nullable = false)
    private IRI propertyTypeIri;

    /**
     * The IRI for a label of a property
     */
    @Column(nullable = false)
    private IRI propertyLabelIri;

    /**
     * The IRI for a description of a property
     */
    @Column(nullable = false)
    private IRI propertyDescriptionIri;


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
    
    /**
     * All statements created in a local KB are prefixed with this string 
     */
    @Column(nullable = false)
    private String basePrefix = IriConstants.INCEPTION_NAMESPACE;
    
    /**
     * A List of explicitly defined root concepts that can be used if auto detection takes too long
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "knowledgebase_root_classes")
    @Column(name = "name")
    private List<IRI> explicitlyDefinedRootConcepts = new ArrayList<>();

    /**
     * The default language for labels and descriptions of KB elements
     */
    @Column
    private String defaultLanguage;

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

    public IRI getLabelIri()
    {
        return labelIri;
    }

    public void setLabelIri(IRI aLabelIri)
    {
        labelIri = aLabelIri;
    }

    public IRI getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public void setPropertyTypeIri(IRI aPropertyTypeIri)
    {
        propertyTypeIri = aPropertyTypeIri;
    }

    public IRI getFullTextSearchIri()
    {
        return fullTextSearchIri;
    }

    public void setFullTextSearchIri(IRI aFtsIri)
    {
        fullTextSearchIri = aFtsIri;
    }

    public String getDefaultLanguage()
    {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String aLanguage)
    {
        defaultLanguage = aLanguage;
    }

    public IRI getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public void setPropertyLabelIri(IRI aPropertyLabelIri)
    {
        propertyLabelIri = aPropertyLabelIri;
    }

    public IRI getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public void setPropertyDescriptionIri(IRI aPropertyDescriptionIri)
    {
        propertyDescriptionIri = aPropertyDescriptionIri;
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
    
    public String getBasePrefix()
    {
        return basePrefix;
    }

    public void setBasePrefix(String aBasePrefix)
    {
        basePrefix = aBasePrefix;
    }
    
    public List<IRI> getExplicitlyDefinedRootConcepts()
    {
        return explicitlyDefinedRootConcepts;
    }

    public void setExplicitlyDefinedRootConcepts(List<IRI> aExplicitlyDefinedRootConcepts)
    {
        explicitlyDefinedRootConcepts = aExplicitlyDefinedRootConcepts;
    }
    
    public void applyMapping(KnowledgeBaseMapping aMapping)
    {
        setClassIri(aMapping.getClassIri());
        setSubclassIri(aMapping.getSubclassIri());
        setTypeIri(aMapping.getTypeIri());
        setDescriptionIri(aMapping.getDescriptionIri());
        setLabelIri(aMapping.getLabelIri());
        setPropertyTypeIri(aMapping.getPropertyTypeIri());
        setPropertyLabelIri(aMapping.getPropertyLabelIri());
        setPropertyDescriptionIri(aMapping.getPropertyDescriptionIri());
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
