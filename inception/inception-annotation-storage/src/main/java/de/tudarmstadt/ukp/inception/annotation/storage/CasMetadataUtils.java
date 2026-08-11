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
package de.tudarmstadt.ukp.inception.annotation.storage;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasMetadataUtils
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Sentinel {@code lastChangedOnDisk} value returned by {@link #getLastChanged(CAS)} when no
     * timestamp is available - either because the CAS carries no {@link CASMetadata} instance or
     * because its (older) CASMetadata type does not include the {@code lastChangedOnDisk} feature.
     */
    public static final long UNKNOWN_CAS_TIMESTAMP = -1l;

    /**
     * Sentinel {@code lastChangedOnDisk} value used to mark a CAS as <i>transient</i>: it was
     * created on-the-fly (e.g. when reading under {@code SHARED_READ_ONLY_ACCESS} and no CAS exists
     * on disk yet) and carries identity metadata for debugging/transparency only, but must never be
     * persisted. This is deliberately distinct from {@link #UNKNOWN_CAS_TIMESTAMP}, so that a
     * not-yet-stamped CAS on its legitimate first write is not mistaken for a transient one.
     */
    public static final long TRANSIENT_CAS_TIMESTAMP = -2l;

    public static TypeSystemDescription getInternalTypeSystem()
    {
        return createTypeSystemDescription(
                "de/tudarmstadt/ukp/clarin/webanno/api/type/webanno-internal");
    }

    public static void clearCasMetadata(CAS aCas) throws IllegalStateException
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data.
        if (aCas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            return;
        }

        var cmds = aCas.select(CASMetadata.class).toList();
        if (cmds.size() > 1) {
            throw new IllegalStateException("CAS contains more than one CASMetadata instance");
        }

        cmds.forEach(aCas::removeFsFromIndexes);
    }

    public static void addOrUpdateCasMetadata(CAS aCas, long aTimeStamp, SourceDocument aDocument,
            String aUsername)
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data. This can happen
        // for CASes that were serialized with a type system predating the introduction of
        // CASMetadata - we must still be able to read those (e.g. for read-only access such as
        // export or CasDoctor checks) instead of failing outright.
        if (!supportsCasMetadata(aCas)) {
            LOG.debug("Annotation file of user [{}] for document {} in project {} does not support "
                    + "CASMetadata yet - not stamping it. The metadata will be added on the "
                    + "next regular CAS upgrade.", aUsername, aDocument, aDocument.getProject());
            return;
        }

        var casMetadataType = getType(aCas, CASMetadata.class);
        FeatureStructure cmd;
        var cmds = aCas.select(CASMetadata.class).toList();
        if (cmds.size() > 1) {
            throw new IllegalStateException("CAS contains more than one CASMetadata instance!");
        }

        if (cmds.size() == 1) {
            cmd = cmds.get(0);
        }
        else {
            cmd = aCas.createAnnotation(casMetadataType, 0, 0);
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_username) != null) {
            setFeature(cmd, CASMetadata._FeatName_username, aUsername);
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_sourceDocumentId) != null) {
            setFeature(cmd, CASMetadata._FeatName_sourceDocumentId, aDocument.getId());
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_sourceDocumentName) != null) {
            setFeature(cmd, CASMetadata._FeatName_sourceDocumentName, aDocument.getName());
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_projectId) != null) {
            setFeature(cmd, CASMetadata._FeatName_projectId, aDocument.getProject().getId());
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_projectName) != null) {
            setFeature(cmd, CASMetadata._FeatName_projectName, aDocument.getProject().getName());
        }

        if (cmd.getType().getFeatureByBaseName(CASMetadata._FeatName_lastChangedOnDisk) != null) {
            setFeature(cmd, CASMetadata._FeatName_lastChangedOnDisk, aTimeStamp);
            LOG.trace("CAS [{}] for [{}]@{}: set lastChangedOnDisk: {}", aCas.hashCode(), aUsername,
                    aDocument, aTimeStamp);
        }

        aCas.addFsToIndexes(cmd);
    }

    public static Optional<FeatureStructure> getCasMetadataFS(CAS aCas)
    {
        // An older type system may not declare CASMetadata at all - treat it as absent.
        var casMetadataType = aCas.getTypeSystem().getType(CASMetadata.class.getName());
        if (casMetadataType == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(CasUtil.selectSingle(aCas, casMetadataType));
    }

    public static long getLastChanged(CAS aCas)
    {
        // An older type system may not declare CASMetadata at all. We tolerate this here and treat
        // it as "no timestamp available".
        var casMetadataType = aCas.getTypeSystem().getType(CASMetadata.class.getName());
        if (casMetadataType == null) {
            return UNKNOWN_CAS_TIMESTAMP;
        }

        var feature = casMetadataType.getFeatureByBaseName(CASMetadata._FeatName_lastChangedOnDisk);
        if (feature == null) {
            // An older type system may have a CASMetadata type that does not yet include the
            // lastChangedOnDisk feature. We tolerate this here (just as addOrUpdateCasMetadata
            // does when stamping) and treat it as "no timestamp available".
            return UNKNOWN_CAS_TIMESTAMP;
        }
        return aCas.select(casMetadataType).map(cmd -> cmd.getLongValue(feature)).findFirst()
                .orElse(UNKNOWN_CAS_TIMESTAMP);
    }

    /**
     * @return whether the type system of the given CAS supports {@link CASMetadata}.
     */
    public static boolean supportsCasMetadata(CAS aCas)
    {
        return aCas.getTypeSystem().getType(CASMetadata.class.getName()) != null;
    }

    /**
     * @return whether the given CAS can be marked as transient - i.e. its type system declares the
     *         {@link CASMetadata} type <em>including</em> the {@code lastChangedOnDisk} feature
     *         that carries the {@link #TRANSIENT_CAS_TIMESTAMP} marker. Without that feature the
     *         marker cannot be set, so the CAS could not later be recognized as transient.
     */
    public static boolean canMarkTransient(CAS aCas)
    {
        var type = aCas.getTypeSystem().getType(CASMetadata.class.getName());
        return type != null
                && type.getFeatureByBaseName(CASMetadata._FeatName_lastChangedOnDisk) != null;
    }

    /**
     * @return whether the given CAS has been marked as transient (i.e. created on-the-fly and not
     *         backed by a file on disk) and therefore must not be persisted.
     * @see #TRANSIENT_CAS_TIMESTAMP
     */
    public static boolean isTransientCas(CAS aCas)
    {
        return supportsCasMetadata(aCas) && getLastChanged(aCas) == TRANSIENT_CAS_TIMESTAMP;
    }

    /**
     * Reads the given feature from the {@link CASMetadata} instance in the given CAS. Returns an
     * empty {@link Optional} if the type system does not declare {@link CASMetadata} at all, if the
     * (older) CASMetadata type does not declare the requested feature, or if the CAS does not
     * contain exactly one CASMetadata instance.
     */
    private static <T> Optional<T> getCasMetadataFeature(CAS aCas, String aFeatureName,
            Class<T> aClazz)
    {
        try {
            var maybeFs = getCasMetadataFS(aCas);
            if (maybeFs.isEmpty()) {
                return Optional.empty();
            }

            var fs = maybeFs.get();
            if (fs.getType().getFeatureByBaseName(aFeatureName) == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(FSUtil.getFeature(fs, aFeatureName, aClazz));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getUsername(CAS aCas)
    {
        return getCasMetadataFeature(aCas, CASMetadata._FeatName_username, String.class);
    }

    public static Optional<Long> getSourceDocumentId(CAS aCas)
    {
        return getCasMetadataFeature(aCas, CASMetadata._FeatName_sourceDocumentId, Long.class);
    }

    public static Optional<String> getSourceDocumentName(CAS aCas)
    {
        return getCasMetadataFeature(aCas, CASMetadata._FeatName_sourceDocumentName, String.class);
    }

    public static Optional<Long> getProjectId(CAS aCas)
    {
        return getCasMetadataFeature(aCas, CASMetadata._FeatName_projectId, Long.class);
    }

    public static Optional<String> getProjectName(CAS aCas)
    {
        return getCasMetadataFeature(aCas, CASMetadata._FeatName_projectName, String.class);
    }
}
