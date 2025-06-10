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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

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
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.recommendation.actionbar.RecommenderActionBarPanel;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;

@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true} and ${websocket.recommender-events.enabled:true}")
@Controller
public class RecommendationEventWebsocketControllerImpl
    implements RecommendationEventWebsocketController
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SimpMessagingTemplate msgTemplate;
    private final RecommendationService recommendationService;
    private final UserDao userService;

    public RecommendationEventWebsocketControllerImpl(@Autowired SimpMessagingTemplate aMsgTemplate,
            RecommendationService aRecommenderService, UserDao aUserService)
    {
        msgTemplate = aMsgTemplate;
        recommendationService = aRecommenderService;
        userService = aUserService;
    }

    @EventListener
    @Override
    public void onRecommenderTaskEvent(RecommenderTaskNotificationEvent aEvent)
    {
        var project = aEvent.getProject();
        var eventMsg = makeEventMessage(aEvent);

        var channel = getChannel(project, aEvent.getUser());

        LOG.debug("Sending event to [{}]: {}", channel, eventMsg);

        msgTemplate.convertAndSend("/topic" + channel, eventMsg);
    }

    private RRecommenderLogMessage makeEventMessage(RecommenderTaskNotificationEvent aEvent)
    {
        var message = aEvent.getMessage();
        var messageBody = message != null ? message.message : null;
        var messageLevel = message != null ? message.level : null;

        if (aEvent.getSource() instanceof PredictionTask) {
            var sessionOwner = userService.get(aEvent.getUser());
            var predictions = recommendationService.getIncomingPredictions(sessionOwner,
                    aEvent.getProject());

            if (predictions != null && predictions.hasNewSuggestions()) {
                return new RRecommenderLogMessage(messageLevel, messageBody,
                        asList(RecommenderActionBarPanel.STATE_PREDICTIONS_AVAILABLE), emptyList());
            }
        }

        return new RRecommenderLogMessage(messageLevel, messageBody);
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
