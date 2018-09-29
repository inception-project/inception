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

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Safe
public class ReattachFeatureAttachedSpanAnnotationsRepair
    implements Repair
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!(WebAnnoConst.SPAN_TYPE.equals(layer.getType())
                    && layer.getAttachFeature() != null)) {
                continue;
            }

            int count = 0;

            // Go over the layer that has an attach feature (e.g. Token) and make sure that it is
            // filled
            // anno -> e.g. Lemma
            // attach -> e.g. Token
            for (AnnotationFS anno : select(aCas, getType(aCas, layer.getName()))) {
                for (AnnotationFS attach : selectCovered(
                        getType(aCas, layer.getAttachType().getName()), anno)) {
                    AnnotationFS candidate = getFeature(attach, layer.getAttachFeature().getName(),
                            AnnotationFS.class);
                    if (candidate == null) {
                        setFeature(attach, layer.getAttachFeature().getName(), anno);
                        count++;
                    }
                    else if (candidate != anno) {
                        aMessages.add(new LogMessage(this, LogLevel.ERROR,
                                "Cannot attach annotation because attach feature alread non-null"));
                    }
                }
            }

            if (count > 0) {
                aMessages.add(new LogMessage(this, LogLevel.INFO,
                        "Reattached [%d] unattached spans on layer [" + layer.getName() + "].",
                        count));
            }
        }
    }
}
