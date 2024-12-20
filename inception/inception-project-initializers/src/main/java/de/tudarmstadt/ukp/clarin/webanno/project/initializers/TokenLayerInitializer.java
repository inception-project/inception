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

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerFactory;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#tokenLayerInitializer}.
 * </p>
 */
public class TokenLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public TokenLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Tokens";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return emptyList();
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(Token.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var tokenLayer = LayerFactory.tokenLayer(project) //
                .withEnabled(false) //
                .build();

        annotationSchemaService.createOrUpdateLayer(tokenLayer);
    }
}
