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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static java.util.stream.Collectors.joining;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks if the type {@link CASMetadata} is defined in the type system of this CAS and whether its
 * definition is up-to-date. If the type is missing or outdated, then the application may not be
 * able to detect concurrent modifications.
 */
public class CASMetadataTypeIsPresentCheck
    implements Check
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String INTERNAL_TYPE_SYSTEM = //
            "/de/tudarmstadt/ukp/clarin/webanno/api/type/webanno-internal.xml";

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var casMetadataType = aCas.getTypeSystem().getType(CASMetadata._TypeName);

        if (casMetadataType == null) {
            aMessages.add(LogMessage.warn(this, "CAS needs upgrade to support CASMetadata which is "
                    + "required to detect concurrent modifications to CAS files."));
            return true;
        }

        var missingFeatures = getMissingFeatures(casMetadataType);
        if (!missingFeatures.isEmpty()) {
            aMessages.add(LogMessage.warn(this,
                    "CAS needs upgrade to bring CASMetadata up-to-date. The following features are "
                            + "missing: [%s].",
                    missingFeatures.stream().collect(joining(", "))));
        }

        if (aCas.select(CASMetadata.class).isEmpty()) {
            aMessages.add(LogMessage.warn(this,
                    "CAS contains no CASMetadata. Cannot check concurrent access."));
        }

        // This is an informative check - not critical, so we always pass it.
        return true;
    }

    /**
     * @return the names of the features which the current {@link CASMetadata} definition declares
     *         but which are absent from the CASMetadata type in the given CAS. An older CAS may
     *         carry a CASMetadata type that predates the addition of some features - such a CAS
     *         silently loses the functionality backed by those features (e.g. concurrent
     *         modification detection, which requires {@code lastChangedOnDisk}).
     */
    private List<String> getMissingFeatures(Type aCasMetadataType)
    {
        var missingFeatures = new ArrayList<String>();

        for (var expectedFeature : getExpectedFeatures()) {
            if (aCasMetadataType.getFeatureByBaseName(expectedFeature) == null) {
                missingFeatures.add(expectedFeature);
            }
        }

        return missingFeatures;
    }

    /**
     * @return the feature names declared by the current {@link CASMetadata} type definition. These
     *         are obtained from the internal type system description rather than being hard-coded
     *         so that this check does not go stale when features are added to CASMetadata.
     */
    private List<String> getExpectedFeatures()
    {
        try (var is = getClass().getResourceAsStream(INTERNAL_TYPE_SYSTEM)) {
            if (is == null) {
                LOG.warn("Unable to locate the internal type system at [{}] - cannot determine the "
                        + "expected CASMetadata features", INTERNAL_TYPE_SYSTEM);
                return List.of();
            }

            var tsd = UIMAFramework.getXMLParser()
                    .parseTypeSystemDescription(new XMLInputSource(is, null));

            var typeDescription = tsd.getType(CASMetadata._TypeName);
            if (typeDescription == null) {
                LOG.warn("Internal type system does not declare [{}] - cannot determine the "
                        + "expected CASMetadata features", CASMetadata._TypeName);
                return List.of();
            }

            return featureNames(typeDescription);
        }
        catch (Exception e) {
            // If we cannot load the internal type system, we simply do not report missing features
            // instead of failing the entire check.
            LOG.warn("Unable to determine the expected CASMetadata features", e);
            return List.of();
        }
    }

    private List<String> featureNames(TypeDescription aTypeDescription)
    {
        var features = aTypeDescription.getFeatures();
        if (features == null) {
            return List.of();
        }

        return List.of(features).stream().map(f -> f.getName()).toList();
    }
}
