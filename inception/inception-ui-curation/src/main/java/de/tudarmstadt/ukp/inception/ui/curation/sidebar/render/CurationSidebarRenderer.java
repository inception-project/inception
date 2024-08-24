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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.render;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.STACKING_ONLY;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarService;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationSidebarRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class CurationSidebarRenderer
    implements RenderStep
{
    public static final String ID = "CurationRenderer";

    private static final String COLOR = "#ccccff";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CurationSidebarService curationService;
    private final LayerSupportRegistry layerSupportRegistry;
    private final DocumentService documentService;
    private final UserDao userRepository;
    private final AnnotationSchemaService annotationService;

    public CurationSidebarRenderer(CurationSidebarService aCurationService,
            LayerSupportRegistry aLayerSupportRegistry, DocumentService aDocumentService,
            UserDao aUserRepository, AnnotationSchemaService aAnnotationService)
    {
        curationService = aCurationService;
        layerSupportRegistry = aLayerSupportRegistry;
        documentService = aDocumentService;
        userRepository = aUserRepository;
        annotationService = aAnnotationService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        var project = aRequest.getProject();
        var state = aRequest.getState();

        // do not show predictions on the decicated curation page
        if (state != null && state.getMode() != ANNOTATION) {
            return false;
        }

        if (aRequest.getCas() == null) {
            return false;
        }

        var sessionOwner = userRepository.getCurrentUsername();
        if (!curationService.existsSession(sessionOwner, project.getId())) {
            return false;
        }

        return true;
    }

    @Override
    public void render(VDocument aVdoc, RenderRequest aRequest)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var project = aRequest.getProject();

        var selectedUsers = curationService.listUsersReadyForCuration(sessionOwner, project,
                aRequest.getSourceDocument());
        if (selectedUsers.isEmpty()) {
            return;
        }

        var targetUser = aRequest.getAnnotationUser().getUsername();

        var casDiff = createDiff(aRequest, selectedUsers);
        var diff = casDiff.toResult();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        var supportedFeatures = annotationService.listSupportedFeatures(project);
        var allFeatures = annotationService.listAnnotationFeature(project);

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        var type2layer = aRequest.getAllLayers().stream() //
                .collect(toMap(AnnotationLayer::getName, identity()));

        var generatedCurationVids = new HashSet<VID>();
        var showAll = curationService.isShowAll(sessionOwner, project.getId());
        var curationTarget = aRequest.getAnnotationUser().getUsername();
        for (var cfgSet : diff.getConfigurationSets()) {
            LOG.trace("Processing set: {}", cfgSet);

            if (!showAll && cfgSet.getCasGroupIds().contains(curationTarget)) {
                // Hide configuration sets where the curator has already curated (likely)
                continue;
            }

            var layer = type2layer.get(cfgSet.getPosition().getType());

            var layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .toList();
            var layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .toList();

            var noOverlap = layer.getOverlapMode() == NO_OVERLAP
                    || layer.getOverlapMode() == STACKING_ONLY;

            for (var cfg : cfgSet.getConfigurations()) {
                LOG.trace("Processing configuration: {}", cfg);
                var fs = cfg.getRepresentative(casDiff.getCasMap());
                if (!(fs instanceof Annotation)) {
                    continue;
                }

                // Do not render configurations that belong only the the target user. Those are
                // already rendered by the normal annotation rendering mechanism
                if (cfg.getCasGroupIds().size() == 1 && cfg.getCasGroupIds().contains(targetUser)) {
                    LOG.trace(
                            "{} - skipping rendering because configuration induced only by target user",
                            cfg.getPosition());
                    continue;
                }

                var user = cfg.getRepresentativeCasGroupId();

                // We need to pass in *all* the annotation features here because we also to that in
                // other places where we create renderers - and the set of features must always be
                // the same because otherwise the IDs of armed slots would be inconsistent
                var layerSupport = layerSupportRegistry.getLayerSupport(layer);
                var renderer = layerSupport.createRenderer(layer, () -> layerAllFeatures);

                var objects = renderer.render(aRequest, layerSupportedFeatures, aVdoc,
                        (AnnotationFS) fs);

                for (var object : objects) {
                    if (!showAll && shouldBeHidden(aRequest, diff, cfg, fs, object, targetUser,
                            noOverlap)) {
                        continue;
                    }

                    // Check if the object has already been rendered
                    var curationVid = new CurationVID(user, object.getVid());
                    if (generatedCurationVids.contains(curationVid)) {
                        continue;
                    }

                    generatedCurationVids.add(curationVid);

                    object.setVid(curationVid);
                    object.setColorHint(COLOR);
                    aVdoc.add(object);

                    aVdoc.add(new VComment(object.getVid(), VCommentType.INFO,
                            "Annotators: " + cfg.getCasGroupIds().stream()
                                    .filter(a -> !targetUser.equals(a)).collect(joining(", "))));

                    if (object instanceof VArc arc) {
                        resolveArcEndpoints(targetUser, diff, showAll, cfg, arc);
                        LOG.trace("Rendered arc: {} source: [{}] target: [{}]", arc,
                                arc.getSource(), arc.getTarget());
                    }
                    else {
                        LOG.trace("Rendered span: {}", object);
                    }
                }
            }
        }
    }

    private boolean shouldBeHidden(RenderRequest aRequest, DiffResult diff, Configuration cfg,
            FeatureStructure fs, VObject object, String targetUser, boolean noOverlap)
    {
        // if (cfg.getPosition() instanceof SpanPosition spanPosition) {
        // if (spanPosition.isLinkFeaturePosition() && object instanceof VSpan) {
        // // When processing a slot position, do not render the origin span because that
        // // should be already when we encounter the base span position
        // LOG.trace("{} - skipping rendering of link origin on link position", object);
        // return true;
        // }
        //
        // if (!spanPosition.isLinkFeaturePosition() && object instanceof VArc) {
        // // When processing a span position, do not render slot links because these should be
        // // rendered when we encounter the slot position
        // LOG.trace("{} - skipping rendering of link arc on span position", object);
        // return true;
        // }
        // }

        // // Check if the target already contains an annotation from this configuration
        // if (object instanceof VSpan
        // && isCurationSuggestionHiddenByMergedAnnotation(targetUser, diff, cfg, object)) {
        // LOG.trace("{} - skipping rendering due to merged annotation", object);
        // return true;
        // }

        // Check if the position could be merged at all or if the merge is blocked by an overlapping
        // annotation (which may not be contained in the configuration)
        if (object instanceof VSpan && noOverlap && fs instanceof Annotation ann) {
            var cas = aRequest.getCas();
            if (cas.<Annotation> select(ann.getType().getName())
                    .anyMatch(f -> ann.overlapping(f))) {
                LOG.trace("{} - skipping rendering due to overlap", object);
                return true;
            }
        }

        return false;
    }

    private boolean isCurationSuggestionHiddenByMergedAnnotation(String aTargetUser,
            DiffResult aDiff, Configuration aCfg, VObject aObject)
    {
        var sourceConfiguration = aDiff.findConfiguration(aCfg.getRepresentativeCasGroupId(),
                new AID(aObject.getVid()));
        return sourceConfiguration.map($ -> $.getAID(aTargetUser) != null).orElse(false);
    }

    private CasDiff createDiff(RenderRequest aRequest, List<User> selectedUsers)
    {
        var casses = new LinkedHashMap<String, CAS>();

        // This is the CAS that the user can actively edit
        casses.put(aRequest.getAnnotationUser().getUsername(), aRequest.getCas());

        for (var user : selectedUsers) {
            try {
                var userCas = documentService.readAnnotationCas(aRequest.getSourceDocument(),
                        user.getUsername());
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                LOG.error("Could not retrieve CAS for user [{}] and project {}", user.getUsername(),
                        aRequest.getProject(), e);
            }
        }

        var adapters = getDiffAdapters(annotationService, aRequest.getVisibleLayers());
        return doDiff(adapters, casses, aRequest.getWindowBeginOffset(),
                aRequest.getWindowEndOffset());
    }

    private void resolveArcEndpoints(String targetUser, DiffResult diff, boolean showAll,
            Configuration cfg, VArc arc)
    {
        if (showAll) {
            var representativeCasGroupId = cfg.getRepresentativeCasGroupId();
            arc.setSource(new CurationVID(representativeCasGroupId, arc.getSource()));
            arc.setTarget(resolveVisibleEndpoint(targetUser, diff, cfg, arc.getTarget()));
            return;
        }

        if (cfg.getPosition() instanceof SpanPosition spanPosition
                && spanPosition.isLinkFeaturePosition()) {
            arc.setSource(resolveVisibleLinkHost(targetUser, diff, cfg, arc.getSource()));
        }
        else {
            arc.setSource(resolveVisibleEndpoint(targetUser, diff, cfg, arc.getSource()));
        }
        arc.setTarget(resolveVisibleEndpoint(targetUser, diff, cfg, arc.getTarget()));
    }

    private VID resolveVisibleLinkHost(String aTargetUser, DiffResult aDiff, Configuration aCfg,
            VID aVid)
    {
        var representativeCasGroupId = aCfg.getRepresentativeCasGroupId();

        // If this is a link feature position, derive the base span position (i.e. the span
        // which owns the link feature) and check if that has already been merged. If yes, we
        // need to return the merged position instead of the curator's position.
        if (false && aCfg.getPosition() instanceof SpanPosition spanPosition
                && spanPosition.isLinkFeaturePosition()) {
            var originalSpanPosition = spanPosition.getBasePosition();
            var cfgSet = aDiff.getConfigurationSet(originalSpanPosition);
            if (cfgSet != null) {
                var targetConfigurations = cfgSet.getConfigurations(aTargetUser);
                if (!targetConfigurations.isEmpty()) {
                    // FIXME: This is probably sub-optimal. What if the target user has multiple
                    // configurations at this position? Currently, we simply attach to the first
                    // one - which may not be the best matching one.
                    // In particular, it may not be the one onto which the link will eventually
                    // be merged...
                    var curatedAID = targetConfigurations.get(0).getAID(aTargetUser);
                    if (curatedAID != null) {
                        return new VID(curatedAID.addr);
                    }
                }
            }
        }

        return new CurationVID(representativeCasGroupId, aVid);
    }

    /**
     * Find and return the rendered VID which is equivalent to the given VID. E.g. if the given VID
     * belongs to an already curated annotation, then locate the VID for the rendered annotation of
     * the curation user by searching the diff for configuration set that contains the annotators
     * annotation and then switching over to the configuration containing the curators annotation.
     */
    private VID resolveVisibleEndpoint(String aTargetUser, DiffResult aDiff, Configuration aCfg,
            VID aVid)
    {
        var representativeCasGroupId = aCfg.getRepresentativeCasGroupId();

        var sourceConfiguration = aDiff.findConfiguration(representativeCasGroupId, new AID(aVid));

        // This is for relation annotation endpoints and link targets. Here we can look for the
        // configuration which contains the annotators annotation and find the corresponding
        // curated version for it in the same configuration
        if (sourceConfiguration.isPresent()) {
            var curatedAID = sourceConfiguration.get().getAID(aTargetUser);
            if (curatedAID != null) {
                return new VID(curatedAID.addr);
            }

            return new CurationVID(representativeCasGroupId,
                    new VID(sourceConfiguration.get().getAID(representativeCasGroupId).addr));
        }

        return new CurationVID(representativeCasGroupId, aVid);
    }
}
