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

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Finds annotations that are reachable through an attach-feature but that are not actually indexed
 * and adds them back to the index.
 */
@Safe
public class ReindexFeatureAttachedSpanAnnotationsRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public ReindexFeatureAttachedSpanAnnotationsRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        Map<FeatureStructure, FeatureStructure> nonIndexed = getNonIndexedFSesWithOwner(aCas);

        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!(SpanLayerSupport.TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
                continue;
            }

            var count = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // attach -> e.g. Token
            // anno -> e.g. Lemma
            for (var attach : select(aCas, getType(aCas, layer.getAttachType().getName()))) {
                var anno = getFeature(attach, layer.getAttachFeature().getName(),
                        AnnotationFS.class);
                if (anno != null && nonIndexed.containsKey(anno)) {
                    aCas.addFsToIndexes(anno);
                    count++;
                }
            }

            if (count > 0) {
                aMessages.add(new LogMessage(this, LogLevel.INFO,
                        "Reindexed [%d] unindexed spans in layer [" + layer.getName() + "].",
                        count));
            }
        }
    }
}
