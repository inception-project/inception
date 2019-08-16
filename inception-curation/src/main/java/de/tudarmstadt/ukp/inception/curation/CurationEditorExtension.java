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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
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
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired DocumentService documentService;
    
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
            saveAnnotation(aPanel, aState, aTarget, aCas, extendedVID, aBegin, aEnd);
        }
        
    }

    /**
     * Save annotation identified by aVID from given CAS in curator's CAS
     * @throws AnnotationException 
     * @throws IOException 
     */
    private void saveAnnotation(AnnotationActionHandler aPanel, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aVID, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        // TODO look at webanno suggestionviewpanel onclientevent -> mergeSpan etc., then save in CAS
        
        // get curator's CAS
//        SourceDocument doc = aState.getDocument();
//        Optional<CAS> curatorCAS = Optional.empty();
//        // FIXME: aCas should already be curation CAS if it was selected (and opened)
//        curatorCAS = curationService.retrieveCurationCAS(aState.getUser().getUsername(),
//                aState.getProject().getId(), doc);
//
//        if (!curatorCAS.isPresent()) {
//            log.error(
//                    String.format("Curator CAS for %s not found", aState.getUser().getUsername()));
//            return;
//        }
//        // get user CAS
//        CAS srcCAS = documentService.readAnnotationCas(doc, ((CurationVID) aVID).getUsername());
//        
//        // create/update anno in curator CAS
//        AnnotationFS fs = selectByAddr(srcCAS, AnnotationFS.class, aVID.getId());
//        CAS destCAS = curatorCAS.get();
//        CasCopier copier = new CasCopier(srcCAS, destCAS);
//        FeatureStructure curatedFs = copier.copyFs(fs);
//        destCAS.addFsToIndexes(curatedFs);
//        int address = WebAnnoCasUtil.getAddr(curatedFs);
//        
//        // Set selection to the accepted annotation and select it and load it into the detail editor
//        // panel
//        aState.getSelection().selectSpan(new VID(address), destCAS, aBegin, aEnd);
//        aPanel.actionSelect(aTarget, destCAS);            
//        aPanel.actionCreateOrUpdate(aTarget, destCAS);
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
                        aState.getAnnotationLayers());
                // copy all arcs and spans to existing doc with new VID
                String username = user.getUsername();
                String color = "#cccccc"; //this is the same color as for recommendations
                for (VObject vobj : tmpDoc.vobjects()) {
                    VID vid = vobj.getVid();
                    VID extendedVID = new CurationVID(EXTENSION_ID, username + ":" + vid.toString(),
                            username, vid);
                    vobj.setVid(extendedVID);
                    aVdoc.add(vobj);
                    // change color for other users' annos
                    if (vobj instanceof VSpan) {
                        ((VSpan) vobj).setColor(color);
                    }
                    else if (vobj instanceof VArc) {
                        ((VArc) vobj).setColor(color);
                    }
                    // set user name as comment
                    aVdoc.add(new VComment(extendedVID, VCommentType.INFO, username));
                }
            }
            catch (IOException e) {
                log.error(String.format("Could not retrieve CAS for user %s and project %d",
                        user.getUsername(), projectId));
                e.printStackTrace();
            }

        }
    }
}
