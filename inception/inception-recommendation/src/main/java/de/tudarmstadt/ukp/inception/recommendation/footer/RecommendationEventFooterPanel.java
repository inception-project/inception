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
package de.tudarmstadt.ukp.inception.recommendation.footer;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getCsrfTokenFromSession;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_RECOMMENDER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.support.svelte.SvelteBehavior;
import de.tudarmstadt.ukp.inception.ui.core.feedback.FeedbackPanelExtensionBehavior;
import jakarta.servlet.ServletContext;

public class RecommendationEventFooterPanel
    extends WebMarkupContainer
{
    private static final long serialVersionUID = -9006607500867612027L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean ServletContext servletContext;
    private @SpringBean UserDao userService;

    private FeedbackPanelExtensionBehavior feedback;

    public RecommendationEventFooterPanel(String aId)
    {
        super(aId);
        setOutputMarkupPlaceholderTag(true);
        feedback = new FeedbackPanelExtensionBehavior();
        add(feedback);
        add(new SvelteBehavior());
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        var maybeProject = getProject();

        if (maybeProject.isPresent()) {
            setVisible(maybeProject.isPresent());
            setDefaultModel(Model.ofMap(Map.of( //
                    "csrfToken", getCsrfTokenFromSession(), //
                    "wsEndpointUrl", constructEndpointUrl(), //
                    "topicChannel",
                    TOPIC_ELEMENT_PROJECT + getProject().get().getId() + TOPIC_ELEMENT_USER
                            + userService.getCurrentUsername() + TOPIC_RECOMMENDER, //
                    "feedbackPanelId", feedback.retrieveFeedbackPanelId(this))));
        }
        else {
            setVisible(false);
            setDefaultModel(Model.ofMap(emptyMap()));
        }
    }

    private Optional<Project> getProject()
    {
        Page page = null;
        try {
            page = getPage();
        }
        catch (WicketRuntimeException e) {
            LOG.debug("No page yet.");
        }

        if (page == null || !(page instanceof ProjectPageBase)) {
            return Optional.empty();
        }

        return Optional.ofNullable(((ProjectPageBase) page).getProject());
    }

    private String constructEndpointUrl()
    {
        var endPointUrl = Url.parse(format("%s%s", servletContext.getContextPath(), WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }
}
