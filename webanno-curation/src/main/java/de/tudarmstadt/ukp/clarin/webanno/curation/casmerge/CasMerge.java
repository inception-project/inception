/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation.RelationPosition;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Do a merge CAS out of multiple user annotations
 */
public class CasMerge
{
    private static final Logger LOG = LoggerFactory.getLogger(CasMerge.class);
    
    private final AnnotationSchemaService schemaService;
    private final ApplicationEventPublisher eventPublisher;
    
    private boolean mergeIncompleteAnnotations = false;
    private boolean silenceEvents = false;
    private Map<AnnotationLayer, List<AnnotationFeature>> featureCache = new HashMap<>();
    private LoadingCache<AnnotationLayer, TypeAdapter> adapterCache;
    
    public CasMerge(AnnotationSchemaService aSchemaService)
    {
        this(aSchemaService, null);
    }
    
    public CasMerge(AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aEventPublisher)
    {
        schemaService = aSchemaService;
        eventPublisher = aEventPublisher;
        
        adapterCache = Caffeine.newBuilder()
                .maximumSize(100)
                .build(schemaService::getAdapter);
    }
    
    public void setSilenceEvents(boolean aSilenceEvents)
    {
        silenceEvents = aSilenceEvents;
    }
    
    public boolean isSilenceEvents()
    {
        return silenceEvents;
    }
    
    public void setMergeIncompleteAnnotations(boolean aMergeIncompleteAnnotations)
    {
        mergeIncompleteAnnotations = aMergeIncompleteAnnotations;
    }
    
    public boolean isMergeIncompleteAnnotations()
    {
        return mergeIncompleteAnnotations;
    }

