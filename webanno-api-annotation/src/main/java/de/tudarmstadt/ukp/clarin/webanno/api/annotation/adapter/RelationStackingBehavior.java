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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType.ERROR;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

@Component
public class RelationStackingBehavior
    extends RelationLayerBehavior
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public CreateRelationAnnotationRequest onCreate(RelationAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (aAdapter.getLayer().isAllowStacking()) {
            return aRequest;
        }
        
        final AnnotationLayer layer = aAdapter.getLayer();
        final CAS jcas = aRequest.getCas();
        final Type type = getType(jcas, aAdapter.getLayer().getName());
        final Feature dependentFeature = type.getFeatureByBaseName(aAdapter.getTargetFeatureName());
        final Feature governorFeature = type.getFeatureByBaseName(aAdapter.getSourceFeatureName());
        
        // Locate the governor and dependent annotations - looking at the annotations that are
        // presently visible on screen is sufficient - we don't have to scan the whole CAS.
        for (AnnotationFS fs : selectCovered(jcas, type, aRequest.getWindowBegin(),
                aRequest.getWindowEnd())) {
            AnnotationFS existingTargetFS = (AnnotationFS) fs.getFeatureValue(dependentFeature);
            AnnotationFS existingOriginFS = (AnnotationFS) fs.getFeatureValue(governorFeature);
        
            if (existingTargetFS == null || existingOriginFS == null) {
                log.warn("Relation [" + layer.getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - ignoring during while checking for duplicates.");
                continue;
            }

            // If stacking is not allowed and we would be creating a duplicate arc, then instead
            // update the label of the existing arc
            if (isDuplicate(existingOriginFS, aRequest.getOriginFs(), existingTargetFS,
                    aRequest.getTargetFs())) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + layer.getUiName() + "] at this location - stacking is not "
                        + "enabled for this layer.");
            }
        }
        
        return aRequest;
    }
    
    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VArc> aAnnoToArcIdx)
    {
        if (aAdapter.getLayer().isCrossSentence() || aAnnoToArcIdx.isEmpty()) {
            return;
        }
        
        RelationAdapter adapter = (RelationAdapter) aAdapter;
        
        CAS cas = aAnnoToArcIdx.keySet().iterator().next().getCAS();
        Type type = getType(cas, adapter.getAnnotationTypeName());
        Feature targetFeature = type.getFeatureByBaseName(adapter.getTargetFeatureName());
        Feature sourceFeature = type.getFeatureByBaseName(adapter.getSourceFeatureName());
        
        // We go through all relations based on the their offsets. Stacked relations must have
        // the same offsets (at least if we consider relations as having a direction, i.e. 
        // that a relation A->B does not count as stacked on a relation B->A). But since there can
        // be multiple relations going out from the same sourceFS, we need to consider all of them
        // for potential stacking.
        List<AnnotationFS> candidates = new ArrayList<>();
        Set<AnnotationFS> warningRendered = new HashSet<>();
        for (Entry<AnnotationFS, VArc> e : aAnnoToArcIdx.entrySet()) {
            AnnotationFS fs = e.getKey();
            AnnotationFS sourceFs = (AnnotationFS) fs.getFeatureValue(sourceFeature);
            AnnotationFS targetFs = (AnnotationFS) fs.getFeatureValue(targetFeature);
            
            // If there are no stacking candidates at the current position yet, collect the first
            if (candidates.isEmpty()) {
                candidates.add(fs);
            }
            // If the current FS is at a different position from the current candidates, clear the
            // candidates list and add the current one as the first new candidate
            else if (
                    candidates.get(0).getBegin() != fs.getBegin() || 
                    candidates.get(0).getEnd() != fs.getEnd()
            ) {
                candidates.clear();
                warningRendered.clear();
                candidates.add(fs);
            }
            // If there are already stacking candidates, check if the current FS is stacking on 
            // any of them. If yes, generate an error message
            else {
                for (AnnotationFS candidate : candidates) {
                    AnnotationFS candidateOriginFS = (AnnotationFS) candidate
                            .getFeatureValue(sourceFeature);
                    AnnotationFS candidateTargetFS = (AnnotationFS) candidate
                            .getFeatureValue(targetFeature);

                    if (isDuplicate(candidateOriginFS, sourceFs, candidateTargetFS,
                            targetFs)) {
                        aResponse.add(new VComment(new VID(e.getKey()), ERROR,
                                "Stacking is not permitted."));
                        warningRendered.add(e.getKey());
                    }
                    
                    if (!warningRendered.contains(candidate)) {
                        aResponse.add(new VComment(new VID(candidate), ERROR,
                                "Stacking is not permitted."));
                        warningRendered.add(candidate);
                    }
                }
            }
        }
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, CAS aCas)
    {
        if (aAdapter.getLayer().isCrossSentence()) {
            return emptyList();
        }
        
        RelationAdapter adapter = (RelationAdapter) aAdapter;
        
        Type type = getType(aCas, adapter.getAnnotationTypeName());
        Feature targetFeature = type.getFeatureByBaseName(adapter.getTargetFeatureName());
        Feature sourceFeature = type.getFeatureByBaseName(adapter.getSourceFeatureName());
        
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();
        
        // We go through all relations based on the their offsets. Stacked relations must have
        // the same offsets (at least if we consider relations as having a direction, i.e. 
        // that a relation A->B does not count as stacked on a relation B->A). But since there can
        // be multiple relations going out from the same sourceFS, we need to consider all of them
        // for potential stacking.
        List<AnnotationFS> candidates = new ArrayList<>();
        for (AnnotationFS fs : select(aCas, type)) {
            AnnotationFS sourceFs = (AnnotationFS) fs.getFeatureValue(sourceFeature);
            AnnotationFS targetFs = (AnnotationFS) fs.getFeatureValue(targetFeature);
            
            // If there are no stacking candidates at the current position yet, collect the first
            if (candidates.isEmpty()) {
                candidates.add(fs);
            }
            // If the current FS is at a different position from the current candidates, clear the
            // candidates list and add the current one as the first new candidate
            else if (
                    candidates.get(0).getBegin() != fs.getBegin() || 
                    candidates.get(0).getEnd() != fs.getEnd()
            ) {
                candidates.clear();
                candidates.add(fs);
            }
            // If there are already stacking candidates, check if the current FS is stacking on 
            // any of them. If yes, generate an error message
            else {
                for (AnnotationFS candidate : candidates) {
                    AnnotationFS candidateOriginFS = (AnnotationFS) candidate
                            .getFeatureValue(sourceFeature);
                    AnnotationFS candidateTargetFS = (AnnotationFS) candidate
                            .getFeatureValue(targetFeature);

                    if (isDuplicate(candidateOriginFS, sourceFs, candidateTargetFS,
                            targetFs)) {
                        messages.add(Pair.of(LogMessage.error(this, "Stacked annotation at [%d-%d]",
                                fs.getBegin(), fs.getEnd()), fs));
                    }
                }
            }
        }

        return messages;
    }
    
    private boolean isDuplicate(AnnotationFS aAnnotationFSOldOrigin,
            AnnotationFS aAnnotationFSNewOrigin, AnnotationFS aAnnotationFSOldTarget,
            AnnotationFS aAnnotationFSNewTarget)
    {
        return isSame(aAnnotationFSOldOrigin, aAnnotationFSNewOrigin)
                && isSame(aAnnotationFSOldTarget, aAnnotationFSNewTarget);
    }
}
