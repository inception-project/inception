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
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.extractor.AnnotatorExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.extractor.DocumentNameExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.extractor.FeatureExtractorSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.pivot.extractor.LayerExtractorSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.pivot.extractor.TypeExtractorSupport;
import de.tudarmstadt.ukp.inception.pivot.page.PivotTableMenuItem;

@Configuration
public class PivotAutoConfiguration
{
    @Bean
    public PivotTableMenuItem pivotTableMenuItem()
    {
        return new PivotTableMenuItem();
    }

    @Bean
    public LayerExtractorSupportRegistry layerextractorSupportRegistry(
            @Lazy @Autowired(required = false) List<LayerExtractorSupport> aExtensions)
    {
        return new LayerExtractorSupportRegistryImpl(aExtensions);
    }

    @Bean
    public FeatureExtractorSupportRegistry featureExtractorSupportRegistry(
            @Lazy @Autowired(required = false) List<FeatureExtractorSupport> aExtensions)
    {
        return new FeatureExtractorSupportRegistryImpl(aExtensions);
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
}
