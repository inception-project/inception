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
package de.tudarmstadt.ukp.inception.pivot.api.extractor;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Provides the lookups an {@link ExtractorSupport} needs to rebuild its binding from a serialised
 * definition, scoped to the report's project. Lookups that fail record a problem on the ongoing
 * resolution and return {@code null} so the extractor is dropped gracefully.
 */
public interface ExtractorBindingResolutionContext
{
    /**
     * @return the layer with the given name in the project, or {@code null} (recording a problem)
     *         if it no longer exists.
     */
    AnnotationLayer resolveLayer(String aLayerName);

    /**
     * @return the feature with the given name on the named layer, or {@code null} (recording a
     *         problem) if either the layer or the feature no longer exists.
     */
    AnnotationFeature resolveFeature(String aLayerName, String aFeatureName);
}
