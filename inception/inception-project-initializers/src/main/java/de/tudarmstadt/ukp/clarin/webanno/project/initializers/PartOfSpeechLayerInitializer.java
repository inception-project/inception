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
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#partOfSpeechLayerInitializer}.
 * </p>
 */
public class PartOfSpeechLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public PartOfSpeechLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Part-of-speech tagging";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Tagsets
                PartOfSpeechTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(POS.class.getName(), aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        TagSet posTagSet = annotationSchemaService
                .getTagSet(PartOfSpeechTagSetInitializer.TAG_SET_NAME, aProject);

        AnnotationLayer tokenLayer = annotationSchemaService.findLayer(aProject,
                Token.class.getName());

        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "Part of speech",
                SPAN_TYPE, aProject, true, SINGLE_TOKEN, NO_OVERLAP);

        AnnotationFeature tokenPosFeature = new AnnotationFeature(aProject, tokenLayer, "pos",
                "pos", POS.class.getName());
        annotationSchemaService.createFeature(tokenPosFeature);

        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        annotationSchemaService.createOrUpdateLayer(posLayer);

        AnnotationFeature xpos = new AnnotationFeature(aProject, posLayer, "PosValue", "XPOS",
                CAS.TYPE_NAME_STRING, "XPOS", null);
        xpos.setDescription("Language-specific part-of-speech tag");
        annotationSchemaService.createFeature(xpos);

        AnnotationFeature upos = new AnnotationFeature(aProject, posLayer, "coarseValue", "UPOS",
                CAS.TYPE_NAME_STRING, "UPOS", posTagSet);
        upos.setDescription("Universal part-of-speech tag");
        annotationSchemaService.createFeature(upos);

    }
}
