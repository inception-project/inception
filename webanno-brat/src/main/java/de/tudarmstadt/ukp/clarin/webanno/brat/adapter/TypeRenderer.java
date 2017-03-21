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
package de.tudarmstadt.ukp.clarin.webanno.brat.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Type Adapters for span, arc, and chain annotations
 *
 */
public interface TypeRenderer
{
    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param features the features.
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aState
     *            Data model for brat annotations
     * @param aColoringStrategy
     *            the  coloring strategy to render this layer
     */
    void render(JCas aJcas, List<AnnotationFeature> features, GetDocumentResponse aResponse,
            AnnotatorState aState, ColoringStrategy aColoringStrategy);
    
    default void renderRequiredFeatureErrors(List<AnnotationFeature> aFeatures,
            FeatureStructure aFS, GetDocumentResponse aResponse)
    {
        for (AnnotationFeature f : aFeatures) {
            if (WebAnnoCasUtil.isRequiredFeatureMissing(f, aFS)) {
                aResponse.addComment(new Comment(getAddr(aFS), Comment.ANNOTATION_ERROR,
                        "Required feature [" + f.getName() + "] not set."));
            }
        }
    }
}
