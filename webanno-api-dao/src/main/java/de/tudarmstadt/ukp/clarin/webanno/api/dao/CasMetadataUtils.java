/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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
    
    public static void failOnConcurrentModification(JCas aJcas, File aCasFile,
            SourceDocument aDocument, String aUsername)
        throws IOException
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data.
        if (aJcas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            LOG.info("Annotation file [{}] of user [{}] for document [{}]({}) in project [{}]({}) "
                    + "does not support CASMetadata yet - unable to detect concurrent modifications",
                    aCasFile.getName(), aUsername, aDocument.getName(),
                    aDocument.getId(), aDocument.getProject().getName(),
                    aDocument.getProject().getId());
            return;
        }
        
        List<CASMetadata> cmds = new ArrayList<>(JCasUtil.select(aJcas, CASMetadata.class));
        if (cmds.size() > 1) {
            throw new IOException("CAS contains more than one CASMetadata instance");
        }
        else if (cmds.size() == 1) {
            CASMetadata cmd = cmds.get(0);
            if (aCasFile.lastModified() != cmd.getLastChangedOnDisk()) {
                throw new IOException(
                        "Detected concurrent modification to file on disk (expected timestamp: "
                                + cmd.getLastChangedOnDisk() + "; actual timestamp "
                                + aCasFile.lastModified() + ") - "
                                + "please try reloading brefore saving again.");
            }
        }
        else {
            LOG.info(
                    "Annotation file [{}] of user [{}] for document [{}]({}) in project "
                            + "[{}]({}) does not support CASMetadata yet - unable to check for "
                            + "concurrent modifications",
                    aCasFile.getName(), aUsername, aDocument.getName(), aDocument.getId(),
                    aDocument.getProject().getName(), aDocument.getProject().getId());
        }
    }
    
    public static void addOrUpdateCasMetadata(JCas aJCas, File aCasFile, SourceDocument aDocument,
            String aUsername)
        throws IOException
    {
        // If the type system of the CAS does not yet support CASMetadata, then we do not add it
        // and wait for the next regular CAS upgrade before we include this data.
        if (aJCas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            LOG.info("Annotation file [{}] of user [{}] for document [{}]({}) in project [{}]({}) "
                    + "does not support CASMetadata yet - not adding",
                    aCasFile.getName(), aUsername, aDocument.getName(),
                    aDocument.getId(), aDocument.getProject().getName(),
                    aDocument.getProject().getId());
            return;
        }
        
        CASMetadata cmd;
        List<CASMetadata> cmds = new ArrayList<>(JCasUtil.select(aJCas, CASMetadata.class));
        if (cmds.size() > 1) {
            throw new IOException("CAS contains more than one CASMetadata instance!");
        }
        else if (cmds.size() == 1) {
            cmd = cmds.get(0);
        }
        else {
            cmd = new CASMetadata(aJCas, 0, 0);
        }
        cmd.setUsername(aUsername);
        cmd.setSourceDocumentId(aDocument.getId());
        cmd.setProjectId(aDocument.getProject().getId());
        cmd.setLastChangedOnDisk(aCasFile.lastModified());
        aJCas.addFsToIndexes(cmd);
    }
}
