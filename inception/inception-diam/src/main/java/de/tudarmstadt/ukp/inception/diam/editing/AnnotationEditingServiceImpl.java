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
package de.tudarmstadt.ukp.inception.diam.editing;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureUtil;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * Default implementation of {@link AnnotationEditingService}.
 */
public class AnnotationEditingServiceImpl
    implements AnnotationEditingService
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditingServiceImpl.class);

    private final AnnotationSchemaService annotationService;

    public AnnotationEditingServiceImpl(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public List<LogMessage> deleteAnnotation(CAS aCas, AnnotatorState aState, VID aVid,
            AnnotationLayer aLayer, TypeAdapter aAdapter)
        throws AnnotationException
    {
        var messages = new ArrayList<LogMessage>();
        var modified = new MutableBoolean(false);

        try {
            deleteAnnotation(aCas, aState, aVid, aLayer, aAdapter, messages, modified);
        }
        catch (AnnotationException e) {
            // If we had already started modifying the CAS, the caller cannot simply carry on with
            // it - it now contains cleanup for an annotation that is still there.
            if (modified.isTrue()) {
                throw new PartialDeleteException(e, messages);
            }

            throw e;
        }

        return messages;
    }

    private void deleteAnnotation(CAS aCas, AnnotatorState aState, VID aVid, AnnotationLayer aLayer,
            TypeAdapter aAdapter, List<LogMessage> aMessages, MutableBoolean aModified)
        throws AnnotationException
    {
        var fs = selectAnnotationByAddr(aCas, aVid.getId());

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because we do currently not allow to create spans
        // that attach to other spans. The only span type for which this is relevant is the Token
        // type which cannot be deleted.
        if (aAdapter instanceof SpanAdapter) {
            for (var attachFeature : annotationService
                    .listAttachedSpanFeatures(aAdapter.getLayer())) {
                var attachedFs = FSUtil.getFeature(fs, attachFeature.getName(), AnnotationFS.class);
                if (attachedFs == null) {
                    continue;
                }

                var attachedSpanLayerAdapter = annotationService.findAdapter(aState.getProject(),
                        attachedFs);

                deleteAnnotation(aCas, aState, VID.of(attachedFs),
                        attachedSpanLayerAdapter.getLayer(), attachedSpanLayerAdapter, aMessages,
                        aModified);
            }
        }

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        if (aAdapter instanceof SpanAdapter) {
            for (var rel : annotationService.getAttachedRels(aLayer, fs)) {
                var relationAdapter = (RelationAdapter) annotationService
                        .findAdapter(aState.getProject(), rel.getRelation());

                relationAdapter.delete(aState.getDocument(), aState.getUser().getUsername(), aCas,
                        VID.of(rel.getRelation()));
                aModified.setTrue();
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (aAdapter instanceof SpanAdapter) {
            cleanUpLinkFeatures(aCas, fs, (SpanAdapter) aAdapter, aState, aMessages, aModified);
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (aAdapter instanceof RelationAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        aAdapter.delete(aState.getDocument(), aState.getUser().getUsername(), aCas, aVid);
        aModified.setTrue();
    }

    private void cleanUpLinkFeatures(CAS aCas, FeatureStructure fs, SpanAdapter aAdapter,
            AnnotatorState aState, List<LogMessage> aMessages, MutableBoolean aModified)
    {
        for (var linkFeature : annotationService.listAttachedLinkFeatures(aAdapter.getLayer())) {
            var linkHostType = CasUtil.getType(aCas, linkFeature.getLayer().getName());

            for (var linkHostFS : aCas.select(linkHostType)) {
                List<LinkWithRoleModel> links = aAdapter.getFeatureValue(linkFeature, linkHostFS);
                var i = links.iterator();
                var modified = false;
                while (i.hasNext()) {
                    var link = i.next();
                    if (link.targetAddr == getAddr(fs)) {
                        i.remove();
                        aMessages.add(LogMessage.info(this,
                                "Cleared slot [%s] in feature [%s] on [%s]", link.role,
                                linkFeature.getUiName(), linkFeature.getLayer().getUiName()));
                        LOG.debug("Cleared slot [{}] in feature [{}] on annotation [{}]", link.role,
                                linkFeature.getName(), getAddr(linkHostFS));
                        modified = true;
                    }
                }
                if (modified) {
                    try {
                        aAdapter.setFeatureValue(aState.getDocument(),
                                aState.getUser().getUsername(), aCas, getAddr(linkHostFS),
                                linkFeature, links);
                        aModified.setTrue();
                    }
                    catch (AnnotationException e) {
                        aMessages.add(LogMessage.error(this,
                                "Unable to clean slots in feature [%s] on [%s]",
                                linkFeature.getUiName(), linkFeature.getLayer().getUiName()));
                        LOG.error("Unable to clean slots in feature [{}] on annotation [{}]",
                                linkFeature.getName(), getAddr(linkHostFS));
                    }

                    // If the currently armed slot is part of this link, then we disarm the slot
                    // to avoid the armed slot no longer pointing at the index which the user
                    // had selected it to point at.
                    var armedFeature = aState.getArmedFeature();
                    if (armedFeature != null
                            && ICasUtil.getAddr(linkHostFS) == armedFeature.vid.getId()
                            && armedFeature.feature.equals(linkFeature)) {
                        aState.clearArmedSlot();
                    }
                }
            }
        }
    }

    @Override
    public List<LogMessage> commitFeatureStates(SourceDocument aDocument, String aDataOwner,
            CAS aTargetCas, int aTargetFsAddr, TypeAdapter aAdapter,
            List<FeatureState> aFeatureStates)
    {
        var messages = new ArrayList<LogMessage>();

        try (var ctx = aAdapter.updateFeatureValues(aDocument, aDataOwner, aTargetCas,
                aTargetFsAddr)) {
            for (var featureState : aFeatureStates) {
                try {
                    LOG.trace("Committing feature states to CAS: {} = {}",
                            featureState.feature.getUiName(), featureState.value);
                    ctx.setFeatureValue(featureState.feature, featureState.value);
                }
                catch (Exception e) {
                    messages.add(LogMessage.error(this, "Cannot set feature [%s]: %s",
                            featureState.feature.getUiName(), e.getMessage()));
                }
            }
        }

        return messages;
    }

    @Override
    public AnnotationFS reverseRelation(SourceDocument aDocument, String aDataOwner, CAS aCas,
            int aRelationAddr, TypeAdapter aAdapter, List<FeatureState> aFeatureStates,
            List<LogMessage> aMessages)
        throws AnnotationException
    {
        // FIXME: This would be much better handled inside the RelationAdapter by simply reversing
        // the relation end-points instead of deleting/adding
        if (!(aAdapter instanceof RelationAdapter relationAdapter)) {
            throw new AnnotationException("Only relations can be reversed");
        }

        var oldRelation = selectAnnotationByAddr(aCas, aRelationAddr);

        // Remove old relation
        relationAdapter.delete(aDocument, aDataOwner, aCas, VID.of(oldRelation));

        // Create new relation with reversed endpoints
        var newSource = relationAdapter.getTargetAnnotation(oldRelation);
        var newTarget = relationAdapter.getSourceAnnotation(oldRelation);
        var newRelation = relationAdapter.add(aDocument, aDataOwner, newSource, newTarget, aCas);

        // Apply the feature values of the old relation to the reversed relation
        aMessages.addAll(commitFeatureStates(aDocument, aDataOwner, aCas, getAddr(newRelation),
                relationAdapter, aFeatureStates));

        return newRelation;
    }

    @Override
    public VID createSlotFiller(SourceDocument aDocument, String aDataOwner, CAS aCas,
            AnnotatorState aState, int aBegin, int aEnd)
        throws AnnotationException
    {
        // This only works if the slot filler is a concrete span type defined in the project, not if
        // the user simply defined CAS.TYPE_NAME_ANNOTATION to allow for arbitrary slot fillers. In
        // the latter case, we abort the operation.
        if (CAS.TYPE_NAME_ANNOTATION.equals(aState.getArmedFeature().feature.getType())) {
            throw new IllegalPlacementException(
                    "Unable to create annotation of type [" + CAS.TYPE_NAME_ANNOTATION
                            + "]. Please click an annotation in stead of selecting new text.");
        }

        var adapter = (SpanAdapter) annotationService.getAdapter(annotationService
                .findLayer(aState.getProject(), aState.getArmedFeature().feature.getType()));

        return VID.of(adapter.handle(CreateSpanAnnotationRequest.builder() //
                .withDocument(aDocument, aDataOwner, aCas) //
                .withRange(aBegin, aEnd) //
                .withAnchoringMode(aState.getAnchoringMode()) //
                .build()));
    }

    @Override
    public List<LogMessage> fillSlot(SourceDocument aDocument, String aDataOwner, CAS aCas,
            AnnotatorState aState, int aSlotFillerAddr)
        throws AnnotationException
    {
        // If this method is called when no slot is armed, it must be a bug!
        if (!aState.isSlotArmed()) {
            throw new IllegalStateException("No slot is armed.");
        }

        // Inject the slot filler into the respective slot
        var armedFeature = aState.getArmedFeature();
        var slotHostFS = selectFsByAddr(aCas, armedFeature.vid.getId());
        var slotHostLayer = annotationService.findLayer(aState.getProject(), slotHostFS);
        var slotHostAdapter = annotationService.getAdapter(slotHostLayer);
        @SuppressWarnings("unchecked")
        var links = (List<LinkWithRoleModel>) armedFeature.value;
        var link = links.get(aState.getArmedSlot());
        link.targetAddr = aSlotFillerAddr;
        link.label = selectAnnotationByAddr(aCas, aSlotFillerAddr).getCoveredText();

        return commitFeatureStates(aDocument, aDataOwner, aCas, armedFeature.vid.getId(),
                slotHostAdapter, asList(armedFeature));
    }

    @Override
    public List<FeatureState> loadFeatureStates(CAS aCas, AnnotatorState aState,
            AnnotationLayer aLayer, FeatureStructure aFS,
            Map<AnnotationFeature, Serializable> aRemembered, List<LogMessage> aMessages)
        throws StaleTypeSystemException
    {
        var featureStates = new ArrayList<FeatureState>();

        var adapter = annotationService.getAdapter(aLayer);

        for (var feature : annotationService.listEnabledFeatures(aLayer)) {
            if (isFeatureSuppressed(aState, feature)) {
                continue;
            }

            if (aFS != null && FeatureUtil.getFeature(aFS, feature).isEmpty()) {
                LOG.error("Unable to find [{}] in the current CAS typesystem", feature.getName());
                throw new StaleTypeSystemException(feature);
            }

            FeatureState featureState;
            if (aFS != null) {
                featureState = adapter.getFeatureState(feature, aFS);
            }
            else if (aRemembered != null) {
                var value = aRemembered.get(feature);
                featureState = new FeatureState(null, feature, value);
            }
            else {
                featureState = new FeatureState(null, feature, null);
            }

            populateTagset(aCas, aState, featureState, aMessages);

            featureStates.add(featureState);
        }

        return featureStates;
    }

    private boolean isFeatureSuppressed(AnnotatorState aState, AnnotationFeature aFeature)
    {
        if (ChainLayerSupport.TYPE.equals(aFeature.getLayer().getType())) {
            // For chain layers, we only want to show the "type" and "relation" features...
            // FIXME: This would probably be better handled by introducing special FeatureSupports
            // for these two features and implementing the isAccessible() method accordingly

            if (aState.getSelection().isArc()) {
                if (aFeature.getLayer().isLinkedListBehavior()
                        && COREFERENCE_RELATION_FEATURE.equals(aFeature.getName())) {
                    // Only show the chain relation feature if the linked-list behavior is active
                    return false;
                }

                return true;
            }

            if (COREFERENCE_TYPE_FEATURE.equals(aFeature.getName())) {
                return false;
            }

            return true;
        }

        return false;
    }

    private void populateTagset(CAS aCas, AnnotatorState aState, FeatureState aFeatureState,
            List<LogMessage> aMessages)
    {
        var tagset = aFeatureState.feature.getTagset();
        if (tagset == null) {
            return;
        }

        // verification to check whether constraints exist for this project or NOT
        if (aState.getConstraints() != null && aState.getSelection().getAnnotation().isSet()) {
            populateTagsetBasedOnRules(aCas, aState, aFeatureState, aMessages);
            return;
        }

        aFeatureState.tagset = annotationService.listTagsReorderable(tagset);
    }

    /**
     * Adds and sorts tags based on Constraints rules
     */
    private void populateTagsetBasedOnRules(CAS aCas, AnnotatorState aState, FeatureState aModel,
            List<LogMessage> aMessages)
    {
        aModel.indicator.reset();

        // Fetch possible values from the constraint rules
        List<PossibleValue> possibleValues;
        try {
            var fs = selectFsByAddr(aCas, aState.getSelection().getAnnotation().getId());

            var evaluator = new ConstraintsEvaluator();
            // Only show indicator if this feature can be affected by Constraint rules!
            aModel.indicator.setAffected(evaluator
                    .isPathUsedInAnyRestriction(aState.getConstraints(), fs, aModel.feature));

            possibleValues = evaluator.generatePossibleValues(aState.getConstraints(), fs,
                    aModel.feature);

            LOG.debug("Possible values for [{}] : {}", fs.getType().getName(), aModel.feature,
                    possibleValues);
        }
        catch (Exception e) {
            aMessages.add(LogMessage.error(this, "Unable to evaluate constraints: %s",
                    ExceptionUtils.getRootCauseMessage(e)));
            LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
            possibleValues = new ArrayList<>();
        }

        // Fetch actual tagset
        var tags = annotationService.listTagsReorderable(aModel.feature.getTagset());

        // First add tags which are suggested by rules and exist in tagset
        var tagset = compareSortAndAdd(possibleValues, tags, aModel.indicator);

        // Record the possible values and the (re-ordered) tagset in the feature state
        aModel.possibleValues = possibleValues;
        aModel.tagset = tagset;
    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<ReorderableTag> compareSortAndAdd(List<PossibleValue> aPossibleValues,
            List<ReorderableTag> aTags, RulesIndicator aRulesIndicator)
    {
        var returnList = new ArrayList<ReorderableTag>();

        // if no possible values, means didn't satisfy conditions
        if (aPossibleValues.isEmpty()) {
            aRulesIndicator.didntMatchAnyRule();
            return aTags;
        }

        var tagIndex = new LinkedHashMap<String, ReorderableTag>();
        for (ReorderableTag tag : aTags) {
            tagIndex.put(tag.getName(), tag);
        }

        for (var value : aPossibleValues) {
            var tag = tagIndex.get(value.getValue());
            if (tag == null) {
                continue;
            }

            // Matching values found in tagset and shown in dropdown
            aRulesIndicator.rulesApplied();
            // HACK BEGIN
            tag.setReordered(true);
            // HACK END
            returnList.add(tag);
            // Avoid duplicate entries
            tagIndex.remove(value.getValue());
        }

        // If no matching tags found
        if (returnList.isEmpty()) {
            aRulesIndicator.didntMatchAnyTag();
        }

        // Add all remaining non-matching tags to the list
        returnList.addAll(tagIndex.values());

        return returnList;
    }

    @Override
    public AttachStatus checkAttachStatus(Project aProject, AnnotationFS aFS)
    {
        var layer = annotationService.findLayer(aProject, aFS);

        var attachStatus = new AttachStatus();

        var attachedRels = annotationService.getAttachedRels(layer, aFS);
        var attachedToReadOnlyRels = attachedRels.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyRels) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedRels.size();

        // We do not count these atm since they only exist for built-in layers and are not
        // visible in the UI for the user.
        // Set<AnnotationFS> attachedSpans = getAttachedSpans(aFS, layer);
        // boolean attachedToReadOnlySpans = attachedSpans.stream().anyMatch(relFS -> {
        // AnnotationLayer relLayer = annotationService.getLayer(aProject, relFS);
        // return relLayer.isReadonly();
        // });
        // if (attachedToReadOnlySpans) {
        // attachStatus.readOnlyAttached |= true;
        // }
        // attachStatus.attachCount += attachedSpans.size();

        var attachedLinks = annotationService.getAttachedLinks(layer, aFS);
        var attachedToReadOnlyLinks = attachedLinks.stream()
                .anyMatch(rel -> rel.getLayer().isReadonly());
        if (attachedToReadOnlyLinks) {
            attachStatus.readOnlyAttached |= true;
        }
        attachStatus.attachCount += attachedLinks.size();

        return attachStatus;
    }
}
