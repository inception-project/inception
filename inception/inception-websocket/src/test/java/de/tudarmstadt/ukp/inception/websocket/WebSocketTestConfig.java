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
package de.tudarmstadt.ukp.inception.websocket;

import static java.util.Arrays.asList;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.text.PretokenizedTextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.log.adapter.DocumentStateChangedEventAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;

@Configuration
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
"de.tudarmstadt.ukp.clarin.webanno.security.model",
"de.tudarmstadt.ukp.inception.log.model"})
public class WebSocketTestConfig
{

    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired EntityManager entityManager;

    @Bean
    public AnnotationSchemaService annotationSchemaService(
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                entityManager);
    }

    @Bean
    public LayerSupportRegistry layerSupportRegistry(
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new LayerSupportRegistryImpl(
                asList(new SpanLayerSupport(aFeatureSupportRegistry, null, null),
                        new RelationLayerSupport(aFeatureSupportRegistry, null, null),
                        new ChainLayerSupport(aFeatureSupportRegistry, null, null)));
    }

    @Bean
    public FeatureSupportRegistry featureSupportRegistry()
    {
        return new FeatureSupportRegistryImpl(asList(new NumberFeatureSupport(),
                new BooleanFeatureSupport(), new StringFeatureSupport()));
    }

    @Bean
    public ProjectService projectService(UserDao aUserDao,
            RepositoryProperties aRepositoryProperties,
            @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
    {
        return new ProjectServiceImpl(aUserDao, applicationEventPublisher,
                aRepositoryProperties, aInitializerProxy);
    }

    @Bean
    public UserDao userRepository()
    {
        return new UserDaoImpl();
    }

    @Bean
    public DocumentService documentService(RepositoryProperties aRepositoryProperties,
            CasStorageService aCasStorageService,
            DocumentImportExportService aImportExportService, ProjectService aProjectService,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                aImportExportService, aProjectService, aApplicationEventPublisher);
    }
    
    @Bean
    public DocumentImportExportService importExportService(
            RepositoryProperties aRepositoryProperties,
            AnnotationSchemaService aAnnotationSchemaService,
            CasStorageService aCasStorageService)
    {
        DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();
        return new DocumentImportExportServiceImpl(aRepositoryProperties,
                List.of(new TextFormatSupport(), new PretokenizedTextFormatSupport()),
                aCasStorageService, aAnnotationSchemaService, properties);
    }
    
    @Bean
    public List<EventLoggingAdapter<?>> eventLoggingAdapter()
    {
        return asList(new DocumentStateChangedEventAdapter());
    }
    
}
