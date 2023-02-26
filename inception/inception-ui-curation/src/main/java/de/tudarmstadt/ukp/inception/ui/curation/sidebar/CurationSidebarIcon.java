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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketExceptionUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class CurationSidebarIcon
    extends Panel
{
    private static final long serialVersionUID = -1870047500327624860L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String STAY = "stay";
    private static final String OFF = "off";
    private static final String ON = "on";

    private static final String PARAM_CURATION_SESSION = "curationSession";
    private static final String PARAM_CURATION_TARGET_OWN = "curationTargetOwn";

    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;

    public CurationSidebarIcon(String aId, IModel<AnnotatorState> aState)
    {
        super(aId, aState);
        queue(new Icon("icon", FontAwesome5IconType.clipboard_s));
        queue(new Icon("badge", LoadableDetachableModel.of(this::getStateIcon))
                .add(new ClassAttributeModifier()
                {
                    private static final long serialVersionUID = 8029123921246115447L;

                    @Override
                    protected Set<String> update(Set<String> aClasses)
                    {
                        if (isSessionActive()) {
                            aClasses.add("text-primary");
                            aClasses.remove("text-muted");
                        }
                        else {
                            aClasses.add("text-muted");
                            aClasses.remove("text-primary");
                        }

                        return aClasses;
                    }
                }));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        handleCurationSessionPageParameters();

        if (isViewingPotentialCurationTarget() && isSessionActive()) {
            String sessionOwner = userService.getCurrentUsername();
            AnnotatorState state = getModelObject();

            // If curation is possible and the curation target user is different from the user set
            // in the annotation state, then we need to update the state and reload.
            User curationTarget = curationSidebarService.getCurationTargetUser(sessionOwner,
                    state.getProject().getId());
            if (!state.getUser().equals(curationTarget)) {
                state.setUser(curationTarget);
                try {
                    // Cannot use actionLoadDocument() here, but we still need to bump the timestamp
                    // to avoid a concurrent access error when switching curation targets
                    findParent(AnnotationPage.class).bumpAnnotationCasTimestamp(state);
                }
                catch (IOException e) {
                    WicketExceptionUtil.handleException(LOG, getPage(), e);
                }
                throw new RestartResponseException(getPage());
            }
        }
    }

    private void handleCurationSessionPageParameters()
    {
        var params = getPage().getPageParameters();
        var curationSessionParameterValue = params.get(PARAM_CURATION_SESSION);
        var curationTargetOwnParameterValue = params.get(PARAM_CURATION_TARGET_OWN);
        var project = getModelObject().getProject();
        String sessionOwner = userService.getCurrentUsername();

        switch (curationSessionParameterValue.toString(STAY)) {
        case ON:
            // Start a new session or switch to new curation target
            if (!isSessionActive() || !curationTargetOwnParameterValue.isEmpty()) {
                curationSidebarService.startSession(sessionOwner, project,
                        curationTargetOwnParameterValue.toBoolean(false));
            }
            break;
        case OFF:
            if (isSessionActive()) {
                curationSidebarService.closeSession(sessionOwner, project.getId());
            }
            break;
        default:
            // Ignore
        }

        if (!curationSessionParameterValue.isEmpty()) {
            params.remove(PARAM_CURATION_TARGET_OWN);
            params.remove(PARAM_CURATION_SESSION);
            setProjectPageParameter(params, project);
            params.set(AnnotationPage.PAGE_PARAM_DOCUMENT, getModelObject().getDocument().getId());
            throw new RestartResponseException(getPage().getClass(), params);
        }
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    private boolean isSessionActive()
    {
        var project = getModelObject().getProject();
        if (project != null && curationSidebarService
                .existsSession(userService.getCurrentUsername(), project.getId())) {
            return true;
        }

        return false;
    }

    private boolean isViewingPotentialCurationTarget()
    {
        // Curation sidebar is not allowed when viewing another users annotations
        String currentUsername = userService.getCurrentUsername();
        AnnotatorState state = getModelObject();
        return asList(CURATION_USER, currentUsername).contains(state.getUser().getUsername());
    }

    private IconType getStateIcon()
    {
        if (isSessionActive()) {
            return FontAwesome5IconType.play_circle_s;
        }

        return FontAwesome5IconType.stop_circle_s;
    }
}
