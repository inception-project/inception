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
package de.tudarmstadt.ukp.clarin.webanno.api.type;

import org.apache.uima.cas.CAS;

/**
 * Helpers for reading the document identity recorded in the INCEpTION-internal {@link CASMetadata}.
 * <p>
 * These tolerate older type systems where the {@code CASMetadata} type exists but does not (yet)
 * declare all of its features - mirroring the feature-presence tolerance applied when the metadata
 * is written. Reading the values via the generated accessors would instead throw if a feature is
 * absent. The identity is only used for debugging/transparency, so a missing feature must never
 * abort the operation that reads it.
 */
public final class CasMetadataSupport
{
    private CasMetadataSupport()
    {
        // No instances
    }

    /**
     * @param aCas
     *            a CAS.
     * @return the project name recorded in the CAS' {@link CASMetadata}, or {@code null} if the CAS
     *         has no {@code CASMetadata} type, no {@code CASMetadata} instance, or a
     *         {@code CASMetadata} type that does not declare the {@code projectName} feature.
     */
    public static String getProjectName(CAS aCas)
    {
        return getIdentityFeature(aCas, CASMetadata._FeatName_projectName);
    }

    /**
     * @param aCas
     *            a CAS.
     * @return the source document name recorded in the CAS' {@link CASMetadata}, or {@code null}
     *         (see {@link #getProjectName(CAS)} for the cases in which {@code null} is returned).
     */
    public static String getSourceDocumentName(CAS aCas)
    {
        return getIdentityFeature(aCas, CASMetadata._FeatName_sourceDocumentName);
    }

    private static String getIdentityFeature(CAS aCas, String aFeatureName)
    {
        var type = aCas.getTypeSystem().getType(CASMetadata.class.getName());
        if (type == null) {
            return null;
        }

        var feature = type.getFeatureByBaseName(aFeatureName);
        if (feature == null) {
            // An older type system may declare the CASMetadata type without all of its features.
            return null;
        }

        var instances = aCas.select(type).toList();
        if (instances.isEmpty()) {
            return null;
        }

        return instances.get(0).getStringValue(feature);
    }
}
