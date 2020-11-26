/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;

/**
 * External workflow extension type
 */
public class ExternalWorkflowExtension
    implements WorkflowExtension, IDocumentChangedEventListener
{
    public static final String EXTERNAL_EXTENSION = "external";

    private static final String EXTERNAL_URL = "http://localhost:5000";

    private static final Logger logger = LoggerFactory.getLogger(ExternalWorkflowExtension.class);

    private final UserDao userRepository;
    private final EventRepository eventRepository;
    private final DocumentService documentService;

    public ExternalWorkflowExtension(UserDao aUserRepository, EventRepository aEventRepository, DocumentService aDocumentService)
    {
        userRepository = aUserRepository;
        eventRepository = aEventRepository;
        documentService = aDocumentService;
    }

    @Override
    public String getLabel()
    {
        return "External workflow";
    }

    @Override
    public String getId()
    {
        return EXTERNAL_EXTENSION;
    }

    @Override
    public List<SourceDocument> getNextDocument(List<SourceDocument> aSourceDocuments)
    {
        User user = userRepository.getCurrentUser();
        var texts = new ArrayList<String>();

        for (SourceDocument sourceDocument : aSourceDocuments) {
            try {
                CAS cas = documentService.readAnnotationCas(sourceDocument, user.getUsername(), AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                String text = cas.getDocumentText();
                texts.add(text);
            } catch (IOException e) {
                logger.error("Error while reading annotation CAS", e);
                return aSourceDocuments;
            }
        }

        var objectMapper = new ObjectMapper();

        try {
            HttpClient client = HttpClient.newHttpClient();

            String url = EXTERNAL_URL + "/rerank/" + user.getUsername();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(texts)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.error("Error while sending sorting request: " + response.body());
                return aSourceDocuments;
            }

            List<Integer> ranks = objectMapper.readValue(response.body(), new TypeReference<List<Integer>>() { });

            if (aSourceDocuments.size() != ranks.size()) {
                logger.error("Ranks have different len that source documents");
                return aSourceDocuments;
            }

            var result = new ArrayList<SourceDocument>(Collections.nCopies(ranks.size(), null));

            for (int i = 0; i < ranks.size(); i++ ) {
                int rank = ranks.get(i);
                var doc = aSourceDocuments.get(rank);
                result.set(i, doc);
            }

            return result;

        } catch (IOException | InterruptedException e) {
            logger.error("Error while sending sorting request", e);
            return aSourceDocuments;
        }
    }

    @EventListener
    @Async
    public void onDocumentStateChangedEvent(AnnotationStateChangeEvent aEvent)
    {
        String userName = aEvent.getAnnotationDocument().getUser();
        logger.info("Annotation state changed for user: {}", userName);

        // Obtain the annotation times
        Project project = aEvent.getDocument().getProject();
        List<LoggedEvent> annoChangedEvents = eventRepository.listLoggedEventsOfType(project, userName, "AnnotationStateChangeEvent");
        List<LoggedEvent> docOpenedEvents = eventRepository.listLoggedEventsOfType(project, userName, "DocumentOpenedEvent");

        ObjectMapper mapper = new ObjectMapper();

        List<String> texts = new ArrayList<>();
        List<Long> times = new ArrayList<>();

        Map<String, Long> docToStartTime = Maps.newHashMap();
        Map<String, Long> docToEndTime = Maps.newHashMap();
        Map<String, String> docToText = Maps.newHashMap();

        try (CasStorageSession session = CasStorageSession.open()) {
            // DocumentOpenedEvent
            for (LoggedEvent e : docOpenedEvents) {
                var sourceDocument = documentService.getSourceDocument(project.getId(), e.getDocument());
                CAS cas = documentService.readAnnotationCas(sourceDocument, userName, AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                String text = cas.getDocumentText();

                String docName = sourceDocument.getName();
                docToStartTime.put(docName, e.getCreated().getTime());
                docToText.put(docName, text);
            }

            // AnnotationStateChangeEvent
            for (LoggedEvent e : annoChangedEvents) {
                Map<String, String> details = mapper.readValue(e.getDetails(), Map.class);

                if (!("INPROGRESS".equals(details.get("previousState")) && "FINISHED".equals(details.get("state")))) {
                    continue;
                }

                var sourceDocument = documentService.getSourceDocument(project.getId(), e.getDocument());
                CAS cas = documentService.readAnnotationCas(sourceDocument, userName, AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                String text = cas.getDocumentText();

                String docName = sourceDocument.getName();
                docToEndTime.put(docName, e.getCreated().getTime());
                docToText.put(docName, text);
            }

            for (String docName : docToText.keySet()) {
                if (!(docToStartTime.containsKey(docName) && docToEndTime.containsKey(docName))) {
                    continue;
                }

                long time = docToEndTime.get(docName) - docToStartTime.get(docName);
                String text = docToText.get(docName);

                texts.add(text);
                times.add(time);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("texts", texts);
            data.put("times", times);

            HttpClient client = HttpClient.newHttpClient();

            String url = EXTERNAL_URL + "/train/" + userName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException | InterruptedException e) {
            logger.error("Error while sending training request", e);
        }
    }
}

interface IDocumentChangedEventListener
{
    @EventListener
    @Async
    void onDocumentStateChangedEvent(AnnotationStateChangeEvent aEvent);
}
