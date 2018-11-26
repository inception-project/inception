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
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

@Component
public class NamedEntityLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public NamedEntityLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
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
        TagSet nerTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-ne-webanno.json").getInputStream(),
                annotationSchemaService);

        AnnotationLayer neLayer = new AnnotationLayer(NamedEntity.class.getName(), "Named entity",
                SPAN_TYPE, aProject, true, AnchoringMode.TOKENS);
        neLayer.setAllowStacking(true);
        annotationSchemaService.createLayer(neLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, neLayer, "value",
                "value", CAS.TYPE_NAME_STRING, "Named entity type", nerTagSet));
    }
}
