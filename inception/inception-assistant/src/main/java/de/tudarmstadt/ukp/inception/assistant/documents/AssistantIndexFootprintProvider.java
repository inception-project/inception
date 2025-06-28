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
package de.tudarmstadt.ukp.inception.assistant.documents;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.footprint.Footprint;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProvider;

public class AssistantIndexFootprintProvider
    implements FootprintProvider
{
    private final RepositoryProperties repositoryProperties;

    public AssistantIndexFootprintProvider(RepositoryProperties aRepositoryProperties)
    {
        repositoryProperties = aRepositoryProperties;
    }

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Override
    public List<Footprint> getFootprint(Project aProject)
    {
        try {
            var totalIndexSize = FileUtils.sizeOfDirectory(getIndexDirectory(aProject));
            return asList( //
                    new Footprint("Assistant index", totalIndexSize, "thistle"));
        }
        catch (UncheckedIOException e) {
            return emptyList();
        }
    }

    private File getIndexDirectory(Project aProject)
    {
        return repositoryProperties.getPath().toPath() //
                .toAbsolutePath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aProject.getId())) //
                .resolve("assistant") //
                .resolve("index") //
                .toFile();
    }
}