    private boolean shouldMerge(DiffResult aDiff, ConfigurationSet cfgs)
    {
        boolean stacked = cfgs.getConfigurations().stream()
                .filter(Configuration::isStacked)
                .findAny()
                .isPresent();
        if (stacked) {
            LOG.trace(" `-> Not merging stacked annotation");
            return false;
        }
        
        if (!aDiff.isComplete(cfgs) && !isMergeIncompleteAnnotations()) {
            LOG.trace(" `-> Not merging incomplete annotation");
            return false;
        }
        
        if (!aDiff.isAgreement(cfgs)) {
            LOG.trace(" `-> Not merging annotation with disagreement");
            return false;
        }
        
        return true;
    }
    
    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAs is stored under a name {@code CurationPanel#CURATION_USER}.
     * <p>
     * Any similar annotations stacked in a {@code CasDiff2.Position} will be assumed a difference
     * <p>
     * Any two annotation with different value will be assumed a difference
     * <p>
     * Any non stacked empty/null annotations are assumed agreement
     * <p>
     * Any non stacked annotations with similar values for each of the features are assumed
     * agreement
     * <p>
     * Any two link mode / slotable annotations which agree on the base features are assumed
     * agreement
     *
     * @param aDiff
     *            the {@link DiffResult}
     * @param aCases
     *            a map of {@code CAS}s for each users and the random merge
     */
    public void reMergeCas(DiffResult aDiff, SourceDocument aTargetDocument, String aTargetUsername,
            CAS aTargetCas, Map<String, CAS> aCases)
        throws AnnotationException, UIMAException
    {
        silenceEvents = true;
        
        List<LogMessage> messages = new ArrayList<>();
        
        // Remove any annotations from the target CAS - keep type system, sentences and tokens
        clearAnnotations(aTargetCas);
        
        // If there is nothing to merge, bail out
        if (aCases.isEmpty()) {
            return;
        }
                
        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        Map<String, AnnotationLayer> type2layer = aDiff.getPositions().stream()
                .map(Position::getType)
                .distinct()
                .map(type -> schemaService.findLayer(aTargetDocument.getProject(), type))
                .collect(Collectors.toMap(AnnotationLayer::getName, Function.identity()));

        List<String> layerNames = new ArrayList<>(type2layer.keySet());
        
        // Move token layer to front
        if (layerNames.contains(Token.class.getName())) {
            layerNames.remove(Token.class.getName());
            layerNames.add(0, Token.class.getName());
        }
        
        // Move sentence layer to front
        if (layerNames.contains(Sentence.class.getName())) {
            layerNames.remove(Sentence.class.getName());
            layerNames.add(0, Sentence.class.getName());
        }
        
        // First we process the SPAN layers since other layers can refer to them (via slot features
        // or as relation layers).
        // We process layer by layer so that we can order the layers (important to process tokens
        // and sentences before the others)
        for (String layerName : layerNames) {
            List<SpanPosition> positions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition)
                    .map(pos -> (SpanPosition) pos)
                    // We don't process slot features here (they are span sub-positions)
                    .filter(pos -> pos.getFeature() == null)
                    .collect(Collectors.toList());
            
            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} span positions on layer {}", positions.size(), layerName);

            // First we merge the spans so that we can attach the relations to something later.
            // Slots are also excluded for the moment
            for (SpanPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
                
                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }
                
                try {
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative();
                    mergeSpanAnnotation(aTargetDocument, aTargetUsername,
                            type2layer.get(position.getType()), aTargetCas, sourceFS, false);
                    LOG.trace(" `-> merged annotation with agreement");
                }
                catch (AnnotationException e) {
                    LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                    messages.add(LogMessage.error(this, "%s", e.getMessage()));
                }
            }
        }
        
        // After the spans are in place, we can merge the slot features
        for (String layerName : layerNames) {
            List<SpanPosition> positions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition)
                    .map(pos -> (SpanPosition) pos)
                    // We only process slot features here
                    .filter(pos -> pos.getFeature() != null)
                    .collect(Collectors.toList());
            
            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} slot positions on layer [{}]", positions.size(), layerName);
            
            for (SpanPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
                
                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }
                
                try {
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative();
                    AID sourceFsAid = cfgs.getConfigurations().get(0)
                            .getRepresentativeAID();
                    mergeSlotFeature(aTargetDocument, aTargetUsername,
                            type2layer.get(position.getType()), aTargetCas, sourceFS,
                            sourceFsAid.feature, sourceFsAid.index);
                    LOG.trace(" `-> merged annotation with agreement");
                }
                catch (AnnotationException e) {
                    LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                    messages.add(LogMessage.error(this, "%s", e.getMessage()));
                }
            }
        }
        
        // Finally, we merge the relations
        for (String layerName : layerNames) {
            List<RelationPosition> positions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof RelationPosition)
                    .map(pos -> (RelationPosition) pos)
                    .collect(Collectors.toList());
            
            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} relation positions on layer [{}]", positions.size(),
                    layerName);
            
            for (RelationPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
                
                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }
                
                try {
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative();
                    mergeRelationAnnotation(aTargetDocument, aTargetUsername,
                            type2layer.get(position.getType()), aTargetCas, sourceFS, false);
                    LOG.trace(" `-> merged annotation with agreement");
                }
                catch (AnnotationException e) {
                    LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                    messages.add(LogMessage.error(this, "%s", e.getMessage()));
                }
            }
        }
        
        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                    new BulkAnnotationEvent(this, aTargetDocument, aTargetUsername, null));
        }
        
//        Set<FeatureStructure> slotFeaturesToReset = new HashSet<>();
//        Set<FeatureStructure> annotationsToDelete = new HashSet<>();

