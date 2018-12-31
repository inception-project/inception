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
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.AnnotationComparator;

@Component
public class SpanStackingBehavior
    extends SpanLayerBehavior
{
    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (aAdapter.getLayer().isAllowStacking()) {
            return aRequest;
        }

        final CAS aCas = aRequest.getJcas().getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();

        // If stacking is not allowed and there already is an annotation, then return the address
        // of the existing annotation.
        Type type = getType(aCas, aAdapter.getAnnotationTypeName());
        for (AnnotationFS fs : selectCovered(aCas, type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + aAdapter.getLayer().getUiName()
                        + "] at this location - stacking is not " + "enabled for this layer.");
            }
        }

        return aRequest;
    }
    
    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VSpan> annoToSpanIdx)
    {
        if (aAdapter.getLayer().isAllowStacking()) {
            return;
        }
        
        // The following code requires annotations with the same offsets to be adjacent during 
        // iteration, so we sort the entries here
        AnnotationComparator cmp = new AnnotationComparator();
        List<Entry<AnnotationFS, VSpan>> sortedEntries = annoToSpanIdx.entrySet().stream()
                .sorted((e1, e2) -> cmp.compare(e1.getKey(), e2.getKey()))
                .collect(Collectors.toList());

        // Render error if annotations are stacked but stacking is not allowed
        AnnotationFS prevFS = null;
        boolean prevFSErrorGenerated = false;
        for (Entry<AnnotationFS, VSpan> e : sortedEntries) {
            AnnotationFS fs = e.getKey();
            if (prevFS != null && prevFS.getBegin() == fs.getBegin()
                    && prevFS.getEnd() == fs.getEnd()) {
                // If the current annotation is stacked with the previous one, generate an error
                aResponse.add(new VComment(new VID(fs), ERROR, "Stacking is not permitted."));
                
                // If we did not already generate an error for the previous one, also generate an
                // error for that one. This ensures that all stacked annotations get the error
                // marker, not only the 2nd, 3rd, and so on.
                if (!prevFSErrorGenerated) {
                    aResponse.add(new VComment(new VID(prevFS), ERROR, "Stacking is not permitted."));
                }
            }
            else {
                prevFSErrorGenerated = false;
            }

            prevFS = fs;
        }
    }
}
