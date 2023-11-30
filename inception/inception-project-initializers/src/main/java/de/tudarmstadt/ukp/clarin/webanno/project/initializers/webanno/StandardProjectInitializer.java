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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers.webanno;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#standardProjectInitializer}.
 * </p>
 */
@Order(100000)
public class StandardProjectInitializer
    implements QuickProjectInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "thumbnail.svg");

    private final ProjectService projectService;

    @Autowired
    public StandardProjectInitializer(@Lazy ProjectService aProjectService)
    {
        projectService = aProjectService;
    }

    @Override
    public String getName()
    {
        return "Classic linguistic project";
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
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

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Comes pre-configured for many linguistic annotation tasks. "
                + "This is closest to the classic WebAnno project setup.");
    }
}
