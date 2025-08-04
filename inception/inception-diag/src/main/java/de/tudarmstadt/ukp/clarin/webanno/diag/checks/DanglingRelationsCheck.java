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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.logging.LogLevel.INFO;

import java.util.HashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * Checks if there are any relations that do not have a source or target. Note that relations
 * referring to non-indexed end-points are handled by {@link AllFeatureStructuresIndexedCheck}.
 */
public class DanglingRelationsCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public DanglingRelationsCheck(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var ok = true;

        var adapterCache = new HashMap<Type, RelationAdapter>();

        for (var fs : aCas.getAnnotationIndex()) {
            var t = fs.getType();

            var sourceFeat = t.getFeatureByBaseName(FEAT_REL_SOURCE);
            var targetFeat = t.getFeatureByBaseName(FEAT_REL_TARGET);

            // Is this a relation?
            if (!(sourceFeat != null && targetFeat != null)) {
                continue;
            }

            var relationAdapter = adapterCache.computeIfAbsent(t,
                    _t -> (RelationAdapter) annotationService.findAdapter(aDocument.getProject(),
                            fs));

            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeat.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeat.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
            }

            var source = fs.getFeatureValue(sourceFeat);
            var target = fs.getFeatureValue(targetFeat);

            // Here we get the annotations that the relation is pointing to in the UI
            if (source != null && relationSourceAttachFeature != null) {
                source = (AnnotationFS) source.getFeatureValue(relationSourceAttachFeature);
            }

            if (target != null && relationTargetAttachFeature != null) {
                target = (AnnotationFS) target.getFeatureValue(relationTargetAttachFeature);
            }

            // Does it have null end-points?
            if (source == null || target == null) {
                var message = new StringBuilder();

                message.append("Relation [" + relationAdapter.getLayer().getName() + "] with id ["
                        + ICasUtil.getAddr(fs) + "] has loose ends.");
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
