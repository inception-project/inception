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
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;

public class CurationSidebarBehavior
    extends Behavior
{
    private static final long serialVersionUID = -6224298395673360592L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationSessionService curationSessionService;
    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;
    private @SpringBean PreferencesService preferencesService;

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
        var source = aEvent.getSource();

        if (!(source instanceof CurationPage page)) {
            LOG.trace(
                    "Curation sidebar is not deployed on CurationPage but rather [{}] - ignoring event [{}]",
                    source.getClass(), aEvent.getClass());
            return;
        }

        var sessionOwner = userService.getCurrentUsername();
        var doc = aEvent.getDocument();
        var project = doc.getProject();

        var dataOwner = aEvent.getDocumentOwner();

        if (!projectService.hasRole(sessionOwner, project, CURATOR)) {
            LOG.trace("Session owner [{}] is not a curator - ignoring event", sessionOwner);
            return;
        }

        LOG.trace("Curation sidebar reacting to [{}]@{} being opened by [{}]", dataOwner, doc,
                sessionOwner);

        var prefs = preferencesService
                .loadDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS, project);
        if (prefs.isAutoMergeCurationSidebar()) {
            if (CURATION_USER.equals(dataOwner)) {
                autoMerge(aEvent, page);
            }
        }
    }

    private void autoMerge(PreparingToOpenDocumentEvent aEvent, CurationPage page)
    {
        var sessionOwner = userService.getCurrentUsername();
        var doc = aEvent.getDocument();
        var project = doc.getProject();

        var editable = documentAccess.canEditAnnotationDocument(sessionOwner,
                String.valueOf(project.getId()), doc.getId(), aEvent.getDocumentOwner());
        if (!editable || !curationDocumentService.isInitialMergeRequired(doc)) {
            return;
        }

        try {
            var editorState = page.getWorkspace().getActiveEditor() //
                    .map(DiamContext::getAnnotatorState) //
                    .orElse(null);

            if (editorState == null) {
                LOG.trace("No editor to merge into - skipping auto-merge");
                return;
            }

            // We need to force upgrade the editor CAS here already so the merge can succeed
            // The annotation page will do this again in the actionLoadDocument, but I don't
            // currently see a good way to avoid this duplication. At least we only do it twice
            // if an initial merge is required.
            documentService.readAnnotationCas(doc, AnnotationSet.forUser(editorState.getUser()),
                    FORCE_CAS_UPGRADE);
            var selectedDataOwners = curationSessionService
                    .listDataOwnersReadyForCuration(sessionOwner, project, doc);

            var workflow = curationService.readOrCreateCurationWorkflow(project);
            var mergeStrategyFactory = curationSidebarService.merge(editorState, workflow,
                    selectedDataOwners, true);

            page.success(
                    "Performed initial merge using [" + mergeStrategyFactory.getLabel() + "].");
            aEvent.getRequestTarget().ifPresent($ -> $.addChildren(page, IFeedback.class));
        }
        catch (Exception e) {
            // This exception needs to go to AnnotationPageBase2.actionLoadDocument and abort
            // the loading of the document before the state transition happens
            throw new RuntimeException("Unable to perform initial merge for [" + doc.getName()
                    + "]: " + getRootCauseMessage(e), e);
        }
    }
}
