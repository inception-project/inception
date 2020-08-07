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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadAndWorkflowEngineFactory;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadAndWorkflowFactoryRegistry;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadAndWorkflowFactoryRegistryImplBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.WorkloadAndWorkflowService;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.WorkloadAndWorkflowServiceImplBase;

@Configuration
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true")
public class WorkloadServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    @Autowired
    public WorkloadAndWorkflowService workloadAndWorkflowService(
        ProjectService aProjectService)
    {
        return new WorkloadAndWorkflowServiceImplBase(entityManager, aProjectService);
    }

    @Bean
    public WorkloadAndWorkflowFactoryRegistry workloadAndWorkflowFactoryRegistry(
        @Lazy @Autowired(required = false) List<WorkloadAndWorkflowEngineFactory> aExtensions)
    {
        return new WorkloadAndWorkflowFactoryRegistryImplBase(aExtensions);
    }
}
