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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarService;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class CurationRenderer
    implements RenderStep
{
    public static final String ID = "CurationRenderer";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CurationSidebarService curationService;
    private final LayerSupportRegistry layerSupportRegistry;
    private final DocumentService documentService;
    private final UserDao userRepository;
    private final AnnotationSchemaService annotationService;

    public CurationRenderer(CurationSidebarService aCurationService,
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
    public boolean accepts(RenderRequest aContext)
    {
        return aContext.getState().getMode() == Mode.ANNOTATION;
    }

    @Override
    public void render(VDocument aVdoc, RenderRequest aRequest)
    {
        String currentUsername = userRepository.getCurrentUsername();
        List<User> selectedUsers = curationService.listUsersReadyForCuration(currentUsername,
                aRequest.getProject(), aRequest.getSourceDocument());

        if (selectedUsers.isEmpty()) {
            return;
        }

        Map<String, CAS> casses = new LinkedHashMap<>();

        // This is the CAS that the user can actively edit
        casses.put(aRequest.getAnnotationUser().getUsername(), aRequest.getCas());

        for (User user : selectedUsers) {
            try {
                CAS userCas = documentService.readAnnotationCas(aRequest.getSourceDocument(),
                        user.getUsername());
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                log.error("Could not retrieve CAS for user [{}] and project {}", user.getUsername(),
                        aRequest.getProject(), e);
            }
        }

        List<DiffAdapter> adapters = getDiffAdapters(annotationService,
                aRequest.getVisibleLayers());
        CasDiff casDiff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses,
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());
        DiffResult diff = casDiff.toResult();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        List<AnnotationFeature> supportedFeatures = annotationService
                .listSupportedFeatures(aRequest.getProject());
        List<AnnotationFeature> allFeatures = annotationService
                .listAnnotationFeature(aRequest.getProject());

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        Map<String, AnnotationLayer> type2layer = diff.getPositions().stream()
                .map(Position::getType).distinct()
                .map(type -> annotationService.findLayer(aRequest.getProject(), type))
                .collect(toMap(AnnotationLayer::getName, identity()));

        Set<VID> generatedCurationVids = new HashSet<>();
        boolean showAll = curationService.isShowAll(currentUsername, aRequest.getProject().getId());
        String curationTarget = curationService.retrieveCurationTarget(currentUsername,
                aRequest.getProject().getId());
        for (ConfigurationSet cfgSet : diff.getConfigurationSets()) {
            if (!showAll && cfgSet.getCasGroupIds().contains(curationTarget)) {
                // Hide configuration sets where the curator has already curated (likely)
                continue;
            }

            AnnotationLayer layer = type2layer.get(cfgSet.getPosition().getType());

            List<AnnotationFeature> layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());
            List<AnnotationFeature> layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());

            for (Configuration cfg : cfgSet.getConfigurations()) {
                FeatureStructure fs = cfg.getRepresentative(casDiff.getCasMap());
                String user = cfg.getRepresentativeCasGroupId();

                // We need to pass in *all* the annotation features here because we also to that in
                // other places where we create renderers - and the set of features must always be
                // the same because otherwise the IDs of armed slots would be inconsistent
                Renderer renderer = layerSupportRegistry.getLayerSupport(layer) //
                        .createRenderer(layer, () -> layerAllFeatures);

                List<VObject> objects = renderer.render(aVdoc, (AnnotationFS) fs,
                        layerSupportedFeatures, aRequest.getWindowBeginOffset(),
                        aRequest.getWindowEndOffset());

                for (VObject object : objects) {
                    VID curationVid = new CurationVID(user, object.getVid());
                    if (generatedCurationVids.contains(curationVid)) {
                        continue;
                    }

                    generatedCurationVids.add(curationVid);

                    object.setVid(curationVid);
                    object.setColorHint("#ccccff");
                    aVdoc.add(new VComment(object.getVid(), VCommentType.INFO,
                            "Users with this annotation:\n" + cfg.getCasGroupIds().stream()
                                    .collect(Collectors.joining(", "))));

                    if (object instanceof VArc) {
                        VArc arc = (VArc) object;
                        // Currently works for relations but not for slots
                        arc.setSource(getCurationVid(aRequest.getAnnotationUser(), diff, cfg,
                                arc.getSource()));
                        arc.setTarget(getCurationVid(aRequest.getAnnotationUser(), diff, cfg,
                                arc.getTarget()));
                        log.trace("Rendering curation vid: {} source: {} target: {}", arc.getVid(),
                                arc.getSource(), arc.getTarget());
                    }
                    else {
                        log.trace("Rendering curation vid: {}", object.getVid());
                    }

                    aVdoc.add(object);
                }
            }
        }
    }

    /**
     * Find and return the rendered VID which is equivalent to the given VID. E.g. if the given VID
     * belongs to an already curated annotation, then locate the VID for the rendered annotation of
     * the curation user by searching the diff for configuration set that contains the annotators
     * annotation and then switching over to the configuration containing the curators annotation.
     */
    private VID getCurationVid(User aAnnotator, DiffResult aDiff, Configuration aCfg, VID aVid)
    {
        Optional<Configuration> sourceConfiguration = aDiff
                .findConfiguration(aCfg.getRepresentativeCasGroupId(), new AID(aVid.getId()));
        if (sourceConfiguration.isPresent()) {
            AID curatedAID = sourceConfiguration.get().getAID(aAnnotator.getUsername());
            if (curatedAID != null) {
                return new VID(curatedAID.addr);
            }
        }

        return new CurationVID(aCfg.getRepresentativeCasGroupId(), aVid);
    }
}
