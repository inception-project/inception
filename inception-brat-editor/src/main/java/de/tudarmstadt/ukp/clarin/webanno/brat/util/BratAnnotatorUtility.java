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
package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

/**
 * Utility methods.
 */
public class BratAnnotatorUtility
{
    public static boolean isDocumentFinished(DocumentService aRepository,
            AnnotatorState aBratAnnotatorModel)
    {
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
                // Load document freshly from DB so we get the latest state. The document state
                // in the annotator state might be stale.
                SourceDocument doc = aRepository.getSourceDocument(
                        aBratAnnotatorModel.getDocument().getProject().getId(),
                        aBratAnnotatorModel.getDocument().getId());
                return doc.getState().equals(SourceDocumentState.CURATION_FINISHED);
            }
            else {
                // if annotationDocument is finished, disable editing
                AnnotationDocument adoc = aRepository.getAnnotationDocument(
                        aBratAnnotatorModel.getDocument(), aBratAnnotatorModel.getUser());

                return adoc.getState().equals(AnnotationDocumentState.FINISHED);
            }
        }
        catch (Exception e) {
            return false;
        }
    }
}
