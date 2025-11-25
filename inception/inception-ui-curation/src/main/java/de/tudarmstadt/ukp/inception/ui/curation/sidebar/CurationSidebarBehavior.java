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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.setProjectPageParameter;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil.handleException;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;

public class CurationSidebarBehavior
    extends Behavior
{
    private static final long serialVersionUID = -6224298395673360592L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String STAY = "stay";
    private static final String OFF = "off";
    private static final String ON = "on";

    private static final String PARAM_CURATION_SESSION = "curationSession";
    private static final String PARAM_CURATION_TARGET_OWN = "curationTargetOwn";

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean CurationSidebarProperties curationSidebarProperties;

    @Override
    public void onEvent(Component aComponent, IEvent<?> aEvent)
    {
        if (aEvent.getPayload() instanceof PreparingToOpenDocumentEvent prepOpenDocEvent) {
            onPreparingToOpenDocumentEvent(prepOpenDocEvent);
            return;
        }

        if (aEvent.getPayload() != null) {
            LOG.trace("Event not relevant to curation sidebar: {} / {}", aEvent.getClass(),
                    aEvent.getPayload().getClass());
        }
        else {
            LOG.trace("Event not relevant to curation sidebar: {}", aEvent.getClass());
        }
    }

    private void onPreparingToOpenDocumentEvent(PreparingToOpenDocumentEvent aEvent)
    {
        var page = aEvent.getSource();

        if (!(page instanceof AnnotationPage) && !(page instanceof CurationPage)) {
            LOG.trace(
                    "Curation sidebar is not deployed on AnnotationPage or CurationPage but "
                            + "rather [{}] - ignoring event [{}]",
                    page.getClass(), aEvent.getClass());
            return;
        }

        var sessionOwner = userService.getCurrentUsername();
        var doc = aEvent.getDocument();
        var project = doc.getProject();

        // If the curation sidebar is not enabled, then we can stop the curation session on the
        // annotation page to avoid rendering curation suggestions if the curation session has been
        // enabled due to visiting the new curation page
        if (page instanceof AnnotationPage && !curationSidebarProperties.isEnabled()) {
            curationSidebarService.closeSession(sessionOwner, project.getId());
            return;
        }

        var params = page.getPageParameters();
        var dataOwner = aEvent.getDocumentOwner();

        if (!projectService.hasRole(sessionOwner, project, CURATOR)) {
            LOG.trace(
                    "Session owner [{}] is not a curator and can therefore not manage curation mode using URL parameters",
                    sessionOwner);
            return;
        }

        LOG.trace("Curation sidebar reacting to [{}]@{} being opened by [{}]", dataOwner, doc,
                sessionOwner);

        handleSessionActivationPageParameters(page, params, doc, sessionOwner);

        ensureDataOwnerMatchesCurationTarget(page, project, sessionOwner, dataOwner);
        curationSidebarService.setDefaultSelectedUsersForDocument(aEvent.getSessionOwner(),
                aEvent.getDocument());

        var prefs = preferencesService
                .loadDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS, project);
        if (prefs.isAutoMergeCurationSidebar()) {
            if (userService.getCurationUser().equals(page.getModelObject().getUser())) {
                autoMerge(aEvent, page);
            }
        }
    }

    private void autoMerge(PreparingToOpenDocumentEvent aEvent, AnnotationPageBase page)
    {
        var sessionOwner = userService.getCurrentUsername();
        var doc = aEvent.getDocument();
        var project = doc.getProject();

        try {
            var editable = page.isEditable();
            if (!asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(doc.getState())
                    && editable) {
                var state = page.getModelObject();
                // We need to force upgrade the editor CAS here already so the merge can succeed
                // The annotation page will do this again in the actionLoadDocument, but I don't
                // currently see a good way to avoid this duplication. At least we only do it twice
                // if an initial merge is required.
                documentService.readAnnotationCas(state.getDocument(),
                        AnnotationSet.forUser(state.getUser()), FORCE_CAS_UPGRADE);
                var selectedUsers = curationSidebarService.getSelectedUsers(sessionOwner,
                        project.getId());

                var workflow = curationService.readOrCreateCurationWorkflow(state.getProject());
                var mergeStrategyFactory = curationSidebarService.merge(state, workflow,
                        state.getUser().getUsername(), selectedUsers, true);

                page.success(
                        "Performed initial merge using [" + mergeStrategyFactory.getLabel() + "].");
                aEvent.getRequestTarget().ifPresent($ -> $.addChildren(page, IFeedback.class));
            }
        }
        catch (Exception e) {
            handleException(LOG, page, e);
        }
    }

    private void ensureDataOwnerMatchesCurationTarget(AnnotationPageBase aPage, Project aProject,
            String aSessionOwner, String aDataOwner)
    {
        if (!isSessionActive(aProject)) {
            LOG.trace(
                    "No curation session active - no need to adjust data owner to curation target");
            return;
        }

        if (!isViewingPotentialCurationTarget(aDataOwner)) {
            return;
        }

        // If the curation target user is different from the data owner set in the annotation
        // state, then we need to update the state and reload.
        var curationTarget = curationSidebarService.getCurationTargetUser(aSessionOwner,
                aProject.getId());

        if (!aDataOwner.equals(curationTarget.getUsername())) {
            LOG.trace("Data owner [{}] should match curation target {} - changing to {}",
                    curationTarget, aDataOwner, curationTarget);
            aPage.getModelObject().setUser(curationTarget);
        }
        else {
            LOG.trace("Data owner [{}] alredy matches curation target {}", curationTarget,
                    aDataOwner);
        }
    }

    private void handleSessionActivationPageParameters(AnnotationPageBase aPage,
            PageParameters aParams, SourceDocument aDoc, String aSessionOwner)
    {
        var project = aDoc.getProject();

        var curationSessionParameterValue = aParams.get(PARAM_CURATION_SESSION);
        if (curationSessionParameterValue.isEmpty()) {
            return;
        }

        switch (curationSessionParameterValue.toString(STAY)) {
        case ON:
            LOG.trace("Checking if to start curation session");
            // Start a new session or switch to new curation target
            var curationTargetOwnParameterValue = aParams.get(PARAM_CURATION_TARGET_OWN);
            if (!isSessionActive(project) || !curationTargetOwnParameterValue.isEmpty()) {
                curationSidebarService.startSession(aSessionOwner, project,
                        curationTargetOwnParameterValue.toBoolean(false));
            }
            break;
        case OFF:
            LOG.trace("Checking if to stop curation session");
            if (isSessionActive(project)) {
                curationSidebarService.closeSession(aSessionOwner, project.getId());
            }
            break;
        default:
            // Ignore
            LOG.trace("No change in curation session state requested [{}]",
                    curationSessionParameterValue);
        }

        LOG.trace("Removing session control parameters and reloading (redirect)");
        aParams.remove(PARAM_CURATION_TARGET_OWN);
        aParams.remove(PARAM_CURATION_SESSION);
        setProjectPageParameter(aParams, project);
        aParams.set(AnnotationPage.PAGE_PARAM_DOCUMENT, aDoc.getId());
        // We need to do a redirect here to discard the arguments from the URL.
        // This also discards the page state.
        throw new RestartResponseException(aPage.getClass(), aParams);
    }

    private boolean isViewingPotentialCurationTarget(String aDataOwner)
    {
        // Curation sidebar is not allowed when viewing another users annotations
        var sessionOwner = userService.getCurrentUsername();
        var candidates = asList(CURATION_USER, sessionOwner);
        var result = candidates.contains(aDataOwner);
        if (!result) {
            LOG.trace("Data ownwer [{}] is not in curation candidates {}", aDataOwner, candidates);
        }
        return result;
    }

    private boolean isSessionActive(Project aProject)
    {
        var sessionOwner = userService.getCurrentUsername();
        return curationSidebarService.existsSession(sessionOwner, aProject.getId());
    }
}
