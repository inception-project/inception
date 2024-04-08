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
package de.tudarmstadt.ukp.inception.project.initializers.neannotation;

import static de.tudarmstadt.ukp.inception.schema.exporters.TagsetImportExportUtils.importTagSetFromJson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TagSetInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config.WikiDataLinkingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WikiDataLinkingProjectInitializersAutoConfiguration#namedEntitySampleDataTagSetInitializer}.
 * </p>
 */
public class NamedEntitySampleDataTagSetInitializer
    implements TagSetInitializer
{
    // Name must match the name in the tag set file loaded by this initializer!
    public static final String TAG_SET_NAME = "Named entity example tags";

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public NamedEntitySampleDataTagSetInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return TAG_SET_NAME;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsTagSet(getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        try (var is = getClass().getResourceAsStream("tagsets/foodista.json")) {
            var tagset = importTagSetFromJson(aRequest.getProject(), is, annotationSchemaService);
            tagset.setName(TAG_SET_NAME);
            annotationSchemaService.createTagSet(tagset);
        }
    }
}
