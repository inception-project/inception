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
        // and wait for the next regular CAS upgrade before we include this data.
        if (aCas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            throw new IllegalStateException("Annotation file of user [" + aUsername
                    + "] for document " + aDocument + " in project " + aDocument.getProject() + " "
                    + "does not support CASMetadata yet");
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
        return Optional.ofNullable(CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class)));
    }

    public static long getLastChanged(CAS aCas)
    {
        var casMetadataType = getType(aCas, CASMetadata.class);
        var feature = casMetadataType.getFeatureByBaseName(CASMetadata._FeatName_lastChangedOnDisk);
        return aCas.select(casMetadataType).map(cmd -> cmd.getLongValue(feature)).findFirst()
                .orElse(-1l);
    }

    public static Optional<String> getUsername(CAS aCas)
    {
        try {
            var fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional.ofNullable(
                    FSUtil.getFeature(fs, CASMetadata._FeatName_username, String.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> getSourceDocumentId(CAS aCas)
    {
        try {
            var fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional.ofNullable(
                    FSUtil.getFeature(fs, CASMetadata._FeatName_sourceDocumentId, Long.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getSourceDocumentName(CAS aCas)
    {
        try {
            var fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional.ofNullable(
                    FSUtil.getFeature(fs, CASMetadata._FeatName_sourceDocumentName, String.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> getProjectId(CAS aCas)
    {
        try {
            var fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional
                    .ofNullable(FSUtil.getFeature(fs, CASMetadata._FeatName_projectId, Long.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getProjectName(CAS aCas)
    {
        try {
            var fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional.ofNullable(
                    FSUtil.getFeature(fs, CASMetadata._FeatName_projectName, String.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
