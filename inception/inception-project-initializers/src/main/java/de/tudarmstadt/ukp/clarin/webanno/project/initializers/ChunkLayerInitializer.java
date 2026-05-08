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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#chunkLayerInitializer}.
 * </p>
 */
public class ChunkLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "ChunkLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public ChunkLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("chunk-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public String getName()
    {
        return "Chunking";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(Chunk.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var chunkLayer = new AnnotationLayer(Chunk.class.getName(), "Chunk", SpanLayerSupport.TYPE,
                project, true, TOKENS, NO_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(chunkLayer);

        AnnotationFeature chunkValueFeature = new AnnotationFeature();
        chunkValueFeature.setDescription("Chunk tag");
        chunkValueFeature.setName("chunkValue");
        chunkValueFeature.setType(TYPE_NAME_STRING);
        chunkValueFeature.setProject(project);
        chunkValueFeature.setUiName("Tag");
        chunkValueFeature.setLayer(chunkLayer);
        annotationSchemaService.createFeature(chunkValueFeature);
    }
}
