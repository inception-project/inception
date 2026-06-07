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
package de.tudarmstadt.ukp.inception.pivot.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.pivot.aggregator.AggregatorSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.pivot.aggregator.CountAggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.aggregator.ValueMapAggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.aggregator.ValueSetAggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.extractor.AnnotatorExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.extractor.DocumentNameExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.extractor.ExtractorSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.pivot.extractor.TypeExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.exporter.PivotReportExporter;
import de.tudarmstadt.ukp.inception.pivot.page.PivotTableMenuItem;
import de.tudarmstadt.ukp.inception.pivot.report.ReportService;
import de.tudarmstadt.ukp.inception.pivot.report.ReportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;

@Configuration
public class PivotAutoConfiguration
{
    @Bean
    public PivotTableMenuItem pivotTableMenuItem()
    {
        return new PivotTableMenuItem();
    }

    @Bean
    public ExtractorSupportRegistry extractorSupportRegistry(
            @Lazy @Autowired(required = false) List<ExtractorSupport> aExtensions)
    {
        return new ExtractorSupportRegistryImpl(aExtensions);
    }

    @Bean
    public DocumentNameExtractorSupport documentNameExtractorSupport()
    {
        return new DocumentNameExtractorSupport();
    }

    @Bean
    public AnnotatorExtractorSupport annotatorExtractorSupport()
    {
        return new AnnotatorExtractorSupport();
    }

    @Bean
    public TypeExtractorSupport typeExtractorSupport()
    {
        return new TypeExtractorSupport();
    }

    @Bean
    public AggregatorSupportRegistry aggregatorSupportRegistry(
            @Lazy @Autowired(required = false) List<AggregatorSupport> aExtensions)
    {
        return new AggregatorSupportRegistryImpl(aExtensions);
    }

    @Bean
    public CountAggregatorSupport countAggregatorSupport()
    {
        return new CountAggregatorSupport();
    }

    @Bean
    public ValueSetAggregatorSupport valueSetAggregatorSupport()
    {
        return new ValueSetAggregatorSupport();
    }

    @Bean
    public ValueMapAggregatorSupport valueMapAggregatorSupport()
    {
        return new ValueMapAggregatorSupport();
    }

    @Bean
    public ReportService reportService(EntityManager aEntityManager,
            AnnotationSchemaService aSchemaService, ExtractorSupportRegistry aExtractorRegistry,
            AggregatorSupportRegistry aAggregatorRegistry, DocumentService aDocumentService,
            UserDao aUserService)
    {
        return new ReportServiceImpl(aEntityManager, aSchemaService, aExtractorRegistry,
                aAggregatorRegistry, aDocumentService, aUserService);
    }

    @Bean
    public PivotReportExporter pivotReportExporter(ReportService aReportService)
    {
        return new PivotReportExporter(aReportService);
    }
}
