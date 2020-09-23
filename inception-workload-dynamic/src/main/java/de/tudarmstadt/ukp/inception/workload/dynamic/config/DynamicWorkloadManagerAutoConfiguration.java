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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.extensions.DynamicWorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.extensions.DynamicWorkflowExtensionPointImpl;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManagementService;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManagementServiceImplBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.model.DynamicWorkflowManager;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.DynamicDefaultWorkflowTypeExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.DynamicRandomizedWorkflowTypeExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowManagerExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowManagerExtensionPointImpl;

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
    public DynamicWorkflowManagementService dynamicWorkflowManagementService(
            EntityManager aEntityManager)
    {
        return new DynamicWorkflowManagementServiceImplBase(aEntityManager);
    }

    @Bean
    public DynamicDefaultWorkflowTypeExtension dynamicDefaultWorkflowTypeExtension()
    {
        return new DynamicDefaultWorkflowTypeExtension();
    }

    @Bean
    public DynamicRandomizedWorkflowTypeExtension dynamicRandomizedWorkflowTypeExtension()
    {
        return new DynamicRandomizedWorkflowTypeExtension();
    }

    @Bean
    public DynamicWorkflowExtensionPoint dynamicWorkflowExtensionPoint(
            List<WorkflowManagerExtension> aWorkflowManagerExtensions)
    {
        return new DynamicWorkflowExtensionPointImpl(aWorkflowManagerExtensions);
    }

    @Bean
    public WorkflowManagerExtensionPoint workflowManagerExtensionPoint(
            List<WorkflowManagerExtension> aWorkflowManagerExtensions)
    {
        return new WorkflowManagerExtensionPointImpl(aWorkflowManagerExtensions);
    }

    @Bean
    public DynamicWorkflowManager dynamicWorkflowManager()
    {
        return new DynamicWorkflowManager();
    }

}
