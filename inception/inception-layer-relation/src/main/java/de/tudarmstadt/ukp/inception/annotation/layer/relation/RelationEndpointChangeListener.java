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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanMovedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class RelationEndpointChangeListener
{
    private final AnnotationSchemaService schemaService;

    public RelationEndpointChangeListener(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @EventListener
    public void onSpanMovedEvent(SpanMovedEvent aEvent)
    {
        var span = aEvent.getAnnotation();
        var cas = span.getCAS();

        for (var relLayer : schemaService.listAttachedRelationLayers(aEvent.getLayer())) {
            var relAdapter = (RelationAdapter) schemaService.getAdapter(relLayer);
            var maybeRelType = relAdapter.getAnnotationType(cas);
            if (!maybeRelType.isPresent()) {
                continue;
            }

            var relCandidates = cas.<Annotation> select(maybeRelType.get()) //
                    .at(aEvent.getOldBegin(), aEvent.getOldEnd()) //
                    .asList();

            for (var relCandidate : relCandidates) {
                if (!span.equals(relAdapter.getTargetAnnotation(relCandidate))) {
                    continue;
                }

                relCandidate.removeFromIndexes();
                relCandidate.setBegin(span.getBegin());
                relCandidate.setEnd(span.getEnd());
                relCandidate.addToIndexes();
            }
        }
    }
}
