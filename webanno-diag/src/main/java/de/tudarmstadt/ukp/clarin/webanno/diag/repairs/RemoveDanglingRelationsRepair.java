/*
 * Copyright 2015
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

import static de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils.getNonIndexedFSes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Removes relations that were not properly cleaned up after deleting a source/target span. Such
 * relations still point to the respective span even through the span is not indexed anymore.
 */
@Safe(false)
public class RemoveDanglingRelationsRepair
    implements Repair
{
    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        Set<FeatureStructure> nonIndexed = getNonIndexedFSes(aCas);
        
        Set<FeatureStructure> toDelete = new LinkedHashSet<>();
        
        for (AnnotationFS fs : aCas.getAnnotationIndex()) {
            Type t = fs.getType();
            
            Feature sourceFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
            Feature targetFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);
            
            // Is this a relation?
            if (!(sourceFeat != null && targetFeat != null)) {
                continue;
            }
            
            FeatureStructure source = fs.getFeatureValue(sourceFeat);
            FeatureStructure target = fs.getFeatureValue(targetFeat);
            
            // Does it point to deleted spans?
            if (nonIndexed.contains(source) || nonIndexed.contains(target)) {
                toDelete.add(fs);
            }
        }

        // Delete those relations that pointed to deleted spans
        if (!toDelete.isEmpty()) {
            toDelete.forEach(aCas::removeFsFromIndexes);
            aMessages.add(new LogMessage(this, LogLevel.INFO, "Removed [%d] dangling relations.",
                    nonIndexed.size()));
        }
    }
}
