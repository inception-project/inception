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

import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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

    public static JCas clearJcasAnnotations(JCas aJCas)
        throws IOException
    {
        JCas target;
        try {
            target = JCasFactory.createJCas();
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }
        
        // Copy the CAS - basically we do this just to keep the full type system information
        CASCompleteSerializer serializer = serializeCASComplete(aJCas.getCasImpl());
        deserializeCASComplete(serializer, (CASImpl) target.getCas());

        // Re-init JCas
        try {
            target.getCas().getJCas();
        }
        catch (CASException e) {
            throw new IOException(e);
        }

        // Remove all annotations from the target CAS but we keep the type system!
        target.reset();
        
        // Copy over essential information
        if (JCasUtil.exists(aJCas, DocumentMetaData.class)) {
            DocumentMetaData.copy(aJCas, target);
        }
        else {
            DocumentMetaData.create(aJCas);
        }
        target.setDocumentLanguage(aJCas.getDocumentLanguage()); // DKPro Core Issue 435
        target.setDocumentText(aJCas.getDocumentText());
        
        // Transfer token boundaries
        for (Token t : select(aJCas, Token.class)) {
            new Token(target, t.getBegin(), t.getEnd()).addToIndexes();
        }

        // Transfer sentence boundaries
        for (Sentence s : select(aJCas, Sentence.class)) {
            new Sentence(target, s.getBegin(), s.getEnd()).addToIndexes();
        }

        return target;
    }
}
