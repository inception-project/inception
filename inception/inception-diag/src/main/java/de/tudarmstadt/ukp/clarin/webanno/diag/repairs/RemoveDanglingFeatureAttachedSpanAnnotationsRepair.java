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

import static de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils.getNonIndexedFSesWithOwner;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class RemoveDanglingFeatureAttachedSpanAnnotationsRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public RemoveDanglingFeatureAttachedSpanAnnotationsRepair(
            AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var nonIndexed = getNonIndexedFSesWithOwner(aCas);

        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            var count = 0;

            if (!(SpanLayerSupport.TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
                continue;
            }

            var attachType = getType(aCas, layer.getAttachType().getName());
            var attachFeature = layer.getAttachFeature().getName();

            for (var anno : select(aCas, attachType)) {
                var existing = getFeature(anno, attachFeature, AnnotationFS.class);
                if (nonIndexed.containsKey(existing)) {
                    setFeature(anno, attachFeature, (FeatureStructure) null);
                    count++;
                }
            }

            if (count > 0) {
                aMessages.add(LogMessage.info(this,
                        "Cleared [%d] unindexed attached annotations on layer [%s].", count,
                        layer.getName()));
            }
        }
    }
}
