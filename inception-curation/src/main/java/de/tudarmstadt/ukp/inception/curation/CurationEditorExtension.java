/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.curation;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component(CurationEditorExtension.EXTENSION_ID)
public class CurationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String EXTENSION_ID = "curationEditorExtension";
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired CurationService curationService;
    private @Autowired PreRenderer preRenderer;
    
    @Override
    public String getBeanName()
    {
        return EXTENSION_ID;
    }

    @Override
    public void handleAction(AnnotationActionHandler aPanel, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aParamId, String aAction, int aBegin, int aEnd)
        throws AnnotationException, IOException
    {
        // only process actions relevant to curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }
        
        VID extendedVID = parse(aParamId);
        // Annotation has been selected for gold
        if (SpanAnnotationResponse.is(aAction)) { //TODO is this action only for spans
                                                  //, what about relations ?
            // TODO: store annotation in user CAS
        }
        
    }

    /**
     * Parse extension payload of given VID into CurationVID
     */
    protected VID parse(VID aParamId)
    {
        // format of extension payload is <USER>:<VID> with standard VID format
        // <ID>-<SUB>.<ATTR>.<SLOT>@<LAYER>
        Matcher matcher = Pattern.compile("(?:(?<USER>\\w+)\\:)" 
                + "(?<VID>.+)").matcher(aParamId.getExtensionPayload());
        if (!matcher.matches()) {
            return aParamId;
        }
        
        if (matcher.group("VID") == null || 
                matcher.group("USER") == null ) {
            return aParamId;
        }
        
        String vidStr = matcher.group("VID");
        String username = matcher.group("USER");
        return new CurationVID(aParamId.getExtensionId(), aParamId.getExtensionPayload(), username, 
                VID.parse(vidStr));
    }

    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        String currentUser = aState.getUser().getUsername();
        long projectId = aState.getProject().getId();
        Optional<List<User>> selectedUsers = curationService
                .listUsersSelectedForCuration(currentUser, projectId);
        if (!selectedUsers.isPresent()) {
            return;
        }

        for (User user : selectedUsers.get()) {
            try {
                Optional<CAS> userCas = curationService.retrieveCurationCAS(user.getUsername(),
                        projectId, aState.getDocument());
                if (!userCas.isPresent()) {
                    log.error(String.format("Could not retrieve CAS for user %s and project %d",
                            user.getUsername(), projectId));
                    continue;
                }
                VDocument tmpDoc = new VDocument();
                preRenderer.render(tmpDoc, aWindowBeginOffset, aWindowEndOffset, userCas.get(),
                        aState.getAnnotationLayers()); //TODO: might need to filter the layers?
                // copy all arcs and spans to existing doc with new VID
                for (VObject vobj : tmpDoc.vobjects()) {
                    VID vid = vobj.getVid();
                    VID extendedVID = parse(vid);
                    vobj.setVid(extendedVID);
                    aVdoc.add(vobj);
                }
                // TODO: add comment with username
            }
            catch (IOException e) {
                log.error(String.format("Could not retrieve CAS for user %s and project %d",
                        user.getUsername(), projectId));
                e.printStackTrace();
            }

        }
    }
}
