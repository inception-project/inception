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
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

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
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        Map<FeatureStructure, FeatureStructure> nonIndexed = getNonIndexedFSesWithOwner(aCas);

        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            int count = 0;

            if (!(SPAN_TYPE.equals(layer.getType()) && layer.getAttachFeature() != null)) {
                continue;
            }

            Type attachType = getType(aCas, layer.getAttachType().getName());
            String attachFeature = layer.getAttachFeature().getName();

            for (AnnotationFS anno : select(aCas, attachType)) {
                AnnotationFS existing = getFeature(anno, attachFeature, AnnotationFS.class);
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
