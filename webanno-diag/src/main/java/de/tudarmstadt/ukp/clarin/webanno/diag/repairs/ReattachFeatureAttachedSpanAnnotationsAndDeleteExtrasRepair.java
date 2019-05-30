/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

@Safe(false)
public class ReattachFeatureAttachedSpanAnnotationsAndDeleteExtrasRepair
    implements Repair
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!(SPAN_TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
                continue;
            }

            Type attachType = getType(aCas, layer.getAttachType().getName());
            String attachFeature = layer.getAttachFeature().getName();

            int count = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // anno   -> e.g. Lemma
            // attach -> e.g. Token
            for (AnnotationFS anno : select(aCas, getType(aCas, layer.getName()))) {
                // Here we fetch all annotations of the layer we attach to at the relevant position,
                // e.g. Token
                List<AnnotationFS> attachables = selectCovered(attachType, anno);
                if (attachables.size() > 1) {
                    aMessages.add(LogMessage.error(this,
                            "There is more than one attachable annotation for [%s] on layer [%s].",
                            layer.getName(), attachType.getName()));
                }
                
                for (AnnotationFS attach : attachables) {
                    AnnotationFS existing = getFeature(attach, attachFeature, AnnotationFS.class);
                    
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
            // attach     -> e.g. Token
            // candidates -> e.g. Lemma
            List<AnnotationFS> toDelete = new ArrayList<>();
            for (AnnotationFS attach : select(aCas, attachType)) {
                List<AnnotationFS> candidates = selectCovered(getType(aCas, layer.getName()),
                        attach);
                
                if (!candidates.isEmpty()) {
                    // One of the candidates should already be attached
                    AnnotationFS attachedCandidate = getFeature(attach, attachFeature,
                            AnnotationFS.class);
                    
                    for (AnnotationFS candidate : candidates) {
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
