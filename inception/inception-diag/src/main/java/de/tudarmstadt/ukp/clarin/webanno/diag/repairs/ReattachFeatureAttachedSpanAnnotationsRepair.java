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

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe
public class ReattachFeatureAttachedSpanAnnotationsRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public ReattachFeatureAttachedSpanAnnotationsRepair(AnnotationSchemaService aAnnotationService)
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
            var nonNullCount = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // anno -> e.g. Lemma
            // attach -> e.g. Token
            // Here we iterate over the attached layer, e.g. Lemma
            for (var anno : select(aCas, getType(aCas, layer.getName()))) {
                // Here we fetch all annotations of the layer we attach to at the relevant position,
                // e.g. Token
                for (var attach : selectCovered(attachType, anno)) {
                    var existing = getFeature(attach, attachFeature, AnnotationFS.class);

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
