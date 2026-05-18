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
package de.tudarmstadt.ukp.inception.workload.ui;

import java.io.IOException;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * SPI that supplies per-document weights for the project progress chart's Y axis. Implemented by a
 * module that depends on the search index (e.g. {@code inception-search-core}); the workload module
 * itself cannot depend on search-core without creating a cycle.
 */
public interface ProgressWeighter
{
    /**
     * @param aMetric
     *            requested metric — implementations should return {@code null} for metrics they do
     *            not support (the caller falls back to unit weights).
     * @return map from source document id to weight, or {@code null} if {@code aMetric} is not
     *         supported. An empty map is a valid result and means "no weight known for any doc".
     */
    Map<Long, Long> getWeights(User aUser, Project aProject, ProgressMetric aMetric)
        throws IOException;
}
