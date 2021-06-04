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
package de.tudarmstadt.ukp.inception.websocket.footer;

import static de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference.DayJsPlugin.LOCALIZED_FORMAT;
import static de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference.DayJsPlugin.RELATIVE_TIME;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeAction;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapFeedbackPanel;
import de.tudarmstadt.ukp.inception.support.dayjs.DayJsResourceReference;
import de.tudarmstadt.ukp.inception.support.vue.VueComponent;
import de.tudarmstadt.ukp.inception.websocket.LoggedEventMessageController;
import de.tudarmstadt.ukp.inception.websocket.LoggedEventMessageControllerImpl;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;

@AuthorizeAction(action = Action.RENDER, roles = "ROLE_ADMIN")
public class LoggedEventFooterPanel extends VueComponent
{
    private static final long serialVersionUID = -9006607500867612027L;
    private final static Logger LOG = LoggerFactory.getLogger(LoggedEventFooterPanel.class);

    private @SpringBean LoggedEventMessageController loggedEventService;
    private @SpringBean ServletContext servletContext;
    
    public LoggedEventFooterPanel(String aId)
    {
        super(aId, "LoggedEventFooterPanel.vue");
        
        setOutputMarkupPlaceholderTag(true);
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        // model will be added as props to vue component
        setDefaultModel(Model.ofMap(Map.of("wsEndpoint", constructEndpointUrl(),
                "topicChannel", LoggedEventMessageControllerImpl.LOGGED_EVENTS, 
                "feedbackPanelId", retrieveFeedbackPanelId())));
    }

    private String retrieveFeedbackPanelId()
    {
        Page page = null;
        BootstrapFeedbackPanel feedbackPanel = null;
        String feedbackPanelId = "";
        try {
            page = getPage();
        }
        catch (WicketRuntimeException e) {
            LOG.debug("No page yet.");
        }
        if (page != null) {
            feedbackPanel = getPage().visitChildren(BootstrapFeedbackPanel.class,
                new IVisitor<BootstrapFeedbackPanel, BootstrapFeedbackPanel>()
                {

                    @Override
                    public void component(BootstrapFeedbackPanel aFeedbackPanel,
                            IVisit<BootstrapFeedbackPanel> aVisit)
                    {
                        aVisit.stop(aFeedbackPanel);
                    }
                });
            if (feedbackPanel != null) {
                feedbackPanelId = feedbackPanel.getMarkupId();
            }
        }
        return feedbackPanelId;
    }

    private String constructEndpointUrl() {
        Url endPointUrl = Url.parse(String.format("%s%s", servletContext.getContextPath(),
                WebsocketConfig.WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        String fullUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
        return fullUrl;
    }
    
    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(new DayJsResourceReference(RELATIVE_TIME, LOCALIZED_FORMAT)));
        aResponse.render(forReference(new WebjarsJavaScriptResourceReference("webstomp-client/current/dist/webstomp.min.js")));
    }
}
