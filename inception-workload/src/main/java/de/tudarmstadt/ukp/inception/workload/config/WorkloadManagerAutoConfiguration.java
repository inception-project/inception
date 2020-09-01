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
package de.tudarmstadt.ukp.inception.workload.config;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.ExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadExtensionPointImpl;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementServiceImplBase;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistry;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistryImpl;
import de.tudarmstadt.ukp.inception.workload.settings.ProjectWorkloadSettingsPanelFactory;

/**
 * Provides Spring beans for the workload settings panel and the workload management service
 */
@Configuration
@Order(300)
public class WorkloadManagerAutoConfiguration
{

    @Bean
    public WorkloadRegistry workloadRegistry(
        List<WorkloadExtension> aWorkloadExtensions,
        ExtensionPoint<Project, WorkloadExtension> aExtensionPoint)
    {
        return new WorkloadRegistryImpl(aWorkloadExtensions,
            aExtensionPoint);
    }

    @Bean
    public ProjectWorkloadSettingsPanelFactory projectWorkloadSettingsPanelFactory()
    {
        return new ProjectWorkloadSettingsPanelFactory();
    }

    @Bean
    public WorkloadExtensionPoint workloadExtensionPoint(
        List<WorkloadExtension> aWorkloadExtensions)
    {
        return new WorkloadExtensionPointImpl(aWorkloadExtensions);
    }

    @Bean
    public WorkloadManagementService workloadManagementService(
        EntityManager aEntityManager, WorkloadRegistry aWorkloadRegistry)
    {
        return new WorkloadManagementServiceImplBase(aEntityManager, aWorkloadRegistry);
    }

    @Bean
    public WorkloadManager workloadManager()
    {
        return new WorkloadManager();
    }
}
