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

import de.tudarmstadt.ukp.inception.workload.dynamic.model.WorkloadAssignment;

public interface WorkloadAndWorkflowEngineFactory<T> {
    String getId();

    String getName();

    T createTraits();

    T readTraits(WorkloadAssignment aWorkloadAssignment);

    void writeTraits(WorkloadAssignment aWorkloadAssignment, T aTraits);
}
