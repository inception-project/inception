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
package de.tudarmstadt.ukp.inception.ui.agreement.page;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface AgreementService
{
    Map<SourceDocument, List<AnnotationDocument>> getDocumentsToEvaluate(Project aProject,
            List<SourceDocument> aDocuments, DefaultAgreementTraits aTraits);

    void exportPairwiseDiff(OutputStream aOut, AnnotationFeature aFeature, String aMeasure,
            DefaultAgreementTraits aTraits, User aCurrentUser, List<SourceDocument> aDocuments,
            String aAnnotator1, String aAnnotator2);

    void exportPairwiseDiff(OutputStream aOut, AnnotationLayer aLayer, String aMeasure,
            DefaultAgreementTraits aTraits, User aCurrentUser, List<SourceDocument> aDocuments,
            String aAnnotator1, String aAnnotator2);

    void exportDiff(OutputStream aOut, AnnotationFeature aFeature, DefaultAgreementTraits aTraits,
            User aCurrentUser, List<SourceDocument> aDocuments, List<String> aAnnotators);

    void exportDiff(OutputStream aOut, AnnotationLayer aLayer, DefaultAgreementTraits aTraits,
            User aCurrentUser, List<SourceDocument> aDocuments, List<String> aAnnotators);
}
