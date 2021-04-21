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

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.curation.CurationMetadata.CURATION_USER_PROJECT;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
public class CurationRenderer
    implements RenderStep
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired CurationService curationService;
    private @Autowired PreRenderer preRenderer;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userRepository;

    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        if (!aState.getMode().equals(ANNOTATION)) {
            return;
        }

        // Check annotator state metadata if user is currently curating for this project
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
