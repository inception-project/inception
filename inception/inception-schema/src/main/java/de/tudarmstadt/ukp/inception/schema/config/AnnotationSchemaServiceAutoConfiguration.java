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
package de.tudarmstadt.ukp.inception.schema.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.exporters.AnnotationDocumentExporter;
import de.tudarmstadt.ukp.inception.schema.exporters.LayerExporter;
import de.tudarmstadt.ukp.inception.schema.exporters.TagSetExporter;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceEventAdapter;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@EnableConfigurationProperties({ //
        AnnotationSchemaPropertiesImpl.class, //
        StringFeatureSupportPropertiesImpl.class })
public class AnnotationSchemaServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean(AnnotationSchemaService.SERVICE_NAME)
    public AnnotationSchemaService annotationSchemaService(
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            AnnotationSchemaProperties aAnnotationEditorProperties,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                aApplicationEventPublisher, aAnnotationEditorProperties, entityManager);
    }

    @Bean
    public FeatureSupportRegistry featureSupportRegistry(
            @Lazy @Autowired(required = false) List<FeatureSupport<?>> aFeatureSupports)
    {
        return new FeatureSupportRegistryImpl(aFeatureSupports);
    }

    @Bean
    public BooleanFeatureSupport booleanFeaturesupport()
    {
        return new BooleanFeatureSupport();
    }

    @Bean
    public NumberFeatureSupport numberFeaturesupport()
    {
        return new NumberFeatureSupport();
    }

    @Bean
    public StringFeatureSupport stringFeaturesupport(StringFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        return new StringFeatureSupport(aProperties, aSchemaService);
    }

    @Bean
    public MultiValueStringFeatureSupport multiValueStringFeatureSupport(
            StringFeatureSupportProperties aProperties, AnnotationSchemaService aSchemaService)
    {
        return new MultiValueStringFeatureSupport(aProperties, aSchemaService);
    }

    @Bean
    public LayerSupportRegistry layerSupportRegistry(
            @Lazy @Autowired(required = false) List<LayerSupport<?, ?>> aLayerSupports)
    {
        return new LayerSupportRegistryImpl(aLayerSupports);
    }

    @Bean
    public LayerBehaviorRegistry LayerBehaviorRegistry(
            @Lazy @Autowired(required = false) List<LayerBehavior> aLayerSupports)
    {
        return new LayerBehaviorRegistryImpl(aLayerSupports);
    }

    @Bean
    public AnnotationSchemaServiceEventAdapter annotationSchemaServiceEventAdapter(
            AnnotationSchemaService aService)
    {
        return new AnnotationSchemaServiceEventAdapter(aService);
    }

    @Bean
    public AnnotationDocumentExporter annotationDocumentExporter(DocumentService aDocumentService,
            UserDao aUserRepository, DocumentImportExportService aImportExportService,
            RepositoryProperties aRepositoryProperties)
    {
        return new AnnotationDocumentExporter(aDocumentService, aUserRepository,
                aImportExportService, aRepositoryProperties);
    }

    @Bean
    public LayerExporter layerExporter(AnnotationSchemaService aAnnotationService)
    {
        return new LayerExporter(aAnnotationService);
    }

    @Bean
    public TagSetExporter tagSetExporter(AnnotationSchemaService aAnnotationService)
    {
        return new TagSetExporter(aAnnotationService);
    }
}
