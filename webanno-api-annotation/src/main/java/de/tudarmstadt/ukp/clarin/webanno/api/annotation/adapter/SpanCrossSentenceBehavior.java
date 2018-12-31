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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;

@Component
public class SpanCrossSentenceBehavior
    extends SpanLayerBehavior
{
    @Override
    public boolean accepts(LayerSupport<?> aLayerType)
    {
        return super.accepts(aLayerType) || aLayerType instanceof ChainLayerSupport;
    }
    
    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (aAdapter.getLayer().isCrossSentence()) {
            return aRequest;
        }
        
        if (!isSameSentence(aRequest.getJcas(), aRequest.getBegin(), aRequest.getEnd())) {
            throw new MultipleSentenceCoveredException("Annotation covers multiple sentences, "
                    + "limit your annotation to single sentence!");
        }

        return aRequest;
    }
    
    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VSpan> annoToSpanIdx)
    {
        if (aAdapter.getLayer().isCrossSentence()) {
            return;
        }
        
        for (Entry<AnnotationFS, VSpan> e : annoToSpanIdx.entrySet()) {
            if (e.getValue().getRanges().size() > 1) {
                aResponse.add(new VComment(new VID(e.getKey()), ERROR,
                        "Crossing sentence bounardies is not permitted."));
            }
        }
    }
}
