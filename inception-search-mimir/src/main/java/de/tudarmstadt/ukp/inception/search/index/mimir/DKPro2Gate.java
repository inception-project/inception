/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.index.mimir;

import static de.tudarmstadt.ukp.dkpro.core.io.gate.internal.GateAnnieConstants.FEAT_LEMMA;
import static de.tudarmstadt.ukp.dkpro.core.io.gate.internal.GateAnnieConstants.FEAT_STEM;
import static gate.creole.ANNIEConstants.SENTENCE_ANNOTATION_TYPE;
import static gate.creole.ANNIEConstants.TOKEN_ANNOTATION_TYPE;
import static gate.creole.ANNIEConstants.TOKEN_CATEGORY_FEATURE_NAME;
import static gate.creole.ANNIEConstants.TOKEN_LENGTH_FEATURE_NAME;
import static gate.creole.ANNIEConstants.TOKEN_STRING_FEATURE_NAME;
import static org.apache.uima.fit.util.JCasUtil.selectAll;

import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import gate.corpora.DocumentContentImpl;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class DKPro2Gate
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Converts DKPro to Gate using default unnamed annotation set (kept for backward compatibility
     */
    public Document convert(JCas aSource, Document aTarget,
            AnnotationSchemaService aAnnotationSchemaService, Project aProject)
        throws GateException
    {
        return convert(aSource, aTarget, null, -1, -1, null, aAnnotationSchemaService, aProject);
    }

    /*
     * Converts DKPro to Gate possibly with a named annotation set
     */
    public Document convert(JCas aSource, Document aTarget, String annotationSetName,
            long aSourceDocumentId, long aAnnotationDocumentId, String aUser,
            AnnotationSchemaService aAnnotationSchemaService, Project aProject)
        throws GateException
    {
        IntOpenHashSet processed = new IntOpenHashSet();

        aTarget.setContent(new DocumentContentImpl(aSource.getDocumentText()));

        AnnotationSet as;

        if (annotationSetName == null || annotationSetName.length() == 0) {
            as = aTarget.getAnnotations();
        }
        else {
            as = aTarget.getAnnotations(annotationSetName);
        }

        for (TOP fs : selectAll(aSource)) {
            if (processed.contains(fs.getAddress())) {
                continue;
            }

            if (fs instanceof Token) {
                // Treat token
                Token t = (Token) fs;
                FeatureMap fm = new SimpleFeatureMapImpl();
                fm.put(TOKEN_LENGTH_FEATURE_NAME, t.getCoveredText().length());
                fm.put(TOKEN_STRING_FEATURE_NAME, t.getCoveredText());
                if (t.getPos() != null) {
                    fm.put(TOKEN_CATEGORY_FEATURE_NAME, t.getPos().getPosValue());
                }
                if (t.getLemma() != null) {
                    fm.put(FEAT_LEMMA, t.getLemma().getValue());
                }
                if (t.getStem() != null) {
                    fm.put(FEAT_STEM, t.getStem().getValue());
                }
                as.add(Long.valueOf(t.getBegin()), Long.valueOf(t.getEnd()), TOKEN_ANNOTATION_TYPE,
                        fm);
            }
            else if (fs instanceof Sentence) {
                // Treat sentences
                Sentence s = (Sentence) fs;
                FeatureMap fm = new SimpleFeatureMapImpl();
                as.add(Long.valueOf(s.getBegin()), Long.valueOf(s.getEnd()),
                        SENTENCE_ANNOTATION_TYPE, fm);
            }
            else if (fs instanceof DocumentMetaData) {
                // Treat metadata features
                DocumentMetaData docMetaData = (DocumentMetaData) fs;
                aTarget.setName(docMetaData.getDocumentTitle());

                // Save the document indexing features
                aTarget.getFeatures().put("SourceDocumentId", aSourceDocumentId);
                aTarget.getFeatures().put("AnnotationDocumentId", aAnnotationDocumentId);
                aTarget.getFeatures().put("User", aUser);
            }
            else if (!(fs instanceof Lemma)) {
                // Treat other features, except lemmas, that have already been treated with the
                // tokens

                Annotation annotation = (Annotation) fs;

                String annotationName = fs.getType().getName();

                if (aAnnotationSchemaService.existsLayer(annotationName, WebAnnoConst.SPAN_TYPE,
                        aProject)) {
                    // Only consider span type layers

                    AnnotationLayer annotationLayer = aAnnotationSchemaService
                            .getLayer(annotationName, aProject);

                    FeatureMap fm = new SimpleFeatureMapImpl();

                    // long begin = fs.getIntValue(fs.getCASImpl().getBeginFeature());
                    // long end = fs.getIntValue(fs.getCASImpl().getEndFeature());

                    fm.put(TOKEN_LENGTH_FEATURE_NAME, annotation.getEnd() - annotation.getBegin());
                    fm.put(TOKEN_STRING_FEATURE_NAME, annotation.getCoveredText());

                    // Get features for this annotation
                    List<AnnotationFeature> annotationFeatures = aAnnotationSchemaService
                            .listAnnotationFeature(annotationLayer);

                    for (AnnotationFeature annotationFeature : annotationFeatures) {

                        String value = "";
                        if (WebAnnoCasUtil.isPrimitiveFeature(fs, annotationFeature.getName())) {
                            value = WebAnnoCasUtil.getFeature(fs, annotationFeature.getName());
                            fm.put(annotationFeature.getName(), value);
                        }

                    }

                    as.add((long) annotation.getBegin(), (long) annotation.getEnd(),
                            fs.getType().getShortName(), fm);
                }
            }

            processed.add(fs.getAddress());
        }

        return aTarget;
    }
}
