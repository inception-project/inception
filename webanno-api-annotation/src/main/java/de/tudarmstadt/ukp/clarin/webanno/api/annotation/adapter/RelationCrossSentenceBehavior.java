/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType.ERROR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSameSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;

/**
 * Ensure that annotations do not cross sentence boundaries.
 */
@Component
public class RelationCrossSentenceBehavior
    extends RelationLayerBehavior
{
    @Override
    public CreateRelationAnnotationRequest onCreate(ArcAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (!aAdapter.getLayer().isCrossSentence() && !isSameSentence(aRequest.getJcas(),
                aRequest.getOriginFs().getBegin(), aRequest.getTargetFs().getEnd())) {
            throw new MultipleSentenceCoveredException("Annotation coveres multiple sentences, "
                    + "limit your annotation to single sentence!");
        }

        return aRequest;
    }
    
    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VArc> aAnnoToArcIdx)
    {
        if (aAdapter.getLayer().isCrossSentence()) {
            return;
        }
        
        try {
            for (Entry<AnnotationFS, VArc> e : aAnnoToArcIdx.entrySet()) {
                JCas jcas = e.getKey().getCAS().getJCas();
                
                if (!isSameSentence(jcas, 
                        selectByAddr(jcas, e.getValue().getSource().getId()).getBegin(),
                        selectByAddr(jcas, e.getValue().getTarget().getId()).getBegin()))
                {
                    aResponse.add(new VComment(new VID(e.getKey()), ERROR,
                            "Crossing sentence bounardies is not permitted."));
                }
            }
        }
        catch (CASException e) {
            throw new IllegalStateException("Unable to obtain JCas");
        }
    }
}
