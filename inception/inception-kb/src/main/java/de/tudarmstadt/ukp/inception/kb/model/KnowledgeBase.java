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
package de.tudarmstadt.ukp.inception.kb.model;

import static de.tudarmstadt.ukp.inception.kb.reification.Reification.NONE;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.reification.ReificationType;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "knowledgebase", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project", "name", }) })
@NamedQueries({
        @NamedQuery(name = "KnowledgeBase.getByProject", query = "from KnowledgeBase kb where kb.project = :project order by lower(kb.name)"),
        @NamedQuery(name = "KnowledgeBase.getByName", query = "from KnowledgeBase kb where kb.project = :project and kb.name = :name "),
        @NamedQuery(name = "KnowledgeBase.getByProjectWhereEnabledTrue", query = "from KnowledgeBase kb where kb.project = :project and kb.enabled = true "
                + "order by lower(kb.name)") })
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
    @Type(KnowledgeBaseType.class)
    private RepositoryType type;

    /**
     * The IRI for a object describing A being of type class, e.g. rdfs:Class, owl:Class or entity
     * (Q35120) in Wikidata
     */
    @Column(nullable = false)
    private String classIri;

    /**
     * The IRI for a property describing A being a subclass B, e.g. rdfs:subClassOf or subclass of
     * (P279) in Wikidata
     */
    @Column(nullable = false)
    private String subclassIri;

    /**
     * The IRI for a property describing A being of type B, e.g. rdfs:type or instance of (P31) in
     * Wikidata
     */
    @Column(nullable = false)
    private String typeIri;

    /**
     * The IRI for a property describing B being a subproperty of A
     */
    @Column(nullable = false)
    private String subPropertyIri;

    /**
     * The IRI for a property describing B being a description of A, e.g. schema:description
     */
    @Column(nullable = false)
    private String descriptionIri;

    /**
     * The IRI used for full text search, e.g. {@code bif:contains} or
     * {@code http://www.openrdf.org/contrib/lucenesail#}. If this field is null, then FTS is not
     * supported.
     */
    @Column(nullable = true)
    private String fullTextSearchIri;

    /**
     * The IRI for a property describing B being a label for A, e.g. rdfs:label
     */
    @Column(nullable = false)
    private String labelIri;

    /**
     * The IRI for an object describing A is of type propertyType, e.g. rdf:Property
     */
    @Column(nullable = false)
    private String propertyTypeIri;

    /**
     * The IRI for a label of a property
     */
    @Column(nullable = false)
    private String propertyLabelIri;

    /**
     * The IRI for a description of a property
     */
    @Column(nullable = false)
    private String propertyDescriptionIri;

    /**
     * The IRI for a property marking a resources as deprecated
     */
    @Column(nullable = false)
    private String deprecationPropertyIri;

    /**
     * The IRI of the default dataset
     */
    @Column(nullable = true)
    private String defaultDatasetIri;

    @Column(nullable = false)
    private boolean readOnly;

    @Column(nullable = false)
    private boolean useFuzzy;

    /**
     * Whether the kb is available in the UI (outside of the project settings).
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    @Type(ReificationType.class)
    private Reification reification = NONE;

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
    private Set<String> rootConcepts = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "knowledgebase_add_match_props")
    @Column(name = "name")
    private Set<String> additionalMatchingProperties = new LinkedHashSet<>();

    /**
     * The default language for labels and descriptions of KB elements
     */
    @Column
    private String defaultLanguage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "knowledgebase_add_languages")
    @Column(name = "name")
    private Set<String> additionalLanguages = new LinkedHashSet<>();

    /**
     * Limits the number of results that can be retrieved from a SPARQL query.
     */
    @Column(nullable = false)
    private int maxResults;

    /**
     * Whether to prevent the validation of the SSL certificate of a remote knowledge base
     */
    @Column(nullable = false)
    private boolean skipSslValidation = false;

    @Column(length = 64000)
    private String traits;

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(String aRepositoryId)
    {
        repositoryId = aRepositoryId;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public RepositoryType getType()
    {
        return type;
    }

    public void setType(RepositoryType aType)
    {
        type = aType;
    }

    public String getClassIri()
    {
        return classIri;
    }

    public void setClassIri(String aClassIri)
    {
        classIri = aClassIri;
    }

    public String getSubclassIri()
    {
        return subclassIri;
    }

    public void setSubclassIri(String aSubclassIri)
    {
        subclassIri = aSubclassIri;
    }

    public String getTypeIri()
    {
        return typeIri;
    }

    public void setTypeIri(String aTypeIri)
    {
        typeIri = aTypeIri;
    }

    public void setPropertyDescriptionIri(String aPropertyDescriptionIri)
    {
        propertyDescriptionIri = aPropertyDescriptionIri;
    }

    public String getSubPropertyIri()
    {
        return subPropertyIri;
    }

    public void setSubPropertyIri(String aSubPropertyIri)
    {
        subPropertyIri = aSubPropertyIri;
    }

    public String getDescriptionIri()
    {
        return descriptionIri;
    }

    public void setDescriptionIri(String aDescriptionIri)
    {
        descriptionIri = aDescriptionIri;
    }

    public String getLabelIri()
    {
        return labelIri;
    }

    public void setLabelIri(String aLabelIri)
    {
        labelIri = aLabelIri;
    }

    public String getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public void setPropertyTypeIri(String aPropertyTypeIri)
    {
        propertyTypeIri = aPropertyTypeIri;
    }

    public String getFullTextSearchIri()
    {
        return fullTextSearchIri;
    }

    public void setFullTextSearchIri(String aFtsIri)
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

    public String getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public void setPropertyLabelIri(String aPropertyLabelIri)
    {
        propertyLabelIri = aPropertyLabelIri;
    }

    public String getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public void setDeprecationPropertyIri(String aDeprecationPropertyIri)
    {
        deprecationPropertyIri = aDeprecationPropertyIri;
    }

    public String getDeprecationPropertyIri()
    {
        return deprecationPropertyIri;
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

    public boolean isSupportConceptLinking()
    {
        return fullTextSearchIri != null;
    }

    public String getBasePrefix()
    {
        return basePrefix;
    }

    public void setBasePrefix(String aBasePrefix)
    {
        basePrefix = aBasePrefix;
    }

    public Set<String> getRootConcepts()
    {
        return rootConcepts;
    }

    public void setRootConcepts(Collection<String> aExplicitlyDefinedRootConcepts)
    {
        rootConcepts = new LinkedHashSet<>(aExplicitlyDefinedRootConcepts);
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int aSparqlQueryResultLimit)
    {
        maxResults = aSparqlQueryResultLimit;
    }

    public void applyMapping(KnowledgeBaseMapping aMapping)
    {
        setClassIri(aMapping.getClassIri());
        setSubclassIri(aMapping.getSubclassIri());
        setTypeIri(aMapping.getTypeIri());
        setSubPropertyIri(aMapping.getSubPropertyIri());
        setDescriptionIri(aMapping.getDescriptionIri());
        setLabelIri(aMapping.getLabelIri());
        setPropertyTypeIri(aMapping.getPropertyTypeIri());
        setPropertyLabelIri(aMapping.getPropertyLabelIri());
        setPropertyDescriptionIri(aMapping.getPropertyDescriptionIri());
        setDeprecationPropertyIri(aMapping.getDeprecationPropertyIri());
    }

    public void applyRootConcepts(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getRootConcepts() == null) {
            rootConcepts = emptySet();
        }
        else {
            rootConcepts = new LinkedHashSet<>(aProfile.getRootConcepts());
        }
    }

    public void applyAdditionalMatchingProperties(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getAdditionalMatchingProperties() == null) {
            additionalMatchingProperties = emptySet();
        }
        else {
            additionalMatchingProperties = new LinkedHashSet<>(
                    aProfile.getAdditionalMatchingProperties());
        }
    }

    public void applyProfile(KnowledgeBaseProfile aProfile)
    {
        setType(aProfile.getType());
        setName(aProfile.getName());
        setFullTextSearchIri(aProfile.getAccess().getFullTextSearchIri());
        setDefaultLanguage(aProfile.getDefaultLanguage());
        setDefaultDatasetIri(aProfile.getDefaultDataset());
        setReification(aProfile.getReification());

        applyRootConcepts(aProfile);
        applyMapping(aProfile.getMapping());
        applyAdditionalLanguages(aProfile);
    }

    public void applyAdditionalLanguages(KnowledgeBaseProfile aProfile)
    {
        if (aProfile.getAdditionalLanguages() == null) {
            additionalLanguages = emptySet();
        }
        else {
            additionalLanguages = new LinkedHashSet<>(aProfile.getAdditionalLanguages());
        }
    }

    public String getDefaultDatasetIri()
    {
        return defaultDatasetIri;
    }

    public void setDefaultDatasetIri(String aDefaultDatasetIri)
    {
        defaultDatasetIri = aDefaultDatasetIri;
    }

    public void setSkipSslValidation(boolean aSkipSslValidation)
    {
        skipSslValidation = aSkipSslValidation;
    }

    public boolean isSkipSslValidation()
    {
        return skipSslValidation;
    }

    public void setAdditionalMatchingProperties(Collection<String> aProperties)
    {
        additionalMatchingProperties = new LinkedHashSet<>(aProperties);
    }

    public Set<String> getAdditionalMatchingProperties()
    {
        return additionalMatchingProperties;
    }

    public void setAdditionalLanguages(Collection<String> aAdditionalLanguages)
    {
        additionalLanguages = new LinkedHashSet<>();
        if (aAdditionalLanguages != null) {
            additionalLanguages.addAll(aAdditionalLanguages);
        }
    }

    public Set<String> getAdditionalLanguages()
    {
        return additionalLanguages;
    }

    public boolean isUseFuzzy()
    {
        return useFuzzy;
    }

    public void setUseFuzzy(boolean aUseFuzzy)
    {
        useFuzzy = aUseFuzzy;
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
        return "[" + name + "](" + repositoryId + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBase that = (KnowledgeBase) o;
        return Objects.equals(repositoryId, that.repositoryId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(repositoryId, name);
    }
}
