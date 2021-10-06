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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.curation.merge.DefaultMergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.MergeIncompleteStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategyFactoryExtensionPoint;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategyFactoryExtensionPointImpl;
import de.tudarmstadt.ukp.inception.curation.merge.ThresholdBasedMergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.ThresholdBasedMergeStrategyFactoryImpl;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.service.CurationServiceImpl;
import de.tudarmstadt.ukp.inception.curation.settings.CurationProjectSettingsMenuItem;
import de.tudarmstadt.ukp.inception.curation.settings.CurationProjectSettingsPanelFactory;

@Configuration
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
}
