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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WorkloadAndWorkflowEngineFactoryImplBase<T>
    implements WorkloadAndWorkflowEngineFactory<T>
{
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public T createTraits()
    {
        return null;
    }

    @Override
    public T readTraits(WorkloadAndWorkflow aWorkloadAndWorkflow)
    {
        if (aWorkloadAndWorkflow.getTraits() == null) {
            return createTraits();
        }

        T traits = null;
        try {
            traits = fromJsonString((Class<T>) createTraits().getClass(), aWorkloadAndWorkflow.getTraits());
        }
        catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = createTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(WorkloadAndWorkflow aWorkloadAndWorkflow, Object aTraits)
    {
        try {
            String json = toJsonString(aTraits);
            aWorkloadAndWorkflow.setTraits(json);
        }
        catch (IOException e) {
            log.error("Error while writing traits", e);
        }
    }
}
