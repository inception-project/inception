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
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * Ensures that the offsets of relations match the target of the relation. This mirrors the DKPro
 * Core convention that the offsets of a dependency relation must match the offsets of the
 * dependent.
 */
@Safe(true)
public class RelationOffsetsRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public RelationOffsetsRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        List<AnnotationFS> fixedRels = new ArrayList<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            if (!WebAnnoConst.RELATION_TYPE.equals(layer.getType())) {
                continue;
            }

            Type type;
            try {
                type = getType(aCas, layer.getName());
            }
            catch (IllegalArgumentException e) {
                // If the type does not exist, the CAS has not been upgraded. In this case, we
                // can skip checking the layer because there will be no annotations anyway.
                continue;
            }

            for (AnnotationFS rel : select(aCas, type)) {
                AnnotationFS target = getFeature(rel, WebAnnoConst.FEAT_REL_TARGET,
                        AnnotationFS.class);
                if ((rel.getBegin() != target.getBegin()) || (rel.getEnd() != target.getEnd())) {
                    fixedRels.add(rel);
                    setFeature(rel, CAS.FEATURE_BASE_NAME_BEGIN, target.getBegin());
                    setFeature(rel, CAS.FEATURE_BASE_NAME_END, target.getEnd());
                }
            }

            // Delete those relations that pointed to deleted spans
            if (!fixedRels.isEmpty()) {
                aMessages.add(new LogMessage(this, LogLevel.INFO,
                        "Fixed the offsets of [%d] relations in layer [" + layer.getName() + "].",
                        fixedRels.size()));
            }
        }
    }
}
