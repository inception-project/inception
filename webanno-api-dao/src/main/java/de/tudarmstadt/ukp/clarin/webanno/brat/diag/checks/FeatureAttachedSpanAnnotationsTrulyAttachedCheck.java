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
package de.tudarmstadt.ukp.clarin.webanno.brat.diag.checks;

import static org.apache.uima.fit.util.FSUtil.*;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import java.util.List;

import javax.annotation.Resource;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.diag.CasDoctor.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.brat.diag.CasDoctor.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class FeatureAttachedSpanAnnotationsTrulyAttachedCheck
    implements Check
{
    @Resource(name = "annotationService")
    private AnnotationService annotationService;

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!(WebAnnoConst.SPAN_TYPE.equals(layer.getType()) && layer.getAttachFeature() != null)) {
                continue;
            }

            for (AnnotationFS anno : select(aCas, getType(aCas, layer.getName()))) {
                for (AnnotationFS attach : selectCovered(getType(aCas, layer.getAttachType().getName()), anno)) {
                    AnnotationFS candidate = getFeature(attach, layer.getAttachFeature().getName(), AnnotationFS.class);
                    if (candidate != anno) {
                        aMessages.add(new LogMessage(this, LogLevel.ERROR,
                                "Annotation should be attached to [" + layer.getAttachFeature().getName()
                                + "] but is not.\nAnnotation: [" + anno 
                                + "]\nAttach annotation:[" + attach + "]"));
                        ok = false;
                    }
                }
            }
        }
        
        return ok;
    }
}
