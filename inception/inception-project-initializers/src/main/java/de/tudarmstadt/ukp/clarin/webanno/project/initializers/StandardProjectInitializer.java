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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#standardProjectInitializer}.
 * </p>
 */
public class StandardProjectInitializer
    implements QuickProjectInitializer
{
    private final ProjectService projectService;

    @Autowired
    public StandardProjectInitializer(@Lazy ProjectService aProjectService)
    {
        projectService = aProjectService;
    }

    @Override
    public String getName()
    {
        return "Standard project";
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return projectService.listProjectInitializers().stream() //
                // .filter(initializer -> initializer instanceof LayerInitializer) //
                .filter(initializer -> initializer.applyByDefault())
                .map(initializer -> initializer.getClass()) //
                .collect(Collectors.toList());
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        // Nothing to do - all initialization is already done by the dependencies
    }
}
