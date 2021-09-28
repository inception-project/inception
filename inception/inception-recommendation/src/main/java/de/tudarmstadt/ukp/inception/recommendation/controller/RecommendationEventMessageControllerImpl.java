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
package de.tudarmstadt.ukp.inception.recommendation.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderErrorEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderEvaluationResultEvent;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderEvaluationResultEventAdapter.Details;
import de.tudarmstadt.ukp.inception.websocket.model.WebsocketEventMessage;

@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true")
@Controller
public class RecommendationEventMessageControllerImpl
    implements RecommendationEventMessageController
{  
    private final Logger log = LoggerFactory
            .getLogger(RecommendationEventMessageControllerImpl.class);

    public static final String REC_EVENTS = "/recEvents";
    public static final String REC_EVAL_EVENTS = "/recEvalEvents";
    public static final String REC_EVENTS_TOPIC = "/queue" + REC_EVENTS;
    public static final String REC_EVAL_EVENTS_TOPIC = "/queue" + REC_EVAL_EVENTS;
    
    private static final int MAX_POINTS_TO_PLOT = 50;
    
    private final EventRepository eventRepo;
    private final SimpMessagingTemplate msgTemplate;
    private final ProjectService projectService;
    private final UserDao userService;
    private final RecommendationService recService;

    @Autowired
    public RecommendationEventMessageControllerImpl(SimpMessagingTemplate aMsgTemplate, 
            ProjectService aProjectService, UserDao aUserDao, 
            RecommendationService aRecommendationService, EventRepository aEventRepository)
    {
        msgTemplate = aMsgTemplate;
        projectService = aProjectService;
        userService = aUserDao;
        eventRepo = aEventRepository;
        recService = aRecommendationService;
    }

    @EventListener
    @Override
    public void onRecommenderErrorEvent(RecommenderErrorEvent aEvent)
    {
        String errorMsg = aEvent.getErrorMsg();
        if (errorMsg == null) {
            return;
        }
        Recommender recommender = aEvent.getRecommender();
        Project project = recommender.getProject();
        WebsocketEventMessage eventMsg = new WebsocketEventMessage(aEvent.getTimestamp(),
                aEvent.getClass().getSimpleName());
        eventMsg.setEventMsg("[" + recommender.getName() + "] " + errorMsg);

        String channel = REC_EVENTS_TOPIC + "/" + project.getId();
        log.debug("Sending websocket event: {} '{}' to {}.", eventMsg.getEventType(),
                eventMsg.getEventMsg(), channel);
        msgTemplate.convertAndSendToUser(aEvent.getUser(), channel, eventMsg);
    }
    
    @EventListener
    @Override
    public void onRecommenderEvaluationEvent(RecommenderEvaluationResultEvent aEvent)
    {
        Recommender recommender = aEvent.getRecommender();
        Project project = recommender.getProject();
        User user = userService.get(aEvent.getUser());
        WebsocketEventMessage eventMsg = addEvalResultMetrics(recommender, user, 
                new WebsocketEventMessage(aEvent.getTimestamp(), aEvent.getClass().getSimpleName()));
        
        String channel = REC_EVAL_EVENTS_TOPIC + "/" + project.getId();
        log.debug("onRecommenderEvaluationEvent: Sending websocket event: {} '{}' to {}.", eventMsg.getEventType(),
                eventMsg.getEventMsg(), channel);
        msgTemplate.convertAndSendToUser(aEvent.getUser(), channel, eventMsg);
    }
    
    @SubscribeMapping(REC_EVAL_EVENTS + "/{aProjectId}")
    @Override
    public List<WebsocketEventMessage> getMostRecentRecommenderEvents(Principal aPrincipal,
            @DestinationVariable String aProjectId)
    {
        List<WebsocketEventMessage> recentEvents = new ArrayList<>();
        Project project = projectService.getProject(Long.valueOf(aProjectId));
        String username = aPrincipal.getName();
        User user = userService.get(username);
        
        //check that user is authorized for the project
        if (project == null || user == null || !projectService.existsProjectPermission(user, project)) {
            throw new IllegalArgumentException(String.format("Could not identify %s with access for project %s", 
                    username, project.getName()));
        }
        
        // get latest evaluation results for available recommenders for this user + project
        // and send as events to subscribing client
        String recommenderEvalEventType = RecommenderEvaluationResultEvent.class.getSimpleName();
        // get the last 50 logged evaluation results from the event log
        recentEvents = recService.listEnabledRecommenders(project).stream()//
              .flatMap(r -> eventRepo.listLoggedEventsForRecommender(
                project, username,
                recommenderEvalEventType, MAX_POINTS_TO_PLOT, r.getId()).stream()//
                      .map(event ->  createWebsocketEventMessageForRecommenderEvaluation(event, project, r)))//
              .collect(Collectors.toList());
        log.debug("getMostRecentRecommenderEvents: Sending websocket event: {}  to {}.",
                recentEvents.stream().map(event -> event.toString()), username);
        return recentEvents;
    }
    
    private WebsocketEventMessage createWebsocketEventMessageForRecommenderEvaluation(
            LoggedEvent aEvent, Project aProject, Recommender aRec)
    {
        Details recDetails;
        String recMetrics = null;
        try {
            recDetails = JSONUtil.fromJsonString(Details.class, aEvent.getDetails());
            recMetrics = JSONUtil.toJsonString(new RecommenderMetrics(aRec, recDetails));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        WebsocketEventMessage msg = new WebsocketEventMessage(aEvent.getCreated(),
                RecommenderErrorEvent.class.getSimpleName());
        msg.setEventMsg(recMetrics);
        return msg;
    }

    private WebsocketEventMessage addEvalResultMetrics(Recommender aRec, User aUser, WebsocketEventMessage aMsg)
    {
        Optional<EvaluatedRecommender> evalRec = recService.getEvaluatedRecommender(aUser, aRec);
        if (evalRec.isEmpty()) {
            return aMsg;
        }
        
        try {
            aMsg.setEventMsg(JSONUtil.toJsonString(new RecommenderMetrics(evalRec.get())));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        return aMsg;
    }

    @Override
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
    
    public class RecommenderMetrics
    {
        private String name;
        private double accuracy;
        private double precision;
        private double recall;
        private double f1;

        public RecommenderMetrics(EvaluatedRecommender aEvaluatedRecommender)
        {
            this(aEvaluatedRecommender.getRecommender(), aEvaluatedRecommender.getEvaluationResult());
        }
        
        public RecommenderMetrics(String aRecName, double aAcc, double aPrec, double aRecall, double aF1) {
            name = aRecName;
            accuracy = aAcc;
            precision = aPrec;
            recall = aRecall;
            f1 = aF1;
        }

        public RecommenderMetrics(Recommender aRec, EvaluationResult aEvalResult)
        {
            this(aRec.getName(), aEvalResult.computeAccuracyScore(),
                    aEvalResult.computePrecisionScore(), aEvalResult.computeRecallScore(),
                    aEvalResult.computeF1Score());
        }

        public RecommenderMetrics(Recommender aRec, Details aRecDetails)
        {
            this(aRec.getName(), aRecDetails.accuracy, aRecDetails.precision, aRecDetails.recall, aRecDetails.f1);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String aRecommenderName)
        {
            name = aRecommenderName;
        }

        public double getAccuracy()
        {
            return accuracy;
        }

        public void setAccuracy(double aAccuracy)
        {
            accuracy = aAccuracy;
        }

        public double getPrecision()
        {
            return precision;
        }

        public void setPrecision(double aPrecision)
        {
            precision = aPrecision;
        }

        public double getRecall()
        {
            return recall;
        }

        public void setRecall(double aRecall)
        {
            recall = aRecall;
        }

        public double getF1()
        {
            return f1;
        }

        public void setF1(double aF1)
        {
            f1 = aF1;
        }
    }
}
