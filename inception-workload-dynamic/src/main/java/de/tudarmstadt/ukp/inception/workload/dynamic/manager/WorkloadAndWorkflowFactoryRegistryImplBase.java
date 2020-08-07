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
package de.tudarmstadt.ukp.inception.workload.dynamic.manager;

import de.tudarmstadt.ukp.inception.workload.dynamic.config.WorkloadServiceAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadServiceAutoConfiguration#workloadAndWorkflowFactoryRegistry}.
 * </p>
 */
public class WorkloadAndWorkflowFactoryRegistryImplBase
    implements WorkloadAndWorkflowFactoryRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<WorkloadAndWorkflowEngineFactory> extensionsProxy;

    private Map<String, WorkloadAndWorkflowEngineFactory> extensions;

    public WorkloadAndWorkflowFactoryRegistryImplBase(
        @Lazy @Autowired(required = false) List<WorkloadAndWorkflowEngineFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }

    @Override
    public List<WorkloadAndWorkflowEngineFactory> getAllFactories()
    {
        List<WorkloadAndWorkflowEngineFactory> factories = new ArrayList<>();
        factories.addAll(extensions.values());
        return Collections.unmodifiableList(factories);
    }

    @Override
    public WorkloadAndWorkflowEngineFactory getFactory(String aId)
    {
        return extensions.get(aId);
    }
}
