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
package de.tudarmstadt.ukp.inception.kb;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Entity
@Table(name = "knowledgebase", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "repositoryId", "project" }) })
@NamedQueries({
        @NamedQuery(
        name = "KnowledgeBase.getByProject",
        query = "from KnowledgeBase kb where kb.project = :project"
    )
})
public class KnowledgeBase implements Serializable {

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
    @Lob
    @Column(length = 1024)
    private String name;
    
    @Enumerated
    private RepositoryType type;
    
    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
    
    public Project getProject() {
        return project;
    }
    
    public void setProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public RepositoryType getType() {
        return type;
    }
    
    public void setType(RepositoryType type) {
        this.type = type;
    }
    
    /**
     * Returns {@code true} if this knowledge base has a repository id, i.e. it is conceptually
     * linked to a {@link Project} and is managed by an RDF4J repository.
     * 
     * @return
     */
    public boolean isManagedRepository() {
        return !(repositoryId == null || repositoryId == "");
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("KnowledgeBase [");
        if (isManagedRepository()) {
            builder.append("id=");
            builder.append(repositoryId);
        } else {
            builder.append(project.toString());
            builder.append(", name=");
            builder.append(name);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KnowledgeBase other = (KnowledgeBase) obj;
        if (repositoryId == null) {
            if (other.repositoryId != null)
                return false;
        } else if (!repositoryId.equals(other.repositoryId))
            return false;
        return true;
    }

}
