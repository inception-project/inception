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
package de.tudarmstadt.ukp.inception.workload.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPointImpl;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementServiceImpl;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;
import de.tudarmstadt.ukp.inception.workload.task.exporter.WorkloadManagerExporter;
import jakarta.persistence.EntityManager;

@Configuration
public class WorkloadManagementAutoConfiguration<T>
{
    @Bean
    public WorkloadManagerExtensionPoint workloadExtensionPoint(
            @Lazy @Autowired(required = false) List<WorkloadManagerExtension<?>> aWorkloadExtensions)
    {
        return new WorkloadManagerExtensionPointImpl(aWorkloadExtensions);
    }

    @Bean
    public WorkloadManagementService workloadManagementService(EntityManager aEntityManager,
            WorkloadManagerExtensionPoint aWorkloadManagerExtensionPoint,
            SchedulingService aSchedulingService)
    {
        return new WorkloadManagementServiceImpl(aEntityManager, aWorkloadManagerExtensionPoint,
                aSchedulingService);
    }

    @Bean
    public WorkloadManager workloadManager()
    {
        return new WorkloadManager();
    }

    @Bean
    public WorkloadManagerExporter workloadManagerExporter(
            WorkloadManagementService aWorkloadManagementService)
    {
        return new WorkloadManagerExporter(aWorkloadManagementService);
    }
}
