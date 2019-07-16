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
        // only process actions relevant curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }
        // Annotation has been selected for gold
        if (SpanAnnotationResponse.is(aAction)) { //TODO is this action only for spans
            //, what about relations
            // TODO: store annotation in user CAS
        }
        
    }

    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVdoc, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        // TODO Auto-generated method stub
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
                // FIXME cannot add the same annotations, change VID ?
                preRenderer.render(aVdoc, aWindowBeginOffset, aWindowEndOffset, userCas.get(),
                        aState.getAnnotationLayers()); //TODO: might need to filter the layers
            }
            catch (IOException e) {
                log.error(String.format("Could not retrieve CAS for user %s and project %d",
                        user.getUsername(), projectId));
                e.printStackTrace();
            }

        }
    }

}
