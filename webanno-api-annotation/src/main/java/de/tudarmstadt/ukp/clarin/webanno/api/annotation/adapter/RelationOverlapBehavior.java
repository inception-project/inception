/*
 * Copyright 2019
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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Handles the {@link OverlapMode} setting for {@link WebAnnoConst#RELATION_TYPE relation layers}.
 */
@Component
public class RelationOverlapBehavior
    extends RelationLayerBehavior
{
    @Override
    public CreateRelationAnnotationRequest onCreate(RelationAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        final AnnotationLayer layer = aAdapter.getLayer();
        final CAS cas = aRequest.getCas();
        final Type type = getType(cas, layer.getName());
        final Feature targetFeature = type.getFeatureByBaseName(aAdapter.getTargetFeatureName());
        final Feature sourceFeature = type.getFeatureByBaseName(aAdapter.getSourceFeatureName());
        
        switch (layer.getOverlapMode()) {
        case ANY_OVERLAP:
            return aRequest;
        case NO_OVERLAP: {
            boolean hasAnyOverlapping = select(cas, type).stream()
                // Check if any of the end-points of the requested relation are already used as
                // end-points in another relation
                .filter(rel -> overlapping(aRequest, rel, sourceFeature, targetFeature))
                .findAny().isPresent();
            
            if (hasAnyOverlapping) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + layer.getUiName()
                        + "] at this location - no overlap or stacking is allowed for this layer.");
            }
            break;
        }
        case OVERLAP_ONLY: {
            boolean hasStacking = select(cas, type).stream()
                // Check if the requested relation has the same end-points as an existing
                // relation
                .filter(rel -> stacking(aRequest, rel, sourceFeature, targetFeature))
                .findAny().isPresent();
            
            if (hasStacking) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + layer.getUiName()
                        + "] at this location - stacking is not allowed for this layer.");
            }
            break;
        }
        case STACKING_ONLY: {
            boolean hasOverlapping = select(cas, type).stream()
                .filter(rel -> 
                    overlapping(aRequest, rel, sourceFeature, targetFeature) &&
                    !stacking(aRequest, rel, sourceFeature, targetFeature))
                .findAny().isPresent();
            
            if (hasOverlapping) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + layer.getUiName()
                        + "] at this location - only stacking is allowed for this layer.");
            }
            break;
        }
        }

        return aRequest;
    }
    
    @Override
    public void onRender(TypeAdapter aAdapter, VDocument aResponse,
            Map<AnnotationFS, VArc> aAnnoToArcIdx)
    {
        if (aAnnoToArcIdx.isEmpty()) {
            return;
        }
        
        final AnnotationLayer layer = aAdapter.getLayer();
        final RelationAdapter adapter = (RelationAdapter) aAdapter;
        final CAS cas = aAnnoToArcIdx.keySet().iterator().next().getCAS();
        final Type type = getType(cas, adapter.getAnnotationTypeName());
        final Feature targetFeature = type.getFeatureByBaseName(adapter.getTargetFeatureName());
        final Feature sourceFeature = type.getFeatureByBaseName(adapter.getSourceFeatureName());

        switch (layer.getOverlapMode()) {
        case ANY_OVERLAP:
            return;
        case NO_OVERLAP: {
            throw new NotImplementedException("");
        }
        case STACKING_ONLY: {
            throw new NotImplementedException("");
        }
        case OVERLAP_ONLY: {
            // We go through all relations based on the their offsets. Stacked relations must have
            // the same offsets (at least if we consider relations as having a direction, i.e. 
            // that a relation A->B does not count as stacked on a relation B->A). But since there 
            // can be multiple relations going out from the same sourceFS, we need to consider all 
            // of them for potential stacking.
            List<AnnotationFS> candidates = new ArrayList<>();
            Set<AnnotationFS> warningRendered = new HashSet<>();
            for (Entry<AnnotationFS, VArc> e : aAnnoToArcIdx.entrySet()) {
                AnnotationFS fs = e.getKey();
                AnnotationFS sourceFs = (AnnotationFS) fs.getFeatureValue(sourceFeature);
                AnnotationFS targetFs = (AnnotationFS) fs.getFeatureValue(targetFeature);
                
                // If there are no stacking candidates at the current position yet, collect the 
                // first
                if (candidates.isEmpty()) {
                    candidates.add(fs);
                }
                // If the current FS is at a different position from the current candidates, clear 
                // the candidates list and add the current one as the first new candidate
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

                        if (stacking(candidateOriginFS, candidateTargetFS, sourceFs, targetFs)) {
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
            break;
        }
        }        
    }
    
    @Override
    public List<Pair<LogMessage, AnnotationFS>> onValidate(TypeAdapter aAdapter, CAS aCas)
    {
        final AnnotationLayer layer = aAdapter.getLayer();
        final RelationAdapter adapter = (RelationAdapter) aAdapter;
        final Type type = getType(aCas, adapter.getAnnotationTypeName());
        final Feature sourceFeature = type.getFeatureByBaseName(adapter.getSourceFeatureName());
        final Feature targetFeature = type.getFeatureByBaseName(adapter.getTargetFeatureName());
        
        List<Pair<LogMessage, AnnotationFS>> messages = new ArrayList<>();

        switch (layer.getOverlapMode()) {
        case ANY_OVERLAP:
            return emptyList();
        case NO_OVERLAP: {
            Set<AnnotationFS> overlapping = new HashSet<>();
            for (AnnotationFS rel1 : select(aCas, type)) {
                for (AnnotationFS rel2 : select(aCas, type)) {
                    if (stacking(rel1, rel2, sourceFeature, targetFeature)) {
                        overlapping.add(rel1);
                        overlapping.add(rel2);
                    }
                }
            }
            
            for (AnnotationFS fs : overlapping) {
                messages.add(Pair.of(LogMessage.error(this, "Overlapping relation at [%d-%d]",
                        fs.getBegin(), fs.getEnd()), fs));
            }
            break;
        }
        case STACKING_ONLY: {
            Set<AnnotationFS> overlapping = new HashSet<>();
            for (AnnotationFS rel1 : select(aCas, type)) {
                for (AnnotationFS rel2 : select(aCas, type)) {
                    if (
                            overlapping(rel1, rel2, sourceFeature, targetFeature) && 
                            !stacking(rel1, rel2, sourceFeature, targetFeature)
                    ) {
                        overlapping.add(rel1);
                        overlapping.add(rel2);
                    }
                }
            }
            
            for (AnnotationFS fs : overlapping) {
                messages.add(Pair.of(LogMessage.error(this, "Overlapping relation at [%d-%d]",
                        fs.getBegin(), fs.getEnd()), fs));
            }
            break;
        }
        case OVERLAP_ONLY: {
            // We go through all relations based on the their offsets. Stacked relations must have
            // the same offsets (at least if we consider relations as having a direction, i.e. 
            // that a relation A->B does not count as stacked on a relation B->A). But since there 
            // can be multiple relations going out from the same sourceFS, we need to consider all 
            // of them for potential stacking.
            List<AnnotationFS> candidates = new ArrayList<>();
            for (AnnotationFS fs : select(aCas, type)) {
                AnnotationFS sourceFs = (AnnotationFS) fs.getFeatureValue(sourceFeature);
                AnnotationFS targetFs = (AnnotationFS) fs.getFeatureValue(targetFeature);
                
                // If there are no stacking candidates at the current position yet, collect the
                // first
                if (candidates.isEmpty()) {
                    candidates.add(fs);
                }
                // If the current FS is at a different position from the current candidates, clear 
                // the candidates list and add the current one as the first new candidate
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
                    for (AnnotationFS cand : candidates) {
                        FeatureStructure candidateOriginFS = cand.getFeatureValue(sourceFeature);
                        FeatureStructure candidateTargetFS = cand.getFeatureValue(targetFeature);

                        if (stacking(candidateOriginFS, candidateTargetFS, sourceFs, targetFs)) {
                            messages.add(Pair.of(LogMessage.error(this, "Stacked annotation at [%d-%d]",
                                    fs.getBegin(), fs.getEnd()), fs));
                        }
                    }
                }
            }
        }
        }

        return messages;
    }
    
    public static boolean stacking(FeatureStructure aRel1Src, FeatureStructure aRel1Tgt,
            FeatureStructure aRel2Src, FeatureStructure aRel2Tgt)
    {
        return isSame(aRel1Src, aRel2Src) && isSame(aRel1Tgt, aRel2Tgt);
    }

    public static boolean overlapping(AnnotationFS aRel1,
            AnnotationFS aRel2, Feature aSrcFeat, Feature aTgtFeat)
    {
        return overlapping(aRel1.getFeatureValue(aSrcFeat), aRel1.getFeatureValue(aTgtFeat),
                aRel2.getFeatureValue(aSrcFeat), aRel2.getFeatureValue(aTgtFeat));
    }

    public static boolean overlapping(CreateRelationAnnotationRequest aRequest,
            AnnotationFS aRelation, Feature aSrcFeat, Feature aTgtFeat)
    {
        return overlapping(aRequest.getOriginFs(), aRequest.getTargetFs(),
                aRelation.getFeatureValue(aSrcFeat), aRelation.getFeatureValue(aTgtFeat));
    }

    public static boolean overlapping(FeatureStructure aRel1Src, FeatureStructure aRel1Tgt,
            FeatureStructure aRel2Src, FeatureStructure aRel2Tgt)
    {
        return isSame(aRel1Src, aRel2Src) || isSame(aRel1Src, aRel2Tgt)
                || isSame(aRel1Tgt, aRel2Src) || isSame(aRel1Tgt, aRel2Tgt);
    }

    public static boolean stacking(CreateRelationAnnotationRequest aRequest,
            AnnotationFS aRelation, Feature aSourceFeature, Feature aTargetFeature)
    {
        return isSame(aRequest.getOriginFs(), aRelation.getFeatureValue(aSourceFeature)) &&
                isSame(aRequest.getTargetFs(), aRelation.getFeatureValue(aTargetFeature));
    }

    public static boolean stacking(AnnotationFS aRel1, AnnotationFS aRel2, Feature aSrcFeat,
            Feature aTgtFeat)
    {
        return stacking(aRel1.getFeatureValue(aSrcFeat), aRel1.getFeatureValue(aTgtFeat),
                aRel2.getFeatureValue(aSrcFeat), aRel2.getFeatureValue(aTgtFeat));
    }
}
