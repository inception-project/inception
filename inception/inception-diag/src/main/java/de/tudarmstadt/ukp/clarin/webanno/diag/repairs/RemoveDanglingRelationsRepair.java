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

import static de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils.getNonIndexedFSes;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel.INFO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * Removes relations that were not properly cleaned up after deleting a source/target span. Such
 * relations either:
 * <ul>
 * <li>still point to the respective span even through the span is not indexed anymore</li>
 * <li>have a {@code null} source or target span</li>
 * <li>have a {@code null} source or target span after resolving the attach feature</li>
 * </ul>
 */
@Safe(false)
public class RemoveDanglingRelationsRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public RemoveDanglingRelationsRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        Set<FeatureStructure> nonIndexed = getNonIndexedFSes(aCas);

        Set<FeatureStructure> toDelete = new LinkedHashSet<>();

        for (AnnotationFS fs : aCas.getAnnotationIndex()) {
            Type t = fs.getType();

            Feature sourceFeat = t.getFeatureByBaseName(FEAT_REL_SOURCE);
            Feature targetFeat = t.getFeatureByBaseName(FEAT_REL_TARGET);

            // Is this a relation?
            if (!(sourceFeat != null && targetFeat != null)) {
                continue;
            }

            FeatureStructure source = fs.getFeatureValue(sourceFeat);
            FeatureStructure target = fs.getFeatureValue(targetFeat);

            // Are there null end-points or does it point to deleted spans?
            if (source == null || target == null || nonIndexed.contains(source)
                    || nonIndexed.contains(target)) {
                toDelete.add(fs);
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

            // Here we get the annotations that the relation is pointing to in the UI
            if (relationSourceAttachFeature != null) {
                source = (AnnotationFS) source.getFeatureValue(relationSourceAttachFeature);
            }

            if (relationTargetAttachFeature != null) {
                target = (AnnotationFS) target.getFeatureValue(relationTargetAttachFeature);
            }

            // Are there null end-points or does it point to deleted spans after resolving to
            // annotations linked to in the UI?
            if (source == null || target == null || nonIndexed.contains(source)
                    || nonIndexed.contains(target)) {
                toDelete.add(fs);
                continue;
            }
        }

        // Delete those relations that pointed to deleted spans
        if (!toDelete.isEmpty()) {
            toDelete.forEach(aCas::removeFsFromIndexes);
            aMessages.add(new LogMessage(this, INFO, "Removed [%d] dangling relations.",
                    toDelete.size()));
        }
    }
}
