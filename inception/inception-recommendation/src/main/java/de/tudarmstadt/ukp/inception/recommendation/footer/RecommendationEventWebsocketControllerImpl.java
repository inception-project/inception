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

import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_RECOMMENDER;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;

@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true} and ${websocket.recommender-events.enabled:true}")
@Controller
public class RecommendationEventWebsocketControllerImpl
    implements RecommendationEventWebsocketController
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SimpMessagingTemplate msgTemplate;

    public RecommendationEventWebsocketControllerImpl(@Autowired SimpMessagingTemplate aMsgTemplate)
    {
        msgTemplate = aMsgTemplate;
    }

    @EventListener
    @Override
    public void onRecommenderTaskEvent(RecommenderTaskNotificationEvent aEvent)
    {
        Project project = aEvent.getProject();
        RRecommenderLogMessage eventMsg = new RRecommenderLogMessage(aEvent.getMessage().getLevel(),
                aEvent.getMessage().getMessage());
        String channel = getChannel(project, aEvent.getUser());

        LOG.debug("Sending event to [{}]: {}", channel, eventMsg);

        msgTemplate.convertAndSend("/topic" + channel, eventMsg);
    }

    static String getChannel(Project project, String aUsername)
    {
        return TOPIC_ELEMENT_PROJECT + project.getId() + TOPIC_ELEMENT_USER + aUsername
                + TOPIC_RECOMMENDER;
    }

    @Override
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
