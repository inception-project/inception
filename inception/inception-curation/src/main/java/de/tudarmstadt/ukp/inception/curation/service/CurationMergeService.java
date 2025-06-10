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
package de.tudarmstadt.ukp.inception.curation.service;

import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.merge.PerCasMergeContext;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;

public interface CurationMergeService
{
    /**
     * Merge the data of multiple CASes into a target CAS using the given merge strategy.
     * 
     * @param aDocument
     *            the source document for the target CAS
     * @param aTargetCasUserName
     *            the name of the user owning the target CAS
     * @param aTargetCas
     *            the target CAS
     * @param aCassesToMerge
     *            the CASes to be merged
     * @param aMergeStrategy
     *            the merge strategy
     * @param aLayers
     *            the layers to be merged
     * @param aClearTargetCas
     *            whether to clear the target CAS before merging
     * @return any messages generated during the merge process.
     * @throws UIMAException
     *             if there was an UIMA-level problem
     */
    PerCasMergeContext mergeCasses(SourceDocument aDocument, String aTargetCasUserName,
            CAS aTargetCas, Map<String, CAS> aCassesToMerge, MergeStrategy aMergeStrategy,
            List<AnnotationLayer> aLayers, boolean aClearTargetCas)
        throws UIMAException;

    /**
     * Merge the data of multiple CASes into a target CAS using the given merge strategy.
     * 
     * @param aDocument
     *            the source document for the target CAS
     * @param aTargetCasUserName
     *            the name of the user owning the target CAS
     * @param aTargetCas
     *            the target CAS
     * @param aCassesToMerge
     *            the CASes to be merged
     * @param aMergeStrategy
     *            the merge strategy
     * @return any messages generated during the merge process.
     * @throws UIMAException
     *             if there was an UIMA-level problem
     */
    PerCasMergeContext mergeCasses(SourceDocument aDocument, String aTargetCasUserName,
            CAS aTargetCas, Map<String, CAS> aCassesToMerge, MergeStrategy aMergeStrategy)
        throws UIMAException;
}
