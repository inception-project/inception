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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectEntry
    implements Serializable
{
    private static final long serialVersionUID = -5715212313329449759L;

    private boolean selected;
    private final Project project;
    private final List<PermissionLevel> levels;

    public ProjectEntry(Project aProject, Collection<PermissionLevel> aLevels)
    {
        project = aProject;
        levels = aLevels.stream() //
                .sorted(comparing(PermissionLevel::getName)) //
                .collect(toList());
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean aSelected)
    {
        selected = aSelected;
    }

    public String getName()
    {
        return project.getName();
    }

    public String getSlug()
    {
        return project.getSlug();
    }

    public String getShortDescription()
    {
        if (StringUtils.isBlank(project.getDescription())) {
            return null;
        }

        return substringBefore(project.getDescription(), "\n");
    }

    public Project getProject()
    {
        return project;
    }

    public List<PermissionLevel> getLevels()
    {
        return levels;
    }

    public boolean hasAllLevels(Set<PermissionLevel> aLevels)
    {
        return levels.containsAll(aLevels);
    }

    @Override
    public String toString()
    {
        return "[" + project.getName() + "]";
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ProjectEntry)) {
            return false;
        }
        ProjectEntry castOther = (ProjectEntry) other;
        return new EqualsBuilder().append(project, castOther.project).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(project).toHashCode();
    }
}
