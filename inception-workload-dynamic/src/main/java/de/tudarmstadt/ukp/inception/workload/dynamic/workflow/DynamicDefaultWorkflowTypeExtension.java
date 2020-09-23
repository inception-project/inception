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
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow;

import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicDefaultWorkflowTypeExtension}
 * </p>
 */
public class DynamicDefaultWorkflowTypeExtension implements WorkflowManagerExtension
{

    public static final String DEFAULT_WORKFLOW_EXTENSION_ID = "default";

    @Override
    public String getId()
    {
        return DEFAULT_WORKFLOW_EXTENSION_ID;
    }

    @Override
    public String getLabel()
    {
        return "Default workflow";
    }
}
