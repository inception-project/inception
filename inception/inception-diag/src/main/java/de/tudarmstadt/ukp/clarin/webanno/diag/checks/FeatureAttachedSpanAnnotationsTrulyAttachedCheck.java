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

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class FeatureAttachedSpanAnnotationsTrulyAttachedCheck
    implements Check
{
    private static final int LIMIT = 100;

    private final AnnotationSchemaService annotationService;

    public FeatureAttachedSpanAnnotationsTrulyAttachedCheck(
            AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var ok = true;
        var count = 0;
        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!(SpanLayerSupport.TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
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

        if (count >= LIMIT) {
            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                    "In total [%d] annotations were not properly attached (only the first [%d] shown)",
                    count, LIMIT));
        }

        return ok;
    }
}
