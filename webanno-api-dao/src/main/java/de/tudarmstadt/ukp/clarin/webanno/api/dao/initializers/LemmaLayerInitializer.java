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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component
public class LemmaLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;
    
    @Autowired
    public LemmaLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries and attaches to token
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);

        AnnotationFeature tokenLemmaFeature = new AnnotationFeature(aProject, tokenLayer, "lemma",
                "lemma", Lemma.class.getName());
        annotationSchemaService.createFeature(tokenLemmaFeature);

        AnnotationLayer lemmaLayer = new AnnotationLayer(Lemma.class.getName(), "Lemma", SPAN_TYPE,
                aProject, true, SINGLE_TOKEN, NO_OVERLAP);
        lemmaLayer.setAttachType(tokenLayer);
        lemmaLayer.setAttachFeature(tokenLemmaFeature);
        annotationSchemaService.createLayer(lemmaLayer);

        AnnotationFeature lemmaFeature = new AnnotationFeature();
        lemmaFeature.setDescription("lemma Annotation");
        lemmaFeature.setName("value");
        lemmaFeature.setType(CAS.TYPE_NAME_STRING);
        lemmaFeature.setProject(aProject);
        lemmaFeature.setUiName("Lemma");
        lemmaFeature.setLayer(lemmaLayer);
        annotationSchemaService.createFeature(lemmaFeature);
    }
}
