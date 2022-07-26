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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#orthographyLayerInitializer}.
 * </p>
 */
public class OrthographyLayerInitializer
    implements LayerInitializer
{
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
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer orthography = new AnnotationLayer(SofaChangeAnnotation.class.getName(),
                "Orthography Correction", SPAN_TYPE, aProject, true, AnchoringMode.SINGLE_TOKEN,
                OverlapMode.NO_OVERLAP);
        annotationSchemaService.createOrUpdateLayer(orthography);

        AnnotationFeature correction = new AnnotationFeature();
        correction.setDescription("Correct this token using the specified operation.");
        correction.setName("value");
        correction.setType(CAS.TYPE_NAME_STRING);
        correction.setProject(aProject);
        correction.setUiName("Correction");
        correction.setLayer(orthography);
        annotationSchemaService.createFeature(correction);

        TagSet operationTagset = annotationSchemaService
                .getTagSet(SofaChangeOperationTagSetInitializer.TAG_SET_NAME, aProject);

        AnnotationFeature operation = new AnnotationFeature();
        operation.setDescription("An operation taken to change this token.");
        operation.setName("operation");
        operation.setType(CAS.TYPE_NAME_STRING);
        operation.setProject(aProject);
        operation.setUiName("Operation");
        operation.setLayer(orthography);
        operation.setVisible(false);
        operation.setTagset(operationTagset);
        operation.setRequired(true);

        annotationSchemaService.createFeature(operation);
    }
}
