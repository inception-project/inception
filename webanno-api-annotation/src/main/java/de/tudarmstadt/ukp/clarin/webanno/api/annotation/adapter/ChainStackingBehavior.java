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

import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

@Component
public class ChainStackingBehavior
    extends SpanStackingBehavior
{
    @Override
    public boolean accepts(LayerSupport<?> aLayerType)
    {
        return aLayerType instanceof ChainLayerSupport;
    }
    
    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        final CAS aCas = aRequest.getJcas().getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();

        // If stacking is not allowed and there already is an annotation, then return the address
        // of the existing annotation.
        Type type = getType(aCas, aAdapter.getLayer().getName() + ChainAdapter.LINK);
        for (AnnotationFS fs : selectCovered(aCas, type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!aAdapter.getLayer().isAllowStacking()) {
                    throw new AnnotationException("Cannot create another annotation of layer ["
                            + aAdapter.getLayer().getUiName()
                            + "] at this location - stacking is not " + "enabled for this layer.");
                }
            }
        }

        return aRequest;
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, JCas aJCas)
    {
        if (aAdapter.getLayer().isAllowStacking()) {
            return emptyList();
        }

        CAS cas = aJCas.getCas();
        Type type = getType(cas, aAdapter.getLayer().getName() + ChainAdapter.LINK);
        AnnotationFS prevFS = null;
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        
        // Since the annotations are sorted, we can easily find stacked annotation by scanning
        // through the entire list and checking if two adjacent annotations have the same offsets
        for (AnnotationFS fs : select(cas, type)) {
            if (prevFS != null && prevFS.getBegin() == fs.getBegin()
                    && prevFS.getEnd() == fs.getEnd()) {
                messages.add(Pair.of(LogMessage.error(this, "Stacked annotation at [%d-%d]",
                        fs.getBegin(), fs.getEnd()), fs));
            }
            
            prevFS = fs;
        }

        return messages;
    }
}
