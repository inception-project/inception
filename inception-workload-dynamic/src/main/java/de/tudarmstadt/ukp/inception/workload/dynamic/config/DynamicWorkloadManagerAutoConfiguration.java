/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.workload.dynamic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.workload.dynamic.extension.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.settings.ProjectWorkloadSettingsPanelFactory;

@Order(300)
@Configuration
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true")
public class DynamicWorkloadManagerAutoConfiguration
{
    @Bean
    public DynamicWorkloadExtension dynamicWorkloadExtension()
    {
        return new DynamicWorkloadExtension();
    }

    @Bean
    public ProjectWorkloadSettingsPanelFactory projectWorkloadSettingsPanelFactory()
    {
        return new ProjectWorkloadSettingsPanelFactory();
    }
}
