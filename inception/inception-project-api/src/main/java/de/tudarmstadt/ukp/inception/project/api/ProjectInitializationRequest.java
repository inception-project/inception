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
package de.tudarmstadt.ukp.inception.project.api;

import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectInitializationRequest
{
    private final Project project;
    private final boolean includeSampleData;

    private ProjectInitializationRequest(Builder builder)
    {
        project = builder.project;
        includeSampleData = builder.includeSampleData;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Project getProject()
    {
        return project;
    }

    public boolean isIncludeSampleData()
    {
        return includeSampleData;
    }

    public static final class Builder
    {
        private Project project;
        private boolean includeSampleData;

        private Builder()
        {
        }

        public Builder withProject(Project aProject)
        {
            project = aProject;
            return this;
        }

        public Builder withIncludeSampleData(boolean aIncludeSampleData)
        {
            includeSampleData = aIncludeSampleData;
            return this;
        }

        public ProjectInitializationRequest build()
        {
            Objects.requireNonNull(project, "Parameter [project] must be specified.");

            return new ProjectInitializationRequest(this);
        }
    }
}
