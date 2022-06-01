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
package de.tudarmstadt.ukp.inception.project.initializers.basic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.config.InceptionBasicProjectInitializersAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionBasicProjectInitializersAutoConfiguration#basicProjectInitializer}.
 * </p>
 */
public class BasicProjectInitializer
    implements QuickProjectInitializer
{
    private final ApplicationContext context;

    public BasicProjectInitializer(ApplicationContext aContext)
    {
        context = aContext;
    }

    @Override
    public String getName()
    {
        return "Basic annotation (span/relation)";
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        List<Class<? extends ProjectInitializer>> dependencies = new ArrayList<>();
        dependencies.add(BasicSpanLayerInitializer.class);
        dependencies.add(BasicRelationLayerInitializer.class);

        if (context.getBeanNamesForType(BasicSpanRecommenderInitializer.class).length > 0) {
            dependencies.add(BasicSpanRecommenderInitializer.class);
        }

        if (context.getBeanNamesForType(BasicRelationRecommenderInitializer.class).length > 0) {
            dependencies.add(BasicRelationRecommenderInitializer.class);
        }

        return dependencies;
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        // Nothing to do - all initialization is already done by the dependencies
    }
}
