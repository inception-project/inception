/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

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

    public static CAS clearJcasAnnotations(CAS aCas)
        throws IOException
    {
        CAS target;
        try {
            target = JCasFactory.createJCas().getCas();
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }
        
        // Copy the CAS - basically we do this just to keep the full type system information
        CASCompleteSerializer serializer = serializeCASComplete((CASImpl) aCas);
        deserializeCASComplete(serializer, (CASImpl) target);

        // Remove all annotations from the target CAS but we keep the type system!
        target.reset();
        
        // Copy over essential information
        if (exists(aCas, getType(aCas, DocumentMetaData.class))) {
            copyDocumentMetadata(aCas, target);
        }
        else {
            WebAnnoCasUtil.createDocumentMetadata(aCas);
        }
        target.setDocumentLanguage(aCas.getDocumentLanguage()); // DKPro Core Issue 435
        target.setDocumentText(aCas.getDocumentText());
        
        // Transfer token boundaries
        for (AnnotationFS t : selectTokens(aCas)) {
            aCas.addFsToIndexes(createToken(target, t.getBegin(), t.getEnd()));
        }

        // Transfer sentence boundaries
        for (AnnotationFS s : selectSentences(aCas)) {
            aCas.addFsToIndexes(createSentence(target, s.getBegin(), s.getEnd()));
        }

        return target;
    }
}
