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
package de.tudarmstadt.ukp.inception.curation.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.export.CuratedDocumentsExporter;
import de.tudarmstadt.ukp.inception.curation.export.CurationWorkflowExporter;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.DefaultMergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeIncompleteStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactoryExtensionPoint;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactoryExtensionPointImpl;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategyFactoryImpl;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeServiceImpl;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.service.CurationServiceImpl;
import de.tudarmstadt.ukp.inception.curation.settings.CurationProjectSettingsMenuItem;
import de.tudarmstadt.ukp.inception.curation.settings.CurationProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarPropertiesImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@EnableConfigurationProperties(CurationSidebarPropertiesImpl.class)
public class CurationServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public CurationProjectSettingsMenuItem curationProjectSettingsMenuItem()
    {
        return new CurationProjectSettingsMenuItem();
    }

    @Bean
    public CurationProjectSettingsPanelFactory curationProjectSettingsPanelFactory()
    {
        return new CurationProjectSettingsPanelFactory();
    }

    @Bean
    public MergeStrategyFactoryExtensionPoint mergeStrategyFactoryExtensionPoint(
            @Lazy @Autowired(required = false) List<MergeStrategyFactory<?>> aExtensions)
    {
        return new MergeStrategyFactoryExtensionPointImpl(aExtensions);
    }

    @Bean
    public CurationService curationService(
            MergeStrategyFactoryExtensionPoint aMergeStrategyFactoryExtensionPoint)
    {
        return new CurationServiceImpl(entityManager, aMergeStrategyFactoryExtensionPoint);
    }

    @Bean
    public CurationMergeService curationMergeService(AnnotationSchemaService aAnnotationService,
            DiffAdapterRegistry aDiffAdapterRegistry,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new CurationMergeServiceImpl(aAnnotationService, aDiffAdapterRegistry,
                aApplicationEventPublisher);
    }

    @Bean
    public DefaultMergeStrategyFactory defaultMergingStrategyFactory()
    {
        return new DefaultMergeStrategyFactory();
    }

    @Bean
    public MergeIncompleteStrategyFactory mergeIncompleteStrategyFactory()
    {
        return new MergeIncompleteStrategyFactory();
    }

    @Bean
    public ThresholdBasedMergeStrategyFactory thresholdBasedMergeStrategyFactory()
    {
        return new ThresholdBasedMergeStrategyFactoryImpl();
    }

    @Bean
    public CuratedDocumentsExporter curatedDocumentsExporter(DocumentService aDocumentService,
            DocumentImportExportService aImportExportService)
    {
        return new CuratedDocumentsExporter(aDocumentService, aImportExportService);
    }

    @Bean
    public CurationWorkflowExporter curationWorkflowExporter(CurationService aCurationService)
    {
        return new CurationWorkflowExporter(aCurationService);
    }
}
