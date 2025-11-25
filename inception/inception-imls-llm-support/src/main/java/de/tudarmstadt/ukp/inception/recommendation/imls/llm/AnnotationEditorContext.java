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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class AnnotationEditorContext
{
    private final Project project;
    private final SourceDocument document;
    private final String dataOwner;

    private AnnotationEditorContext(Builder aBuilder)
    {
        project = aBuilder.project;
        document = aBuilder.document;
        dataOwner = aBuilder.dataOwner;
    }

    public String getDataOwner()
    {
        return dataOwner;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public Project getProject()
    {
        return project;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Project project;
        private SourceDocument document;
        private String dataOwner;

        private Builder()
        {
            // Nothing to do
        }

        public Builder withProject(Project aProject)
        {
            project = aProject;
            return this;
        }

        public Builder withDocument(SourceDocument aDocument)
        {
            document = aDocument;
            return this;
        }

        public Builder withDataOwner(String aDataOwner)
        {
            dataOwner = aDataOwner;
            return this;
        }

        public AnnotationEditorContext build()
        {
            return new AnnotationEditorContext(this);
        }
    }
}
