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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isBasicFeature;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isEquivalentSpanAnnotation;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isPrimitiveType;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalFeatureValueException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
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

        adapterCache = Caffeine.newBuilder().maximumSize(100).build(schemaService::getAdapter);
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
        boolean stacked = cfgs.getConfigurations().stream().filter(Configuration::isStacked)
                .findAny().isPresent();
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

        int updated = 0;
        int created = 0;
        Set<LogMessage> messages = new LinkedHashSet<>();

        // Remove any annotations from the target CAS - keep type system, sentences and tokens
        clearAnnotations(aTargetCas);

        // If there is nothing to merge, bail out
        if (aCases.isEmpty()) {
            return;
        }

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        Map<String, AnnotationLayer> type2layer = aDiff.getPositions().stream()
                .map(Position::getType).distinct()
                .map(type -> schemaService.findLayer(aTargetDocument.getProject(), type))
                .collect(toMap(AnnotationLayer::getName, identity()));

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
                    .filter(pos -> pos instanceof SpanPosition).map(pos -> (SpanPosition) pos)
                    // We don't process slot features here (they are span sub-positions)
                    .filter(pos -> pos.getFeature() == null).collect(Collectors.toList());

            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} span positions on layer {}", positions.size(), layerName);

            // First we merge the spans so that we can attach the relations to something later.
            // Slots are also excluded for the moment
            for (SpanPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurationSet(position);

                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }

                try {
                    Map<String, List<CAS>> casMap = new LinkedHashMap<>();
                    aCases.forEach((k, v) -> casMap.put(k, asList(v)));
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative(casMap);
                    CasMergeOperationResult result = mergeSpanAnnotation(aTargetDocument,
                            aTargetUsername, type2layer.get(position.getType()), aTargetCas,
                            sourceFS, false);
                    LOG.trace(" `-> merged annotation with agreement");

                    switch (result.getState()) {
                    case CREATED:
                        created++;
                        break;
                    case UPDATED:
                        updated++;
                        break;
                    }
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
                    .filter(pos -> pos instanceof SpanPosition).map(pos -> (SpanPosition) pos)
                    // We only process slot features here
                    .filter(pos -> pos.getFeature() != null).collect(Collectors.toList());

            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} slot positions on layer [{}]", positions.size(), layerName);

            for (SpanPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurationSet(position);

                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }

                try {
                    Map<String, List<CAS>> casMap = new LinkedHashMap<>();
                    aCases.forEach((k, v) -> casMap.put(k, asList(v)));
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative(casMap);
                    AID sourceFsAid = cfgs.getConfigurations().get(0).getRepresentativeAID();
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
                    .map(pos -> (RelationPosition) pos).collect(Collectors.toList());

            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} relation positions on layer [{}]", positions.size(),
                    layerName);

            for (RelationPosition position : positions) {
                LOG.trace(" |   processing {}", position);
                ConfigurationSet cfgs = aDiff.getConfigurationSet(position);

                if (!shouldMerge(aDiff, cfgs)) {
                    continue;
                }

                try {
                    Map<String, List<CAS>> casMap = new LinkedHashMap<>();
                    aCases.forEach((k, v) -> casMap.put(k, asList(v)));
                    AnnotationFS sourceFS = (AnnotationFS) cfgs.getConfigurations().get(0)
                            .getRepresentative(casMap);
                    CasMergeOperationResult result = mergeRelationAnnotation(aTargetDocument,
                            aTargetUsername, type2layer.get(position.getType()), aTargetCas,
                            sourceFS, false);
                    LOG.trace(" `-> merged annotation with agreement");

                    switch (result.getState()) {
                    case CREATED:
                        created++;
                        break;
                    case UPDATED:
                        updated++;
                        break;
                    }
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
    }

    private static void clearAnnotations(CAS aCas) throws UIMAException
    {
        CAS backup = CasFactory.createCas((TypeSystemDescription) null);

        // Copy the CAS - basically we do this just to keep the full type system information
        CASCompleteSerializer serializer = serializeCASComplete((CASImpl) getRealCas(aCas));
        deserializeCASComplete(serializer, (CASImpl) getRealCas(backup));

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

    private static boolean existsEquivalentAt(CAS aCas, TypeAdapter aAdapter, AnnotationFS aFs)
    {
        return selectAt(aCas, aFs.getType(), aFs.getBegin(), aFs.getEnd()).stream() //
                .filter(cand -> aAdapter.equivalents(aFs, cand,
                        (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_fs, _f))) //
                .findAny().isPresent();
    }

    private static List<AnnotationFS> selectCandidateRelationsAt(CAS aTargetCas,
            AnnotationFS aSourceFs, AnnotationFS aSourceOriginFs, AnnotationFS aSourceTargetFs)
    {
        Type type = aSourceFs.getType();
        Type targetType = CasUtil.getType(aTargetCas, aSourceFs.getType().getName());
        Feature sourceFeat = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeat = type.getFeatureByBaseName(FEAT_REL_TARGET);
        return selectCovered(aTargetCas, targetType, aSourceFs.getBegin(), aSourceFs.getEnd())
                .stream()
                .filter(fs -> fs.getFeatureValue(sourceFeat).equals(aSourceOriginFs)
                        && fs.getFeatureValue(targetFeat).equals(aSourceTargetFs))
                .collect(toList());
    }

    private void copyFeatures(SourceDocument aDocument, String aUsername, TypeAdapter aAdapter,
            FeatureStructure aTargetFS, FeatureStructure aSourceFs)
        throws AnnotationException
    {
        // Cache the feature list instead of hammering the database
        List<AnnotationFeature> features = featureCache.computeIfAbsent(aAdapter.getLayer(),
                key -> schemaService.listSupportedFeatures(key));
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

            try {
                aAdapter.setFeatureValue(aDocument, aUsername, aTargetFS.getCAS(),
                        getAddr(aTargetFS), feature, value);
            }
            catch (IllegalArgumentException e) {
                // This happens e.g. if the value we try to set is not in the tagset and the tagset
                // cannot be extended.
                throw new IllegalFeatureValueException("Cannot set value of feature ["
                        + feature.getUiName() + "] to [" + value + "]: " + e.getMessage(), e);
            }
        }
    }

    private static List<AnnotationFS> getCandidateAnnotations(CAS aTargetCas, TypeAdapter aAdapter,
            AnnotationFS aSource)
    {
        Type targetType = CasUtil.getType(aTargetCas, aSource.getType().getName());
        return selectCovered(aTargetCas, targetType, aSource.getBegin(), aSource.getEnd()).stream()
                .filter(fs -> aAdapter.equivalents(fs, aSource,
                        (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_fs, _f)))
                .collect(toList());
    }

    public CasMergeOperationResult mergeSpanAnnotation(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas, AnnotationFS aSourceFs,
            boolean aAllowStacking)
        throws AnnotationException
    {
        SpanAdapter adapter = (SpanAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }

        if (existsEquivalentAt(aTargetCas, adapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        // a) if stacking allowed add this new annotation to the mergeview
        Type targetType = CasUtil.getType(aTargetCas, adapter.getAnnotationTypeName());
        List<AnnotationFS> existingAnnos = selectAt(aTargetCas, targetType, aSourceFs.getBegin(),
                aSourceFs.getEnd());
        if (existingAnnos.isEmpty() || aAllowStacking) {
            // Create the annotation via the adapter - this also takes care of attaching to an
            // annotation if necessary
            AnnotationFS mergedSpan = adapter.add(aDocument, aUsername, aTargetCas,
                    aSourceFs.getBegin(), aSourceFs.getEnd());

            int mergedSpanAddr = -1;
            try {
                copyFeatures(aDocument, aUsername, adapter, mergedSpan, aSourceFs);
                mergedSpanAddr = getAddr(mergedSpan);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                adapter.delete(aDocument, aUsername, aTargetCas, new VID(mergedSpan));
                throw e;
            }
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.CREATED,
                    mergedSpanAddr);
        }
        // b) if stacking is not allowed, modify the existing annotation with this one
        else {
            AnnotationFS annoToUpdate = existingAnnos.get(0);
            copyFeatures(aDocument, aUsername, adapter, annoToUpdate, aSourceFs);
            int mergedSpanAddr = getAddr(annoToUpdate);
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                    mergedSpanAddr);
        }
    }

    public CasMergeOperationResult mergeRelationAnnotation(SourceDocument aDocument,
            String aUsername, AnnotationLayer aAnnotationLayer, CAS aTargetCas,
            AnnotationFS aSourceFs, boolean aAllowStacking)
        throws AnnotationException
    {
        RelationAdapter relationAdapter = (RelationAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            relationAdapter.silenceEvents();
        }

        if (existsEquivalentAt(aTargetCas, relationAdapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        AnnotationFS originFsClicked = getFeature(aSourceFs, relationAdapter.getSourceFeatureName(),
                AnnotationFS.class);
        AnnotationFS targetFsClicked = getFeature(aSourceFs, relationAdapter.getTargetFeatureName(),
                AnnotationFS.class);

        SpanAdapter spanAdapter = (SpanAdapter) adapterCache.get(aAnnotationLayer.getAttachType());

        List<AnnotationFS> candidateOrigins = getCandidateAnnotations(aTargetCas, spanAdapter,
                originFsClicked);
        List<AnnotationFS> candidateTargets = getCandidateAnnotations(aTargetCas, spanAdapter,
                targetFsClicked);

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

        AnnotationFS originFs = candidateOrigins.get(0);
        AnnotationFS targetFs = candidateTargets.get(0);

        if (relationAdapter.getAttachFeatureName() != null) {
            AnnotationFS originAttachAnnotation = FSUtil.getFeature(originFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);
            AnnotationFS targetAttachAnnotation = FSUtil.getFeature(targetFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);

            if (originAttachAnnotation == null || targetAttachAnnotation == null) {
                throw new UnfulfilledPrerequisitesException(
                        "No annotation to attach to. Cannot merge this relation.");
            }
        }

        List<AnnotationFS> existingAnnos = selectCandidateRelationsAt(aTargetCas, aSourceFs,
                originFs, targetFs);
        if (existingAnnos.isEmpty() || aAllowStacking) {
            AnnotationFS mergedRelation = relationAdapter.add(aDocument, aUsername, originFs,
                    targetFs, aTargetCas);
            try {
                copyFeatures(aDocument, aUsername, relationAdapter, mergedRelation, aSourceFs);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                relationAdapter.delete(aDocument, aUsername, aTargetCas, new VID(mergedRelation));
            }
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.CREATED,
                    getAddr(mergedRelation));
        }
        else {
            AnnotationFS mergeTargetFS = existingAnnos.get(0);
            copyFeatures(aDocument, aUsername, relationAdapter, mergeTargetFS, aSourceFs);
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                    getAddr(mergeTargetFS));
        }
    }

    public CasMergeOperationResult mergeSlotFeature(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas, AnnotationFS aSourceFs,
            String aSourceFeature, int aSourceSlotIndex)
        throws AnnotationException
    {
        TypeAdapter adapter = adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }

        List<AnnotationFS> candidateHosts = getCandidateAnnotations(aTargetCas, adapter, aSourceFs);

        AnnotationFS targetFs;
        if (candidateHosts.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "The base annotation does not exist. Please add it first. ");
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
        return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                getAddr(mergeFs));
    }

    private static List<AnnotationFS> checkAndGetTargets(CAS aCas, AnnotationFS aOldTarget)
        throws UnfulfilledPrerequisitesException
    {
        Type casType = CasUtil.getType(aCas, aOldTarget.getType().getName());
        List<AnnotationFS> targets = selectCovered(aCas, casType, aOldTarget.getBegin(),
                aOldTarget.getEnd())
                        .stream()
                        .filter(fs -> isEquivalentSpanAnnotation(fs, aOldTarget,
                                (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_fs, _f)))
                        .collect(Collectors.toList());

        if (targets.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "This target annotation do not exist. Copy or create the target first ");
        }

        if (targets.size() > 1) {
            throw new UnfulfilledPrerequisitesException(
                    "There are multiple targets on the mergeview."
                            + " Can not copy this slot annotation.");
        }

        return targets;
    }

    public static boolean shouldIgnoreFeatureOnMerge(FeatureStructure aFS, Feature aFeature)
    {
        return !isPrimitiveType(aFeature.getRange()) || isBasicFeature(aFeature)
                || aFeature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                || aFeature.getName().equals(CAS.FEATURE_FULL_NAME_END);
    }
}
