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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel.INFO;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Checks if there are any relations that do not have a source or target. Note that relations
 * referring to non-indexed end-points are handled by {@link AllFeatureStructuresIndexedCheck}.
 */
public class DanglingRelationsCheck
    implements Check
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;

        for (AnnotationFS fs : aCas.getAnnotationIndex()) {
            Type t = fs.getType();

            Feature sourceFeat = t.getFeatureByBaseName(FEAT_REL_SOURCE);
            Feature targetFeat = t.getFeatureByBaseName(FEAT_REL_TARGET);

            // Is this a relation?
            if (!(sourceFeat != null && targetFeat != null)) {
                continue;
            }

            RelationAdapter relationAdapter = (RelationAdapter) annotationService
                    .findAdapter(aProject, fs);

            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeat.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeat.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
            }

            FeatureStructure source = fs.getFeatureValue(sourceFeat);
            FeatureStructure target = fs.getFeatureValue(targetFeat);

            // Here we get the annotations that the relation is pointing to in the UI
            if (source != null && relationSourceAttachFeature != null) {
                source = (AnnotationFS) source.getFeatureValue(relationSourceAttachFeature);
            }

            if (target != null && relationTargetAttachFeature != null) {
                target = (AnnotationFS) target.getFeatureValue(relationTargetAttachFeature);
            }

            // Does it have null endpoints?
            if (source == null || target == null) {
                StringBuilder message = new StringBuilder();

                message.append("Relation [" + relationAdapter.getLayer().getName() + "] with id ["
                        + getAddr(fs) + "] has loose ends - cannot identify attached annotations.");
                if (relationAdapter.getAttachFeatureName() != null) {
                    message.append("\nRelation [" + relationAdapter.getLayer().getName()
                            + "] attached to feature [" + relationAdapter.getAttachFeatureName()
                            + "].");
                }
                message.append("\nSource: " + source);
                message.append("\nTarget: " + target);

                aMessages.add(new LogMessage(this, INFO, "%s", message));
                ok = false;
            }
        }

        return ok;
    }
}
