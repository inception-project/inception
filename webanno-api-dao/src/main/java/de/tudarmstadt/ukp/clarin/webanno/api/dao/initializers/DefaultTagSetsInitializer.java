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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class DefaultTagSetsInitializer
    implements TagSetInitializer
{
    private final AnnotationSchemaService annotationSchemaService;
    
    @Autowired
    public DefaultTagSetsInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return Collections.emptyList();
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-pos-stts.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/de-dep-tiger.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-dep-sd.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/en-pos-ptb-tt.json").getInputStream(),
                annotationSchemaService);
        JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-pos-upos.json").getInputStream(),
                annotationSchemaService);
    }
}
