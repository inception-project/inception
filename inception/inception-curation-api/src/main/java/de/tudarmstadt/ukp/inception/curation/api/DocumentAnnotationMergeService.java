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
package de.tudarmstadt.ukp.inception.curation.api;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.cas.AnnotationBase;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;

/**
 * Merges a single document-level annotation into a target CAS. This allows modules that cannot
 * depend on the curation implementation (e.g. the document metadata sidebar) to use the same merge
 * logic as the rest of curation.
 */
public interface DocumentAnnotationMergeService
{
    /**
     * Merge the given document-level annotation into the target CAS. On a layer that allows only a
     * single annotation per document, the first existing annotation in the target is updated.
     * Otherwise, or if the target has no annotation on the layer yet, a new annotation is created.
     * Only features that are curatable and that their feature support copies on a curation merge
     * are copied from the source annotation.
     * <p>
     * If the source CAS has not been upgraded since a feature was added to the layer, that feature
     * is merged as unset - the value it would have after an upgrade. When an existing annotation is
     * updated, this overwrites its value for the feature.
     * <p>
     * Events are only published if the merge succeeds.
     *
     * @param aDocument
     *            the document.
     * @param aTargetDataOwner
     *            the owner of the target CAS.
     * @param aLayer
     *            the layer of the annotation.
     * @param aTargetCas
     *            the target CAS.
     * @param aSourceAnnotation
     *            the annotation to merge.
     * @throws AnnotationException
     *             if an equivalent annotation already exists in the target or if a feature value
     *             could not be copied. In the latter case, a newly created annotation is removed
     *             again, but when an existing annotation is updated, the feature values copied
     *             before the failure remain set.
     */
    void mergeDocumentAnnotation(SourceDocument aDocument, String aTargetDataOwner,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationBase aSourceAnnotation)
        throws AnnotationException;
}
