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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair(
            AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!(SpanLayerSupport.TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
                continue;
            }

            var attachType = getType(aCas, layer.getAttachType().getName());
            var attachFeature = layer.getAttachFeature().getName();

            var count = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // anno -> e.g. Lemma
            // attach -> e.g. Token
            for (var anno : select(aCas, getType(aCas, layer.getName()))) {
                // Here we fetch all annotations of the layer we attach to at the relevant position,
                // e.g. Token
                var attachables = selectCovered(attachType, anno);
                if (attachables.size() > 1) {
                    aMessages.add(LogMessage.error(this,
                            "There is more than one attachable annotation for [%s] on layer [%s].",
                            layer.getName(), attachType.getName()));
                }

                for (var attach : attachables) {
                    var existing = getFeature(attach, attachFeature, AnnotationFS.class);

                    // So there is an annotation to which we could attach and it does not yet have
                    // an annotation attached, so we attach to it.
                    if (existing == null) {
                        setFeature(attach, attachFeature, anno);
                        count++;
                    }
                }
            }

            if (count > 0) {
                aMessages.add(LogMessage.info(this,
                        "Reattached [%d] unattached spans on layer [%s].", count, layer.getName()));
            }

            // Go over the layer that is being attached to (e.g. Lemma) and ensure that if there
            // only exactly one annotation for each annotation in the layer that has the attach
            // feature (e.g. Token) - or short: ensure that there are not multiple Lemmas for a
            // single Token because such a thing is not valid in WebAnno. Layers that have an
            // attach feature cannot have stacking enabled!
            //
            // attach -> e.g. Token
            // candidates -> e.g. Lemma
            var toDelete = new ArrayList<AnnotationFS>();
            for (var attach : select(aCas, attachType)) {
                var candidates = selectCovered(getType(aCas, layer.getName()), attach);

                if (!candidates.isEmpty()) {
                    // One of the candidates should already be attached
                    var attachedCandidate = getFeature(attach, attachFeature, AnnotationFS.class);

                    for (var candidate : candidates) {
                        if (!candidate.equals(attachedCandidate)) {
                            toDelete.add(candidate);
                        }
                    }
                }
            }

            // Delete those the extra candidates that are not properly attached
            if (!toDelete.isEmpty()) {
                toDelete.forEach(aCas::removeFsFromIndexes);
                aMessages.add(
                        LogMessage.info(this, "Removed [%d] unattached stacked candidates [%s].",
                                toDelete.size(), layer.getName()));
            }
        }
    }
}