//        List<RelationPosition> differingRelations = aDiff.getPositions().stream()
//                .filter(pos -> pos instanceof RelationPosition)
//                .map(pos -> (RelationPosition) pos)
//                .collect(Collectors.toList());
//
//        for (RelationPosition position : differingRelations) {
//            LOG.trace("Processing relation position {}", position);
//            ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
//            
//            // Collect the annotations at the current position from the CASes of the annotators
//            Map<String, List<FeatureStructure>> annosPerUser = getAllRelationAnnosOnPosition(
//                    aCases, position);
//            
//            boolean annotatorsAgree = isAgree(annosPerUser, mergeDisjunctAnnotations);
//        }
//
//        for (Position position : aDiff.getPositions()) {
//            LOG.trace("Processing position {}", position);
//            
//            ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
//
//            if (!mergeDisjunctAnnotations && cfgs.getConfigurations(CURATION_USER).isEmpty()) {
//                // incomplete annotations
//                LOG.trace(
//                        "Incomplete annotation not present in curation CAS: no need to do anything");
//                continue;
//            }
//            
//            AnnotationFS mergeAnno = (AnnotationFS) cfgs
//                    .getConfigurations(CURATION_USER).get(0)
//                    .getFs(CURATION_USER, aCases);
//
//            // Get Annotations per user in this position
//            Map<String, List<FeatureStructure>> annosPerUser = getAllAnnosOnPosition(aCases,
//                    users, position);
//            
//            boolean annotatorsAgree = isAgree(annosPerUser, mergeDisjunctAnnotations);
//
//            List<FeatureStructure> mergedFSes = annosPerUser.get(CURATION_USER);
//            if (mergedFSes.isEmpty()) {
//                // The curator view does not include any annotation at the current position, so
//                // we need to check if we need to add it
//                if (mergeDisjunctAnnotations) {
//                    // FIXME
//                }
//                else {
//                    // Incomplete annotations are treated as disagreement
//                    // Since the curator CAS does already not include the annotation, we do not
//                    // have to remove it - so we don't have to do anything in this case.
//                    LOG.trace(
//                            "Incomplete annotation not present in curation CAS: no need to do anything");
//                }
//            }
//            else {
//                // The curator view includes at least one annotation at the current position, so
//                // we need to check if we can keep it
//                nextAnnotation: for (FeatureStructure fs : mergedFSes) {
//                    // incomplete annotations
//                    if (!mergeDisjunctAnnotations && (aCases.size() != annosPerUser.size())) {
//                        LOG.trace(
//                                "Incomplete annotation present in curation CAS: scheduling for deletion");
//                        annotationsToDelete.add(fs);
//                        continue nextAnnotation;
//                    }
//                    
//                    // agreed and not stacked
//                    if (annotatorsAgree) {
//                        // Is this a relation? If yes, then also require agreement on source and
//                        // target annotation
//                        Type t = fs.getType();
//                        Feature sourceFeat = t.getFeatureByBaseName(FEAT_REL_SOURCE);
//                        Feature targetFeat = t.getFeatureByBaseName(FEAT_REL_TARGET);
//                        if (sourceFeat != null && targetFeat != null) {
//                            AnnotationFS source = (AnnotationFS) fs
//                                    .getFeatureValue(sourceFeat);
//                            AnnotationFS target = (AnnotationFS) fs
//                                    .getFeatureValue(targetFeat);
//    
//                            // all span anno on this source positions
//                            Map<String, List<FeatureStructure>> sourceAnnosPerUser = 
//                                    getAllAnnosOnPosition(aCases, users, source);
//                            // all span anno on this target positions
//                            Map<String, List<FeatureStructure>> targetAnnosPerUser = 
//                                    getAllAnnosOnPosition(aCases, users, target);
//    
//                            if (
//                                    isAgree(sourceAnnosPerUser, false) && 
//                                    isAgree(targetAnnosPerUser, false)
//                            ) {
//                                LOG.trace("Annotators agree on relation {}: keeping (resetting any slots)",
//                                        annosPerUser.values().iterator().next());
//                                slotFeaturesToReset.add(fs);
//                            }
//                            else {
//                                LOG.trace("Annotators disagree on relation {}: scheduling for deletion",
//                                        annosPerUser.values().iterator().next());
//                                annotationsToDelete.add(fs);
//                            }
//                        }
//                        else {
//                            LOG.trace("Annotators agree on span {}: keeping (resetting any slots)",
//                                    annosPerUser.values().iterator().next());
//                            slotFeaturesToReset.add(fs);
//                        }
//                        
//                        continue nextAnnotation;
//                    }
//                    
//                    // disagree or stacked annotations
//                    LOG.trace("Annotators disagree on {}: scheduling for deletion",
//                            annosPerUser.values().iterator().next());
//                    annotationsToDelete.add(fs);
//                    continue nextAnnotation;
//                }
//            }
//        }
//
//        // remove annotations that do not agree or are a stacked ones
//        for (FeatureStructure fs : annotationsToDelete) {
//            if (!slotFeaturesToReset.contains(fs)) {
//                CAS mergeCas = aCases.get(CURATION_USER);
//                // If this difference is attached to the Token remove it from there as well
//                Type tokenType = CasUtil.getType(mergeCas, Token.class);
//                Type type = fs.getType();
//                int fsBegin = ((AnnotationFS) fs).getBegin();
//                int fsEnd = ((AnnotationFS) fs).getEnd();
//                if (type.getName().equals(POS.class.getName())) {
//                    AnnotationFS t = selectCovered(mergeCas, tokenType, fsBegin, fsEnd).get(0);
//                    FSUtil.setFeature(t, "pos", (FeatureStructure) null);
//                }
//                if (type.getName().equals(Stem.class.getName())) {
//                    AnnotationFS t = selectCovered(mergeCas, tokenType, fsBegin, fsEnd).get(0);
//                    FSUtil.setFeature(t, "stem", (FeatureStructure) null);
//                }
//                if (type.getName().equals(Lemma.class.getName())) {
//                    AnnotationFS t = selectCovered(mergeCas, tokenType, fsBegin, fsEnd).get(0);
//                    FSUtil.setFeature(t, "lemma", (FeatureStructure) null);
//                }
//                if (type.getName().equals(MorphologicalFeatures.class.getName())) {
//                    AnnotationFS t = selectCovered(mergeCas, tokenType, fsBegin, fsEnd).get(0);
//                    FSUtil.setFeature(t, "morph", (FeatureStructure) null);
//                }
//                mergeCas.removeFsFromIndexes(fs);
//            }
//        }
//        
//        // if slot bearing annotation, clean
//        for (FeatureStructure baseFs : slotFeaturesToReset) {
//            for (Feature roleFeature : baseFs.getType().getFeatures()) {
//                if (isLinkMode(baseFs, roleFeature)) {
//                    // FeatureStructure roleFs = baseFs.getFeatureValue(f);
//                    ArrayFS roleFss = (ArrayFS) WebAnnoCasUtil.getFeatureFS(baseFs,
//                            roleFeature.getShortName());
//                    if (roleFss == null) {
//                        continue;
//                    }
//                    Map<String, ArrayFS> roleAnnosPerUser = new HashMap<>();
//
//                    setAllRoleAnnosOnPosition(aCases, roleAnnosPerUser, users, baseFs,
//                            roleFeature);
//                    List<FeatureStructure> linkFSes = new LinkedList<>(
//                            Arrays.asList(roleFss.toArray()));
//                    for (FeatureStructure roleFs : roleFss.toArray()) {
//                        if (isRoleAgree(roleFs, roleAnnosPerUser)) {
//                            for (Feature targetFeature : roleFs.getType().getFeatures()) {
//                                if (isBasicFeature(targetFeature)) {
//                                    continue;
//                                }
//                                if (!targetFeature.getShortName().equals("target")) {
//                                    continue;
//                                }
//                                AnnotationFS targetFs = (AnnotationFS) roleFs
//                                        .getFeatureValue(targetFeature);
//                                if (targetFs == null) {
//                                    continue;
//                                }
//                                Map<String, List<FeatureStructure>> targetAnnosPerUser = 
//                                        getAllAnnosOnPosition(aCases, users, targetFs);
//
//                                // do not agree on targets
//                                if (!isAgree(targetAnnosPerUser, false)) {
//                                    linkFSes.remove(roleFs);
//                                }
//                            }
//                        }
//                        // do not agree on some role features
//                        else {
//                            linkFSes.remove(roleFs);
//                        }
//                    }
//
//                    ArrayFS array = baseFs.getCAS().createArrayFS(linkFSes.size());
//                    array.copyFromArray(linkFSes.toArray(new FeatureStructure[linkFSes.size()]),
//                            0, 0, linkFSes.size());
//                    baseFs.setFeatureValue(roleFeature, array);
//                }
//            }
//        }
    }

    private static void clearAnnotations(CAS aCas)
        throws UIMAException
    {
        CAS backup = createCas();
        
        // Copy the CAS - basically we do this just to keep the full type system information
        CASCompleteSerializer serializer = serializeCASComplete((CASImpl) aCas);
        deserializeCASComplete(serializer, (CASImpl) backup);

        // Remove all annotations from the target CAS but we keep the type system!
        aCas.reset();
        
        // Copy over essential information
        if (exists(backup, getType(backup, DocumentMetaData.class))) {
            copyDocumentMetadata(backup, aCas);
        }
        else {
            WebAnnoCasUtil.createDocumentMetadata(aCas);
        }
        aCas.setDocumentLanguage(backup.getDocumentLanguage()); // DKPro Core Issue 435
        aCas.setDocumentText(backup.getDocumentText());
        
        // Transfer token boundaries
        for (AnnotationFS t : selectTokens(backup)) {
            aCas.addFsToIndexes(createToken(aCas, t.getBegin(), t.getEnd()));
        }

        // Transfer sentence boundaries
        for (AnnotationFS s : selectSentences(backup)) {
            aCas.addFsToIndexes(createSentence(aCas, s.getBegin(), s.getEnd()));
        }
    }
    /**
     * Do not check on agreement on Position and SOfa feature - already checked
     */
    private static boolean isBasicFeature(Feature aFeature)
    {
        // FIXME The two parts of this OR statement seem to be redundant. Also the order
        // of the check should be changes such that equals is called on the constant.
        return aFeature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                || aFeature.toString().equals("uima.cas.AnnotationBase:sofa");
    }

    /**
     * Return true if these two annotations agree on every non slot features
     */
    private static boolean isSameAnno(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        // Check offsets (because they are excluded by shouldIgnoreFeatureOnMerge())
        if (aFs1.getBegin() != aFs2.getBegin() || aFs1.getEnd() != aFs2.getEnd()) {
            return false;
        }
        
        // Check the features (basically limiting to the primitive features)
        for (Feature f : aFs1.getType().getFeatures()) {
            if (shouldIgnoreFeatureOnMerge(aFs1, f)) {
                continue;
            }

            Object value1 = getFeatureValue(aFs1, f);
            Object value2 = getFeatureValue(aFs2, f);
            
            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the feature value of this {@code Feature} on this annotation
     */
    private static Object getFeatureValue(FeatureStructure aFS, Feature aFeature)
    {
        switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING:
            return aFS.getFeatureValueAsString(aFeature);
        case CAS.TYPE_NAME_BOOLEAN:
            return aFS.getBooleanValue(aFeature);
        case CAS.TYPE_NAME_FLOAT:
            return aFS.getFloatValue(aFeature);
        case CAS.TYPE_NAME_INTEGER:
            return aFS.getIntValue(aFeature);
        case CAS.TYPE_NAME_BYTE:
            return aFS.getByteValue(aFeature);
        case CAS.TYPE_NAME_DOUBLE:
            return aFS.getDoubleValue(aFeature);
        case CAS.TYPE_NAME_LONG:
            aFS.getLongValue(aFeature);
        case CAS.TYPE_NAME_SHORT:
            aFS.getShortValue(aFeature);
        default:
            return null;
        // return aFS.getFeatureValue(aFeature);
        }
    }

    private static boolean existsSameAt(CAS aCas, AnnotationFS aFs)
    {
        return CasUtil.selectAt(aCas, aFs.getType(), aFs.getBegin(), aFs.getEnd()).stream()
                .filter(cand -> isSameAnno(aFs, cand))
                .findAny()
                .isPresent();
    }

    private static List<AnnotationFS> selectCandidateRelationsAt(CAS aTargetCas,
            AnnotationFS aSourceFs, AnnotationFS aSourceOriginFs, AnnotationFS aSourceTargetFs)
    {
        Type type = aSourceFs.getType();
        Feature sourceFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);
        return selectCovered(aTargetCas, type, aSourceFs.getBegin(), aSourceFs.getEnd()).stream()
                .filter(fs -> fs.getFeatureValue(sourceFeat).equals(aSourceOriginFs)
                        && fs.getFeatureValue(targetFeat).equals(aSourceTargetFs))
                .collect(Collectors.toList());
    }

    private void copyFeatures(SourceDocument aDocument, String aUsername, TypeAdapter aAdapter,
            FeatureStructure aTargetFS, FeatureStructure aSourceFs)
    {
        // Cache the feature list instead of hammering the database
        List<AnnotationFeature> features = featureCache.computeIfAbsent(aAdapter.getLayer(),
            key -> schemaService.listAnnotationFeature(key));
        for (AnnotationFeature feature : features) {
            Type sourceFsType = aAdapter.getAnnotationType(aSourceFs.getCAS());
            Feature sourceFeature = sourceFsType.getFeatureByBaseName(feature.getName());

            if (sourceFeature == null) {
                throw new IllegalStateException("Target CAS type [" + sourceFsType.getName()
                        + "] does not define a feature named [" + feature.getName() + "]");
            }

            if (shouldIgnoreFeatureOnMerge(aSourceFs, sourceFeature)) {
                continue;
            }

            Object value = aAdapter.getFeatureValue(feature, aSourceFs);
            aAdapter.setFeatureValue(aDocument, aUsername, aTargetFS.getCAS(), getAddr(aTargetFS),
                    feature, value);
        }
    }

    private static List<AnnotationFS> getCandidateAnnotations(CAS aTargetCas, AnnotationFS aSource)
    {
        return selectCovered(aTargetCas, aSource.getType(), aSource.getBegin(), aSource.getEnd())
                .stream()
                .filter(fs -> isSameAnno(fs, aSource))
                .collect(Collectors.toList());
    }

    private static boolean shouldIgnoreFeatureOnMerge(FeatureStructure aFS, Feature aFeature)
    {
        return !WebAnnoCasUtil.isPrimitiveType(aFeature.getRange()) || 
                isBasicFeature(aFeature) ||
                aFeature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN) ||
                aFeature.getName().equals(CAS.FEATURE_FULL_NAME_END);
    }

    public CasMergeOpertationResult mergeSpanAnnotation(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas, AnnotationFS aSourceFs,
            boolean aAllowStacking)
        throws AnnotationException
    {
        if (existsSameAt(aTargetCas, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        SpanAdapter adapter = (SpanAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }

        // a) if stacking allowed add this new annotation to the mergeview
        List<AnnotationFS> existingAnnos = selectAt(aTargetCas, aSourceFs.getType(),
                aSourceFs.getBegin(), aSourceFs.getEnd());
        if (existingAnnos.isEmpty() || aAllowStacking) {
            // Create the annotation via the adapter - this also takes care of attaching to an
            // annotation if necessary
            AnnotationFS mergedSpan = adapter.add(aDocument, aUsername, aTargetCas,
                    aSourceFs.getBegin(), aSourceFs.getEnd());
            
            copyFeatures(aDocument, aUsername, adapter, mergedSpan, aSourceFs);
            return CasMergeOpertationResult.CREATED;
        }
        // b) if stacking is not allowed, modify the existing annotation with this one
        else {
            copyFeatures(aDocument, aUsername, adapter, existingAnnos.get(0), aSourceFs);
            return CasMergeOpertationResult.UPDATED;
        }
    }

    public CasMergeOpertationResult mergeRelationAnnotation(SourceDocument aDocument,
            String aUsername, AnnotationLayer aAnnotationLayer, CAS aTargetCas,
            AnnotationFS aSourceFs, boolean aAllowStacking)
        throws AnnotationException
    {
        RelationAdapter adapter = (RelationAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }
        
        AnnotationFS originFsClicked = FSUtil.getFeature(aSourceFs,
                adapter.getSourceFeatureName(), AnnotationFS.class);
        AnnotationFS targetFsClicked = FSUtil.getFeature(aSourceFs,
                adapter.getTargetFeatureName(), AnnotationFS.class);
        
        List<AnnotationFS> candidateRelations = getCandidateAnnotations(aTargetCas, aSourceFs);
        List<AnnotationFS> candidateOrigins = getCandidateAnnotations(aTargetCas, originFsClicked);
        List<AnnotationFS> candidateTargets = getCandidateAnnotations(aTargetCas, targetFsClicked);

        // check if target/source exists in the mergeview
        if (candidateOrigins.isEmpty() || candidateTargets.isEmpty()) {
            throw new UnfulfilledPrerequisitesException("Both the source and target annotation"
                    + " must exist in the target document. Please first merge/create them");
        }

        if (candidateOrigins.size() > 1) {
            throw new MergeConflictException(
                    "Stacked sources exist in the target document. Cannot merge this relation.");
        }
        
        if (candidateTargets.size() > 1) {
            throw new MergeConflictException(
                    "Stacked targets exist in the target document. Cannot merge this relation.");
        }
        
        if (!candidateRelations.isEmpty()) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        AnnotationFS originFs = candidateOrigins.get(0);
        AnnotationFS targetFs = candidateTargets.get(0);
        
        if (adapter.getAttachFeatureName() != null) {
            AnnotationFS originAttachAnnotation = FSUtil.getFeature(originFs,
                    adapter.getAttachFeatureName(), AnnotationFS.class);
            AnnotationFS targetAttachAnnotation = FSUtil.getFeature(targetFs,
                    adapter.getAttachFeatureName(), AnnotationFS.class);
            
            if (originAttachAnnotation == null || targetAttachAnnotation == null) {
                throw new UnfulfilledPrerequisitesException(
                        "No annotation to attach to. Cannot merge this relation.");
            }
        }
        
        List<AnnotationFS> existingAnnos = selectCandidateRelationsAt(aTargetCas, aSourceFs,
                originFs, targetFs);
        if (existingAnnos.isEmpty() || aAllowStacking) {
            AnnotationFS mergedRelation = adapter.add(aDocument, aUsername, originFs, targetFs,
                    aTargetCas);
            copyFeatures(aDocument, aUsername, adapter, mergedRelation, aSourceFs);
            return CasMergeOpertationResult.CREATED;
        }
        else {
            copyFeatures(aDocument, aUsername, adapter, existingAnnos.get(0), aSourceFs);
            return CasMergeOpertationResult.UPDATED;
        }
    }

    public void mergeSlotFeature(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas,
            AnnotationFS aSourceFs, String aSourceFeature, int aSourceSlotIndex)
        throws AnnotationException
    {
        TypeAdapter adapter = adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }
        
        List<AnnotationFS> candidateHosts = getCandidateAnnotations(aTargetCas, aSourceFs);

        AnnotationFS targetFs;
        if (candidateHosts.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "The base annotation do not exist. Please add it first. ");
        }
        AnnotationFS mergeFs = candidateHosts.get(0);
        int liIndex = aSourceSlotIndex;

        AnnotationFeature slotFeature = null;
        LinkWithRoleModel linkRole = null;
        f: for (AnnotationFeature feat : adapter.listFeatures()) {
            if (!aSourceFeature.equals(feat.getName())) {
                continue;
            }
            
            if (MultiValueMode.ARRAY.equals(feat.getMultiValueMode())
                    && LinkMode.WITH_ROLE.equals(feat.getLinkMode())) {
                List<LinkWithRoleModel> links = adapter.getFeatureValue(feat, aSourceFs);
                for (int li = 0; li < links.size(); li++) {
                    LinkWithRoleModel link = links.get(li);
                    if (li == liIndex) {
                        slotFeature = feat;

                        List<AnnotationFS> targets = checkAndGetTargets(aTargetCas,
                                selectAnnotationByAddr(aSourceFs.getCAS(), link.targetAddr));
                        targetFs = targets.get(0);
                        link.targetAddr = getAddr(targetFs);
                        linkRole = link;
                        break f;
                    }
                }
            }
        }

        List<LinkWithRoleModel> links = adapter.getFeatureValue(slotFeature, mergeFs);
        LinkWithRoleModel duplicateLink = null; //
        for (LinkWithRoleModel lr : links) {
            if (lr.targetAddr == linkRole.targetAddr) {
                duplicateLink = lr;
                break;
            }
        }
        links.add(linkRole);
        links.remove(duplicateLink);

        setFeature(mergeFs, slotFeature, links);
    }

    private static List<AnnotationFS> checkAndGetTargets(CAS aCas, AnnotationFS aOldTraget)
        throws UnfulfilledPrerequisitesException
    {
        List<AnnotationFS> targets = getCandidateAnnotations(aCas, aOldTraget);

        if (targets.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "This target annotation do not exist. Copy or create the target first ");
        }

        if (targets.size() > 1) {
            throw new UnfulfilledPrerequisitesException("There are multiple targets on the mergeview."
                    + " Can not copy this slot annotation.");
        }
        return targets;
    }
}
