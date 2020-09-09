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
package de.tudarmstadt.ukp.inception.workload.registry;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.ExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadExtension;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagerAutoConfiguration#workloadRegistry}
 * </p>
 */
public class WorkloadRegistryImpl
    implements WorkloadRegistry
{
    private final List<WorkloadExtension> extensions;
    private final ExtensionPoint<Project, WorkloadExtension> extensionPoint;


    @Autowired
    public WorkloadRegistryImpl(
        List<WorkloadExtension> aExtensions,
        ExtensionPoint<Project, WorkloadExtension> aExtensionPoint)
    {
        extensions = aExtensions;
        extensionPoint = aExtensionPoint;
    }

    @Override
    public List<WorkloadExtension> getExtensions()
    {
        return extensions;
    }

    @Override
    public WorkloadExtension getExtension(String aExtension)
    {
        return extensionPoint.getExtensions().stream().filter(ext -> ext.getId().equals(aExtension))
            .findFirst().orElse(null);
    }
}

