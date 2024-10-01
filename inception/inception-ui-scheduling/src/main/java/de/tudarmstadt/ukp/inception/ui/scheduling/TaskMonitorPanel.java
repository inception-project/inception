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
package de.tudarmstadt.ukp.inception.ui.scheduling;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getCsrfTokenFromSession;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.constructEndpointUrl;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.constructWsEndpointUrl;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map;

import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerController;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerWebsocketController;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import jakarta.servlet.ServletContext;

@AuthorizeAction(action = Action.RENDER, roles = "ROLE_USER")
public class TaskMonitorPanel
    extends WebMarkupContainer
{
    private static final long serialVersionUID = -9006607500867612027L;

    private @SpringBean ServletContext servletContext;
    private @SpringBean SchedulerWebsocketController schedulerWebsocketController;

    private final String taskStatusTopic;
    private final String taskUpdatesTopic;
    private boolean popupMode = true;
    private boolean showFinishedTasks = true;
    private String typePattern = "";

    /**
     * Create a monitoring panel that subscribes to all events for the current user.
     * 
     * @param aId
     *            The non-null id of this component
     */
    public TaskMonitorPanel(String aId)
    {
        super(aId);
        setOutputMarkupPlaceholderTag(true);
        taskStatusTopic = "/app" + SchedulerWebsocketController.getUserTaskUpdatesTopic();
        taskUpdatesTopic = "/user/queue" + SchedulerWebsocketController.getUserTaskUpdatesTopic();
    }

    /**
     * Create a monitoring panel that subscribes to all events for the given project.
     * 
     * @param aId
     *            The non-null id of this component
     * @param aProject
     *            The project to monitor.
     */
    public TaskMonitorPanel(String aId, Project aProject)
    {
        super(aId);
        setOutputMarkupPlaceholderTag(true);
        taskStatusTopic = "/app"
                + SchedulerWebsocketController.getProjectTaskUpdatesTopic(aProject);
        taskUpdatesTopic = "/topic"
                + SchedulerWebsocketController.getProjectTaskUpdatesTopic(aProject);
    }

    public TaskMonitorPanel setPopupMode(boolean aPopupMode)
    {
        popupMode = aPopupMode;
        return this;
    }

    public TaskMonitorPanel setShowFinishedTasks(boolean aKeepRemovedTasks)
    {
        showFinishedTasks = aKeepRemovedTasks;
        return this;
    }

    public TaskMonitorPanel setTypePattern(String aTypePattern)
    {
        if (isBlank(aTypePattern)) {
            typePattern = "";
        }
        else {
            typePattern = aTypePattern;
        }

        return this;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setDefaultModel(Model.ofMap(Map.of( //
                "csrfToken", getCsrfTokenFromSession(), //
                "popupMode", popupMode, //
                "showFinishedTasks", showFinishedTasks, //
                "typePattern", typePattern, //
                "endpointUrl", constructEndpointUrl(SchedulerController.BASE_URL), //
                "wsEndpointUrl", constructWsEndpointUrl(WS_ENDPOINT), //
                "taskStatusTopic", taskStatusTopic, //
                "taskUpdatesTopic", taskUpdatesTopic)));

        add(new SvelteBehavior());
    }
}
