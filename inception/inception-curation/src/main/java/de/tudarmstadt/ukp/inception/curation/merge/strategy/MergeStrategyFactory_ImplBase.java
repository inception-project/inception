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
package de.tudarmstadt.ukp.inception.curation.merge.strategy;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;

public abstract class MergeStrategyFactory_ImplBase<T>
    implements MergeStrategyFactory<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected abstract T createTraits();

    @SuppressWarnings("unchecked")
    @Override
    public T readTraits(CurationWorkflow aCurationWorkflow)
    {
        if (aCurationWorkflow.getMergeStrategyTraits() == null) {
            return createTraits();
        }

        T traits = createTraits();
        if (traits != null) {
            try {
                traits = fromJsonString((Class<T>) traits.getClass(),
                        aCurationWorkflow.getMergeStrategyTraits());
            }
            catch (IOException e) {
                LOG.error("Error while reading traits", e);
            }
        }

        if (traits == null) {
            traits = createTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(CurationWorkflow aCurationWorkflow, T aTraits)
    {
        try {
            String json = toJsonString(aTraits);
            aCurationWorkflow.setMergeStrategyTraits(json);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }
}
