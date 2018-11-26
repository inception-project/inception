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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component
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
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        TagSet posTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-ud.json").getInputStream(),
                annotationSchemaService);

        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);

        AnnotationLayer posLayer = new AnnotationLayer(POS.class.getName(), "POS", SPAN_TYPE,
                aProject, true, AnchoringMode.SINGLE_TOKEN);

        AnnotationFeature tokenPosFeature = new AnnotationFeature(aProject, tokenLayer, "pos",
                "pos", POS.class.getName());
        annotationSchemaService.createFeature(tokenPosFeature);

        posLayer.setAttachType(tokenLayer);
        posLayer.setAttachFeature(tokenPosFeature);
        annotationSchemaService.createLayer(posLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, posLayer, "PosValue",
                "PosValue", CAS.TYPE_NAME_STRING, "Part-of-speech tag", posTagSet));
    }
}
