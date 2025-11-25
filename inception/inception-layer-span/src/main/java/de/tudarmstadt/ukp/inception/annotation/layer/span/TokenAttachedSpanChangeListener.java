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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanMovedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class TokenAttachedSpanChangeListener
{
    private final AnnotationSchemaService schemaService;

    public TokenAttachedSpanChangeListener(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @EventListener
    public void onSpanMovedEvent(SpanMovedEvent aEvent)
    {
        if (aEvent.getAnnotation() instanceof Token) {
            adjustAttachedAnnotations(aEvent);
            adjustSingleTokenAnchoredAnnotations(aEvent);
            adjustTokenAnchoredAnnotations(aEvent);
        }
    }

    void adjustTokenAnchoredAnnotations(SpanMovedEvent aEvent)
    {
        var unit = aEvent.getAnnotation();
        var cas = unit.getCAS();

        var multiTokenLayers = schemaService.listAnnotationLayer(aEvent.getProject()).stream() //
                .filter(layer -> layer.getAnchoringMode() == TOKENS) //
                .toList();

        for (var layer : multiTokenLayers) {
            var adapter = schemaService.getAdapter(layer);
            var maybeType = adapter.getAnnotationType(cas);

            if (maybeType.isEmpty()) {
                continue;
            }

            for (var ann : cas.<Annotation> select(maybeType.get()).asList()) {
                if (ann.getBegin() != aEvent.getOldBegin() && ann.getEnd() != aEvent.getOldEnd()) {
                    continue;
                }

                ann.removeFromIndexes();
                if (ann.getBegin() == aEvent.getOldBegin()) {
                    ann.setBegin(unit.getBegin());
                }

                if (ann.getEnd() == aEvent.getOldEnd()) {
                    ann.setEnd(unit.getEnd());
                }
                ann.addToIndexes();
            }
        }
    }

    void adjustSingleTokenAnchoredAnnotations(SpanMovedEvent aEvent)
    {
        var unit = aEvent.getAnnotation();
        var cas = unit.getCAS();

        var singleTokenLayers = schemaService.listAnnotationLayer(aEvent.getProject()).stream() //
                .filter(layer -> layer.getAnchoringMode() == SINGLE_TOKEN) //
                .toList();

        for (var layer : singleTokenLayers) {
            var adapter = schemaService.getAdapter(layer);
            if (adapter == null) {
                continue;
            }

            var maybeType = adapter.getAnnotationType(cas);

            if (maybeType.isEmpty()) {
                continue;
            }

            for (var ann : cas.<Annotation> select(maybeType.get())
                    .at(aEvent.getOldBegin(), aEvent.getOldEnd()).asList()) {
                moveAnnotation(unit, ann);
            }
        }
    }

    void adjustAttachedAnnotations(SpanMovedEvent aEvent)
    {
        var unit = aEvent.getAnnotation();
        for (var attachFeature : schemaService.listAttachedSpanFeatures(aEvent.getLayer())) {
            var attachedAnnotation = getFeature(unit, attachFeature.getName(), Annotation.class);

            if (attachedAnnotation == null) {
                continue;
            }

            moveAnnotation(unit, attachedAnnotation);
        }
    }

    private void moveAnnotation(AnnotationFS aUnit, Annotation aAnn)
    {
        aAnn.removeFromIndexes();
        aAnn.setBegin(aUnit.getBegin());
        aAnn.setEnd(aUnit.getEnd());
        aAnn.addToIndexes();
    }
}
