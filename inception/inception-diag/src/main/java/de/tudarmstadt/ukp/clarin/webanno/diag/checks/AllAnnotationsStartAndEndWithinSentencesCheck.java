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

import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.springframework.util.CollectionUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class AllAnnotationsStartAndEndWithinSentencesCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public AllAnnotationsStartAndEndWithinSentencesCheck(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (annotationService == null) {
            return true;
        }

        var allAnnoLayers = annotationService.listAnnotationLayer(aDocument.getProject());
        allAnnoLayers.removeIf(layer -> Sentence._TypeName.equals(layer.getName()));
        if (CollectionUtils.isEmpty(allAnnoLayers)) {
            return true;
        }

        boolean ok = true;
        for (var layer : allAnnoLayers) {
            Type type;
            try {
                type = getType(aCas, layer.getName());
            }
            catch (IllegalArgumentException e) {
                // If the type does not exist, the CAS has not been upgraded. In this case, we
                // can skip checking the layer because there will be no annotations anyway.
                continue;
            }

            if (!aCas.getTypeSystem().subsumes(aCas.getAnnotationType(), type)) {
                // Skip non-annotation types
                continue;
            }

            for (var ann : select(aCas, type)) {
                var startsOutside = aCas.select(Sentence._TypeName)
                        .covering(ann.getBegin(), ann.getBegin()).isEmpty();
                var endsOutside = aCas.select(Sentence._TypeName)
                        .covering(ann.getEnd(), ann.getEnd()).isEmpty();

                if (!startsOutside && !endsOutside) {
                    continue;
                }

                var outsides = new ArrayList<>();
                if (startsOutside) {
                    outsides.add("starts");
                }
                if (endsOutside) {
                    outsides.add("ends");
                }

                aMessages.add(LogMessage.error(this, "[%s] [%s]@[%d-%d] %s outside any sentence",
                        ann.getType().getName(),
                        escapeJava(abbreviateMiddle(ann.getCoveredText(), "…", 20)), ann.getBegin(),
                        ann.getEnd(), String.join(" and ", outsides.toArray(String[]::new))));

                ok = false;
            }
        }

        return ok;
    }
}
