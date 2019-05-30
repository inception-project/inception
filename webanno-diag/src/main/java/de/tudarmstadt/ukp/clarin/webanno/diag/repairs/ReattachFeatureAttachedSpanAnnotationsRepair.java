/*
 * Copyright 2016
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

@Safe
public class ReattachFeatureAttachedSpanAnnotationsRepair
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
            int nonNullCount = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // anno   -> e.g. Lemma
            // attach -> e.g. Token
            // Here we iterate over the attached layer, e.g. Lemma
            for (AnnotationFS anno : select(aCas, getType(aCas, layer.getName()))) {
                // Here we fetch all annotations of the layer we attach to at the relevant position,
                // e.g. Token
                for (AnnotationFS attach : selectCovered(attachType, anno)) {
                    AnnotationFS existing = getFeature(attach, attachFeature, AnnotationFS.class);
                    
                    if (existing == null) {
                        setFeature(attach, layer.getAttachFeature().getName(), anno);
                        count++;
                    }
                    else if (!anno.equals(existing)) {
                        nonNullCount++;
                    }
                }
            }

            if (count > 0) {
                aMessages.add(LogMessage.info(this,
                        "Reattached [%d] unattached spans on layer [%s].", count, layer.getName()));
            }
            
            if (nonNullCount > 0) {
                aMessages.add(LogMessage.error(this,
                        "Could not attach [%d] annotations on layer [%s] because attach feature "
                                + "already non-null.",
                        nonNullCount, layer.getName()));
            }
        }
    }
}
