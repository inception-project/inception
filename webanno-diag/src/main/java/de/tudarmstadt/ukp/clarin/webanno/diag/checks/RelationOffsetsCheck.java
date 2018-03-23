/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * Checks that the offsets of relations match the target of the relation. This mirrors the DKPro
 * Core convention that the offsets of a dependency relation must match the offsets of the 
 * dependent.
 */
public class RelationOffsetsCheck
    implements Check
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!WebAnnoConst.RELATION_TYPE.equals(layer.getType())) {
                continue;
            }

            for (AnnotationFS rel : select(aCas, getType(aCas, layer.getName()))) {
                AnnotationFS target = getFeature(rel, WebAnnoConst.FEAT_REL_TARGET,
                        AnnotationFS.class);
                if ((rel.getBegin() != target.getBegin()) || (rel.getEnd() != target.getEnd())) {
                    aMessages.add(new LogMessage(this, LogLevel.ERROR,
                            "Relation offsets [%d,%d] to not match target offsets [%d,%d]",
                            rel.getBegin(), rel.getEnd(), target.getBegin(), target.getEnd()));
                    ok = false;
                }
            }
        }

        return ok;
    }
}
