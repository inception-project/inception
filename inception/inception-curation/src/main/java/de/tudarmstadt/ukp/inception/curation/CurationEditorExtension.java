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
package de.tudarmstadt.ukp.inception.curation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.curation.CurationMetadata.CURATION_USER_PROJECT;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMergeOperationResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component(CurationEditorExtension.EXTENSION_ID)
public class CurationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String EXTENSION_ID = "curationEditorExtension";

    // actions from the ui when selecting span or arc annotation
    private static final String ACTION_SELECT_ARC = "arcOpenDialog";
    private static final String ACTION_SELECT_SPAN = "spanOpenDialog";

    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired CurationService curationService;
    private @Autowired PreRenderer preRenderer;
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userRepository;
    private @Autowired LayerSupportRegistry layerSupportRegistry;

    @Override
    public String getBeanName()
    {
        return EXTENSION_ID;
    }

    @Override
    public void handleAction(AnnotationActionHandler aPanel, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aParamId, String aAction)
        throws AnnotationException, IOException
    {
        // only process actions relevant to curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }

        if (!aAction.equals(ACTION_SELECT_ARC) && !aAction.equals(ACTION_SELECT_SPAN)) {
            return;
        }

        // Annotation has been selected for gold
        CurationVID extendedVID = CurationVID.parse(aParamId.getExtensionPayload());
        if (extendedVID != null) {
            saveAnnotation(aAction, aPanel, aState, aTarget, aCas, extendedVID);
        }
    }

    /**
     * Save annotation identified by aVID from user CAS to given curator's CAS
     */
    private void saveAnnotation(String aAction, AnnotationActionHandler aPanel,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aTargetCas,
            CurationVID aCurationVid)
        throws IOException, AnnotationException
    {
        // get user CAS and annotation (to be merged into curator's)
        SourceDocument doc = aState.getDocument();
        String srcUser = aCurationVid.getUsername();

        if (!documentService.existsAnnotationDocument(doc, srcUser)) {
            log.error(String.format("Source CAS of %s for curation not found", srcUser));
            return;
        }

        VID vid = VID.parse(aCurationVid.getExtensionPayload());

        AnnotationLayer layer = annotationService.getLayer(vid.getLayerId());

        CAS srcCas = documentService.readAnnotationCas(doc, srcUser);
        AnnotationFS sourceAnnotation = selectAnnotationByAddr(srcCas, vid.getId());

        // merge into curator's CAS depending on annotation type (span or arc)
        CasMerge casMerge = new CasMerge(annotationService);
        CasMergeOperationResult mergeResult;
        if (ACTION_SELECT_SPAN.equals(aAction.toString())) {
            mergeResult = casMerge.mergeSpanAnnotation(doc, srcUser, layer, aTargetCas,
                    sourceAnnotation, layer.isAllowStacking());

            // open created/updates FS in annotation detail editorpanel
            AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                    mergeResult.getResultFSAddress());
            aState.getSelection().selectSpan(mergedAnno);
        }
        else if (ACTION_SELECT_ARC.equals(aAction.toString())) {
            // this is a slot arc
            if (vid.isSlotSet()) {
                TypeAdapter adapter = annotationService.getAdapter(layer);
                AnnotationFeature feature = adapter.listFeatures().stream().sequential()
                        .skip(vid.getAttribute()).findFirst().get();

                mergeResult = casMerge.mergeSlotFeature(doc, srcUser, layer, aTargetCas,
                        sourceAnnotation, feature.getName(), vid.getSlot());

                // open created/updates FS in annotation detail editorpanel
                AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                        mergeResult.getResultFSAddress());
                aState.getSelection().selectSpan(mergedAnno);
            }
            // normal relation annotation arc is clicked
            else {
                mergeResult = casMerge.mergeRelationAnnotation(doc, srcUser, layer, aTargetCas,
                        sourceAnnotation, layer.isAllowStacking());

                // open created/updates FS in annotation detail editorpanel
                AnnotationFS mergedAnno = selectAnnotationByAddr(aTargetCas,
                        mergeResult.getResultFSAddress());
                aState.getSelection().selectArc(mergedAnno);
            }
        }

        aPanel.actionSelect(aTarget);
        aPanel.actionCreateOrUpdate(aTarget, aTargetCas); // should also update timestamps
    }

    public void render2(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        List<DiffAdapter> adapters = getDiffAdapters(annotationService,
                aState.getAnnotationLayers());

        String currentUsername = userRepository.getCurrentUsername();
        List<User> selectedUsers = curationService.listUsersReadyForCuration(currentUsername,
                aState.getProject(), aState.getDocument());

        if (selectedUsers.isEmpty()) {
            return;
        }

        Map<String, CAS> casses = new HashMap<>();

        // This is the CAS that the user can actively edit
        casses.put(aState.getUser().getUsername(), aCas);

        for (User user : selectedUsers) {
            try {
                CAS userCas = documentService.readAnnotationCas(aState.getDocument(),
                        user.getUsername());
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                log.error("Could not retrieve CAS for user [{}] and project [{}]({})",
                        user.getUsername(), aState.getProject().getName(),
                        aState.getProject().getId(), e);
            }
        }

        CasDiff casDiff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses, aWindowBeginOffset,
                aWindowEndOffset);
        DiffResult diff = casDiff.toResult();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        List<AnnotationFeature> supportedFeatures = annotationService
                .listSupportedFeatures(aState.getProject());
        List<AnnotationFeature> allFeatures = annotationService
                .listAnnotationFeature(aState.getProject());

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        Map<String, AnnotationLayer> type2layer = diff.getPositions().stream()
                .map(Position::getType).distinct()
                .map(type -> annotationService.findLayer(aState.getProject(), type))
                .collect(toMap(AnnotationLayer::getName, identity()));

        for (ConfigurationSet cfgSet : diff.getConfigurationSets()) {
            AnnotationLayer layer = type2layer.get(cfgSet.getPosition().getType());

            List<AnnotationFeature> layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());
            List<AnnotationFeature> layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .collect(toList());

            for (Configuration cfg : cfgSet.getConfigurations()) {
                FeatureStructure fs = cfg.getRepresentative(casDiff.getCasMap());

                // We need to pass in *all* the annotation features here because we also to that in
                // other places where we create renderers - and the set of features must always be
                // the same because otherwise the IDs of armed slots would be inconsistent
                Renderer renderer = layerSupportRegistry.getLayerSupport(layer) //
                        .createRenderer(layer, () -> layerAllFeatures);
            }
        }
    }

    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        if (!aState.getMode().equals(ANNOTATION)) {
            return;
        }

        // Check annotator state metadata if user is currently curating for this project
        long projectId = aState.getProject().getId();
        Boolean isCurating = aState.getMetaData(CURATION_USER_PROJECT);
        if (isCurating == null || !isCurating) {
            return;
        }

        // check if user already finished with this document
        String currentUsername = userRepository.getCurrentUsername();
        if (curationService.isCurationFinished(aState, currentUsername)) {
            return;
        }

        List<User> selectedUsers = curationService.listUsersReadyForCuration(currentUsername,
                aState.getProject(), aState.getDocument());
        if (selectedUsers.isEmpty()) {
            return;
        }

        for (User user : selectedUsers) {
            try {
                CAS userCas = documentService.readAnnotationCas(aState.getDocument(),
                        user.getUsername());

                VDocument tmpDoc = new VDocument();
                preRenderer.render(tmpDoc, aWindowBeginOffset, aWindowEndOffset, userCas,
                        aState.getAnnotationLayers());

                String username = user.getUsername();
                String color = "#ccccff"; // "#cccccc" is the color for recommendations

                // copy all arcs and spans to existing doc with new VID

                // copy all spans and add to map as possible varc dependents
                // spans with new vids identified by their old vid for lookup in varcs
                Map<VID, VSpan> newIdSpan = new HashMap<>();
                for (VSpan vspan : tmpDoc.spans()) {
                    VID aDepVID = vspan.getVid();
                    VID prevVID = VID.copyVID(aDepVID);
                    VID newVID = new CurationVID(username,
                            new VID(vspan.getLayer().getId(), aDepVID.getId(), aDepVID.getSubId(),
                                    aDepVID.getAttribute(), aDepVID.getSlot()));
                    vspan.setVid(newVID);
                    vspan.setColorHint(color);
                    // TODO: might be better to change after bugfix #1389
                    vspan.setLazyDetails(Collections.emptyList());
                    newIdSpan.put(prevVID, vspan);
                    // set user name as comment
                    aVdoc.add(new VComment(newVID, VCommentType.INFO, username));
                    aVdoc.add(vspan);
                }

                // copy arcs to VDoc
                for (VArc varc : tmpDoc.arcs()) {
                    // update varc vid
                    VID vid = varc.getVid();
                    VID extendedVID = new CurationVID(username, new VID(varc.getLayer().getId(),
                            vid.getId(), vid.getSubId(), vid.getAttribute(), vid.getSlot()));
                    // set target and src with new vids for arc
                    VSpan targetSpan = newIdSpan.get(varc.getTarget());
                    VSpan srcSpan = newIdSpan.get(varc.getSource());
                    VArc newVarc = new VArc(varc.getLayer(), extendedVID, varc.getType(),
                            srcSpan.getVid(), targetSpan.getVid(), varc.getLabelHint(),
                            varc.getFeatures(), color);
                    // set user name as comment
                    aVdoc.add(new VComment(extendedVID, VCommentType.INFO, username));
                    aVdoc.add(newVarc);
                }
            }
            catch (IOException e) {
                log.error("Could not retrieve CAS for user [{}] and project [{}]({})",
                        user.getUsername(), aState.getProject().getName(),
                        aState.getProject().getId(), e);
            }
        }
    }
}
