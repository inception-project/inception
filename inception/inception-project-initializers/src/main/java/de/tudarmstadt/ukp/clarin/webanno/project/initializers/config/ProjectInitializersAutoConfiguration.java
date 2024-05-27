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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.project.initializers.ChunkLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.CoreferenceLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.CoreferenceRelationTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.CoreferenceTypeTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.DependencyFlavorTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.DependencyLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.DependencyTypeTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LemmaLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.MorphologicalFeaturesLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.OrthographyLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.PartOfSpeechLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.PartOfSpeechTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SemPredArgLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SentenceLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SofaChangeOperationTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SurfaceFormLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.empty.EmptyProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.webanno.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
public class ProjectInitializersAutoConfiguration
{
    @Bean
    public StandardProjectInitializer standardProjectInitializer(
            @Lazy ProjectService aProjectService)
    {
        return new StandardProjectInitializer(aProjectService);
    }

    @Bean
    public ChunkLayerInitializer chunkLayerInitializer(AnnotationSchemaService aSchemaService)
    {
        return new ChunkLayerInitializer(aSchemaService);
    }

    @Bean
    public CoreferenceLayerInitializer coreferenceLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new CoreferenceLayerInitializer(aSchemaService);
    }

    @Bean
    public CoreferenceRelationTagSetInitializer coreferenceRelationTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new CoreferenceRelationTagSetInitializer(aSchemaService);
    }

    @Bean
    public CoreferenceTypeTagSetInitializer coreferenceTypeTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new CoreferenceTypeTagSetInitializer(aSchemaService);
    }

    @Bean
    public DependencyFlavorTagSetInitializer dependencyFlavorTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new DependencyFlavorTagSetInitializer(aSchemaService);
    }

    @Bean
    public DependencyLayerInitializer dependencyLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new DependencyLayerInitializer(aSchemaService);
    }

    @Bean
    public DependencyTypeTagSetInitializer dependencyTypeTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new DependencyTypeTagSetInitializer(aSchemaService);
    }

    @Bean
    public LemmaLayerInitializer lemmaLayerInitializer(AnnotationSchemaService aSchemaService)
    {
        return new LemmaLayerInitializer(aSchemaService);
    }

    @Bean
    public MorphologicalFeaturesLayerInitializer morphologicalFeaturesLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new MorphologicalFeaturesLayerInitializer(aSchemaService);
    }

    @Bean
    public NamedEntityLayerInitializer namedEntityLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new NamedEntityLayerInitializer(aSchemaService);
    }

    @Bean
    public NamedEntityTagSetInitializer namedEntityTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new NamedEntityTagSetInitializer(aSchemaService);
    }

    @Bean
    public OrthographyLayerInitializer orthographyLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new OrthographyLayerInitializer(aSchemaService);
    }

    @Bean
    public PartOfSpeechLayerInitializer partOfSpeechLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new PartOfSpeechLayerInitializer(aSchemaService);
    }

    @Bean
    public PartOfSpeechTagSetInitializer partOfSpeechTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new PartOfSpeechTagSetInitializer(aSchemaService);
    }

    @Bean
    public SemPredArgLayerInitializer semPredArgLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new SemPredArgLayerInitializer(aSchemaService);
    }

    @Bean
    public SofaChangeOperationTagSetInitializer sofaChangeOperationTagSetInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new SofaChangeOperationTagSetInitializer(aSchemaService);
    }

    @Bean
    public SurfaceFormLayerInitializer SurfaceFormLayerInitializer(
            AnnotationSchemaService aSchemaService)
    {
        return new SurfaceFormLayerInitializer(aSchemaService);
    }

    @Bean
    public TokenLayerInitializer tokenLayerInitializer(AnnotationSchemaService aSchemaService)
    {
        return new TokenLayerInitializer(aSchemaService);
    }

    @Bean
    public SentenceLayerInitializer sentenceLayerInitializer(AnnotationSchemaService aSchemaService)
    {
        return new SentenceLayerInitializer(aSchemaService);
    }

    @Bean
    public EmptyProjectInitializer emptyProjectInitializer()
    {
        return new EmptyProjectInitializer();
    }
}
