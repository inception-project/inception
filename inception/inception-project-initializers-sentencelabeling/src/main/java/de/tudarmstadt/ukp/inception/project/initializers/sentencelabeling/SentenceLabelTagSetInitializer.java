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
package de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TagSetInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.config.InceptionSentenceLabelingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionSentenceLabelingProjectInitializersAutoConfiguration#sentenceLabelTagSetInitializer}.
 * </p>
 */
public class SentenceLabelTagSetInitializer
    implements TagSetInitializer
{
    public static final String SENTENCE_LABEL_TAG_SET_NAME = "Sentence labels";

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public SentenceLabelTagSetInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return SENTENCE_LABEL_TAG_SET_NAME;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsTagSet(getName(), aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        TagSet tagSet = new TagSet();
        tagSet.setProject(aProject);
        tagSet.setName(getName());
        tagSet.setDescription("Sentence labels");
        tagSet.setLanguage("en");
        tagSet.setCreateTag(false);

        annotationSchemaService.createTagSet(tagSet);

        Tag[] tags = new Tag[3];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = new Tag(tagSet, "Label " + (i + 1));
        }
        annotationSchemaService.createTags(tags);
    }
}
