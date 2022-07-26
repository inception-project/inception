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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CHAIN_TYPE;
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
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#coreferenceLayerInitializer}.
 * </p>
 */
public class CoreferenceLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    private final String COREFERENCE_LAYER_NAME = "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference";

    @Autowired
    public CoreferenceLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Co-reference annotation";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Tagsets
                CoreferenceTypeTagSetInitializer.class, CoreferenceRelationTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(COREFERENCE_LAYER_NAME, aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        TagSet corefTypeTagSet = annotationSchemaService
                .getTagSet(CoreferenceTypeTagSetInitializer.TAG_SET_NAME, aProject);
        TagSet corefRelTagSet = annotationSchemaService
                .getTagSet(CoreferenceRelationTagSetInitializer.TAG_SET_NAME, aProject);

        AnnotationLayer base = new AnnotationLayer(COREFERENCE_LAYER_NAME, "Coreference",
                CHAIN_TYPE, aProject, true, AnchoringMode.TOKENS, OverlapMode.ANY_OVERLAP);
        base.setCrossSentence(true);
        annotationSchemaService.createOrUpdateLayer(base);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, base, "referenceType",
                "referenceType", CAS.TYPE_NAME_STRING, "Coreference type", corefTypeTagSet));
        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, base, "referenceRelation", "referenceRelation",
                        CAS.TYPE_NAME_STRING, "Coreference relation", corefRelTagSet));
    }
}
