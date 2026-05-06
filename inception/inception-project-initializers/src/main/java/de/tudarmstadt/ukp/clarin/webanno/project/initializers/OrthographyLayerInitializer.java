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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
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
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#orthographyLayerInitializer}.
 * </p>
 */
public class OrthographyLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "OrthographyLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public OrthographyLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Spelling correction";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("orthography-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Tagsets
                SofaChangeOperationTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(SofaChangeAnnotation.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var orthography = new AnnotationLayer(SofaChangeAnnotation.class.getName(),
                "Orthography Correction", SpanLayerSupport.TYPE, project, true, SINGLE_TOKEN,
                NO_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(orthography);

        var correction = new AnnotationFeature();
        correction.setDescription("Correct this token using the specified operation.");
        correction.setName("value");
        correction.setType(TYPE_NAME_STRING);
        correction.setProject(project);
        correction.setUiName("Correction");
        correction.setLayer(orthography);
        annotationSchemaService.createFeature(correction);

        var operationTagset = annotationSchemaService
                .getTagSet(SofaChangeOperationTagSetInitializer.TAG_SET_NAME, project);

        var operation = new AnnotationFeature();
        operation.setDescription("An operation taken to change this token.");
        operation.setName("operation");
        operation.setType(TYPE_NAME_STRING);
        operation.setProject(project);
        operation.setUiName("Operation");
        operation.setLayer(orthography);
        operation.setVisible(false);
        operation.setTagset(operationTagset);
        operation.setRequired(true);

        annotationSchemaService.createFeature(operation);
    }
}
