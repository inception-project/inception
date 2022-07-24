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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasMetadataUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(CasMetadataUtils.class);

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

        List<AnnotationFS> cmds = new ArrayList<>(
                CasUtil.select(aCas, getType(aCas, CASMetadata.class)));
        if (cmds.size() > 1) {
            throw new IllegalStateException("CAS contains more than one CASMetadata instance");
        }

        cmds.forEach(aCas::removeFsFromIndexes);
    }

    public static long getLastChanged(CAS aCas)
    {
        Type casMetadataType = getType(aCas, CASMetadata.class);
        Feature feature = casMetadataType.getFeatureByBaseName("lastChangedOnDisk");
        return aCas.select(casMetadataType).map(cmd -> cmd.getLongValue(feature)).findFirst()
                .orElse(-1l);
    }

    public static void addOrUpdateCasMetadata(CAS aCas, long aTimeStamp, SourceDocument aDocument,
            String aUsername)
        throws IOException
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data.
        if (aCas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            LOG.info(
                    "Annotation file of user [{}] for document [{}]({}) in project [{}]({}) "
                            + "does not support CASMetadata yet - not adding",
                    aUsername, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId());
            return;
        }

        Type casMetadataType = getType(aCas, CASMetadata.class);
        FeatureStructure cmd;
        List<AnnotationFS> cmds = new ArrayList<>(CasUtil.select(aCas, casMetadataType));
        if (cmds.size() > 1) {
            throw new IOException("CAS contains more than one CASMetadata instance!");
        }
        else if (cmds.size() == 1) {
            cmd = cmds.get(0);
        }
        else {
            cmd = aCas.createAnnotation(casMetadataType, 0, 0);
        }

        if (cmd.getType().getFeatureByBaseName("username") != null) {
            FSUtil.setFeature(cmd, "username", aUsername);
        }

        if (cmd.getType().getFeatureByBaseName("sourceDocumentId") != null) {
            FSUtil.setFeature(cmd, "sourceDocumentId", aDocument.getId());
        }

        if (cmd.getType().getFeatureByBaseName("sourceDocumentName") != null) {
            FSUtil.setFeature(cmd, "sourceDocumentName", aDocument.getName());
        }

        if (cmd.getType().getFeatureByBaseName("projectId") != null) {
            FSUtil.setFeature(cmd, "projectId", aDocument.getProject().getId());
        }

        if (cmd.getType().getFeatureByBaseName("projectName") != null) {
            FSUtil.setFeature(cmd, "projectName", aDocument.getProject().getName());
        }

        if (cmd.getType().getFeatureByBaseName("lastChangedOnDisk") != null) {
            FSUtil.setFeature(cmd, "lastChangedOnDisk", aTimeStamp);
            LOG.trace("CAS [{}] for [{}]@[{}]({}): set lastChangedOnDisk: {}", aCas.hashCode(),
                    aUsername, aDocument.getName(), aDocument.getId(), aTimeStamp);
        }

        aCas.addFsToIndexes(cmd);
    }

    public static Optional<String> getSourceDocumentName(CAS aCas)
    {
        try {
            FeatureStructure fs = CasUtil.selectSingle(aCas, getType(aCas, CASMetadata.class));
            return Optional.ofNullable(FSUtil.getFeature(fs, "sourceDocumentName", String.class));
        }
        catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
