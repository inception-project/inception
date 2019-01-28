/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;

@Component
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
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer orthography = new AnnotationLayer(SofaChangeAnnotation.class.getName(),
                "Orthography Correction", SPAN_TYPE, aProject, true, AnchoringMode.SINGLE_TOKEN,
                OverlapMode.NO_OVERLAP);
        annotationSchemaService.createLayer(orthography);

        AnnotationFeature correction = new AnnotationFeature();
        correction.setDescription("Correct this token using the specified operation.");
        correction.setName("value");
        correction.setType(CAS.TYPE_NAME_STRING);
        correction.setProject(aProject);
        correction.setUiName("Correction");
        correction.setLayer(orthography);
        annotationSchemaService.createFeature(correction);

        TagSet operationTagset = annotationSchemaService.createTagSet(
                "operation to be done with specified in tokenIDs token/tokens in order to correct",
                "Operation", "en",
                new String[] { "replace", "insert_before", "insert_after", "delete" },
                new String[] { "replace", "insert before", "insert after", "delete" },
                aProject);

        AnnotationFeature operation = new AnnotationFeature();
        operation.setDescription("An operation taken to change this token.");
        operation.setName("operation");
        operation.setType(CAS.TYPE_NAME_STRING);
        operation.setProject(aProject);
        operation.setUiName("Operation");
        operation.setLayer(orthography);
        operation.setVisible(false);
        operation.setTagset(operationTagset);

        annotationSchemaService.createFeature(operation);
    }
}
