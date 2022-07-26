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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class FeatureAttachedSpanAnnotationsTrulyAttachedCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public FeatureAttachedSpanAnnotationsTrulyAttachedCheck(
            AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        int count = 0;
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!(SPAN_TYPE.equals(layer.getType()) && layer.getAttachFeature() != null)) {
                continue;
            }

            Type layerType;
            Type attachType;

            try {
                layerType = getAnnotationType(aCas, layer.getName());
                attachType = getAnnotationType(aCas, layer.getAttachType().getName());
            }
            catch (IllegalArgumentException e) {
                // This happens if the types do not (yet) exist in the CAS because the types are
                // new and the CAS has not been upgraded yet. In this case, we can just ignore the
                // check
                continue;
            }

            for (AnnotationFS anno : select(aCas, layerType)) {
                for (AnnotationFS attach : selectCovered(attachType, anno)) {
                    AnnotationFS candidate = getFeature(attach, layer.getAttachFeature().getName(),
                            AnnotationFS.class);
                    if (!anno.equals(candidate)) {
                        if (count < 100) {
                            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                                    "Annotation should be attached to ["
                                            + layer.getAttachFeature().getName()
                                            + "] but is not.\nAnnotation: [" + anno
                                            + "]\nAttach annotation:[" + attach + "]"));
                        }
                        count++;
                        ok = false;
                    }
                }
            }
        }

        if (count >= 100) {
            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                    "In total [%d] annotations were not properly attached", count));
        }

        return ok;
    }
}
