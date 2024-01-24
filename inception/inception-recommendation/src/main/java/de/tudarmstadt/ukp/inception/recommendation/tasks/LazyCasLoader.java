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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class LazyCasLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final Project project;
    private final String dataOwner;

    private List<TrainingDocument> candidates;

    public LazyCasLoader(DocumentService aDocumentService, Project aProject, String aDataOwner)
    {
        documentService = aDocumentService;
        project = aProject;
        dataOwner = aDataOwner;
    }

    public List<CAS> getRelevantCasses(Recommender aRecommender) throws ConcurrentException
    {
        return get().stream() //
                .filter(e -> !aRecommender.getStatesIgnoredForTraining().contains(e.state)) //
                .map(e -> e.getCas()) //
                .filter(Objects::nonNull) //
                .filter(cas -> containsTargetTypeAndFeature(aRecommender, cas)) //
                .toList();
    }

    private Collection<TrainingDocument> get()
    {
        if (candidates == null) {
            candidates = readCasses();
        }
        return candidates;
    }

    private List<TrainingDocument> readCasses()
    {
        var casses = new ArrayList<TrainingDocument>();

        var allDocuments = documentService.listAllDocuments(project, dataOwner);
        for (var entry : allDocuments.entrySet()) {
            var sourceDocument = entry.getKey();
            var annotationDocument = entry.getValue();
            var state = annotationDocument != null ? annotationDocument.getState() : NEW;

            casses.add(new TrainingDocument(sourceDocument, dataOwner, state));
        }

        return casses;
    }

    public int size()
    {
        return get().size();
    }

    private boolean containsTargetTypeAndFeature(Recommender aRecommender, CAS aCas)
    {
        Type type;
        try {
            type = CasUtil.getType(aCas, aRecommender.getLayer().getName());
        }
        catch (IllegalArgumentException e) {
            // If the CAS does not contain the target type at all, then it cannot contain any
            // annotations of that type.
            return false;
        }

        if (type.getFeatureByBaseName(aRecommender.getFeature().getName()) == null) {
            // If the CAS does not contain the target feature, then there won't be any training
            // data.
            return false;
        }

        return !aCas.select(type).isEmpty();
    }

    private class TrainingDocument
    {
        private final SourceDocument document;
        private final String user;
        private final AnnotationDocumentState state;

        private boolean attemptedLoading = false;
        private CAS _cas;

        TrainingDocument(SourceDocument aDocument, String aAnnotator,
                AnnotationDocumentState aState)
        {
            document = aDocument;
            user = aAnnotator;
            state = aState;
        }

        private CAS getCas()
        {
            if (attemptedLoading) {
                return _cas;
            }

            attemptedLoading = true;
            try {
                // During training, we should not have to modify the CASes... right? Fingers
                // crossed.
                _cas = documentService.readAnnotationCas(document, user, AUTO_CAS_UPGRADE,
                        SHARED_READ_ONLY_ACCESS);
            }
            catch (IOException e) {
                LOG.error("Unable to load CAS to train recommender", e);
            }

            return _cas;
        }
    }
}
