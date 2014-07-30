/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A helper class for {@link BratAnnotator} and CurationEditor
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAnnotatorUtility
{

    public static boolean isDocumentFinished(RepositoryService aRepository,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        // if annotationDocument is finished, disable editing
        boolean finished = false;
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
                if (aBratAnnotatorModel.getDocument().getState()
                        .equals(SourceDocumentState.CURATION_FINISHED)) {
                    finished = true;
                }
            }
            else if (aRepository
                    .getAnnotationDocument(aBratAnnotatorModel.getDocument(),
                            aBratAnnotatorModel.getUser()).getState()
                    .equals(AnnotationDocumentState.FINISHED)) {
                finished = true;
            }
        }
        catch (Exception e) {
            finished = false;
        }

        return finished;
    }

    public static JCas clearJcasAnnotations(JCas aJCas, SourceDocument aSourceDocument, User aUser,
            RepositoryService repository)
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
        try {
            DocumentMetaData.copy(aJCas, target);
            target.setDocumentLanguage(aJCas.getDocumentLanguage()); // DKPro Core Issue 435
            target.setDocumentText(aJCas.getDocumentText());
        }
        catch (AnalysisEngineProcessException e) {
            throw new IOException(e);
        }
        
        // Transfer token boundaries
        for (Token t : select(aJCas, Token.class)) {
            new Token(target, t.getBegin(), t.getEnd()).addToIndexes();
        }

        // Transfer sentence boundaries
        for (Sentence s : select(aJCas, Sentence.class)) {
            new Sentence(target, s.getBegin(), s.getEnd()).addToIndexes();
        }

        
        repository.createAnnotationDocumentContent(target, aSourceDocument, aUser);
        return target;
    }

    public static void clearJcas(JCas aJCas, SourceDocument aSourceDocument, User aUser,
            RepositoryService repository)
        throws IOException
    {
        List<Annotation> annotationsToRemove = new ArrayList<Annotation>();
        for (Annotation a : select(aJCas, Annotation.class)) {
            if (!(a instanceof Token || a instanceof Sentence || a instanceof DocumentMetaData)) {
                annotationsToRemove.add(a);
            }
        }
        for (Annotation annotation : annotationsToRemove) {
            aJCas.removeFsFromIndexes(annotation);
        }
        repository.createAnnotationDocumentContent(aJCas, aSourceDocument, aUser);
    }

    public static void clearAnnotations(JCas aJCas, Type aType)
        throws IOException
    {
        List<AnnotationFS> annotationsToRemove = new ArrayList<AnnotationFS>();
        for (AnnotationFS a : select(aJCas.getCas(), aType)) {
            annotationsToRemove.add(a);

        }
        for (AnnotationFS annotation : annotationsToRemove) {
            aJCas.removeFsFromIndexes(annotation);
        }
    }
}
