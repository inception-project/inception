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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.setProjectPageParameter;
import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;

public class CurationSidebarBehavior
    extends Behavior
{
    private static final long serialVersionUID = -6224298395673360592L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String STAY = "stay";
    private static final String OFF = "off";
    private static final String ON = "on";

    private static final String PARAM_CURATION_SESSION = "curationSession";
    private static final String PARAM_CURATION_TARGET_OWN = "curationTargetOwn";

    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userService;

    @Override
    public void onConfigure(Component aComponent)
    {
        super.onConfigure(aComponent);

        var page = aComponent.getPage();
        if (!(page instanceof AnnotationPage)) {
            return;
        }

        var annotationPage = (AnnotationPage) page;

        if (annotationPage.getModelObject().getDocument() == null) {
            return;
        }

        handleCurationSessionPageParameters(annotationPage);

        handleWrongAnnotatorUserInState(annotationPage);
    }

    @Override
    public void onEvent(Component aComponent, IEvent<?> aEvent)
    {
        if (aEvent.getPayload() instanceof BeforeDocumentOpenedEvent) {
            var event = (BeforeDocumentOpenedEvent) aEvent.getPayload();
            var page = event.getRequestTarget().getPage();

            if (!(page instanceof AnnotationPage)) {
                return;
            }

            var annotationPage = (AnnotationPage) page;

            handleCurationSessionPageParameters(annotationPage);

            handleWrongAnnotatorUserInState(annotationPage);
        }
    }

    private void handleWrongAnnotatorUserInState(AnnotationPage aPage)
    {
        if (isViewingPotentialCurationTarget(aPage) && isSessionActive(aPage)) {
            var sessionOwner = userService.getCurrentUsername();
            var state = aPage.getModelObject();

            // If curation is possible and the curation target user is different from the user set
            // in the annotation state, then we need to update the state and reload.
            var curationTarget = curationSidebarService.getCurationTargetUser(sessionOwner,
                    state.getProject().getId());
            if (!state.getUser().equals(curationTarget)) {
                LOG.trace("Wrong user in state, setting and reloading");
                state.setUser(curationTarget);
                aPage.actionLoadDocument(null);
                RequestCycle.get().setResponsePage(aPage);
            }
        }
    }

    private void handleCurationSessionPageParameters(AnnotationPage aPage)
    {
        var params = aPage.getPageParameters();

        var curationSessionParameterValue = params.get(PARAM_CURATION_SESSION);
        var curationTargetOwnParameterValue = params.get(PARAM_CURATION_TARGET_OWN);
        var project = aPage.getModelObject().getProject();
        var sessionOwner = userService.getCurrentUsername();

        switch (curationSessionParameterValue.toString(STAY)) {
        case ON:
            LOG.trace("Checking if to start curation session");
            // Start a new session or switch to new curation target
            if (!isSessionActive(aPage) || !curationTargetOwnParameterValue.isEmpty()) {
                curationSidebarService.startSession(sessionOwner, project,
                        curationTargetOwnParameterValue.toBoolean(false));
            }
            break;
        case OFF:
            LOG.trace("Checking if to stop curation session");
            if (isSessionActive(aPage)) {
                curationSidebarService.closeSession(sessionOwner, project.getId());
            }
            break;
        default:
            // Ignore
        }

        if (!curationSessionParameterValue.isEmpty()) {
            LOG.trace("Reloading page without session parameters");
            params.remove(PARAM_CURATION_TARGET_OWN);
            params.remove(PARAM_CURATION_SESSION);
            setProjectPageParameter(params, project);
            params.set(AnnotationPage.PAGE_PARAM_DOCUMENT,
                    aPage.getModelObject().getDocument().getId());
            throw new RestartResponseException(aPage.getClass(), params);
        }
    }

    private boolean isViewingPotentialCurationTarget(AnnotationPage aPage)
    {
        // Curation sidebar is not allowed when viewing another users annotations
        var sessionOwner = userService.getCurrentUsername();
        var state = aPage.getModelObject();
        return asList(CURATION_USER, sessionOwner).contains(state.getUser().getUsername());
    }

    private boolean isSessionActive(AnnotationPage aPage)
    {
        var sessionOwner = userService.getCurrentUsername();
        var project = aPage.getModelObject().getProject();
        if (project != null
                && curationSidebarService.existsSession(sessionOwner, project.getId())) {
            return true;
        }

        return false;
    }
}
