/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.ANNOTATION_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.DOCUMENT_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.PROJECT_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.X_AERO_NOTIFICATION;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.ProjectStateChangeMessage;

@RunWith(SpringRunner.class)
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebhookServiceTest
{
    private @LocalServerPort int port;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired WebhooksConfiguration webhooksConfiguration;
    private @Autowired TestService testService;

    @Test
    public void test()
    {
        Webhook hook = new Webhook();
        hook.setUrl("http://localhost:" + port + "/test/subscribe");
        hook.setTopics(asList(PROJECT_STATE, ANNOTATION_STATE, DOCUMENT_STATE));
        hook.setEnabled(true);

        webhooksConfiguration.setGlobalHooks(asList(hook));

        Project project = new Project();
        project.setState(ProjectState.NEW);
        project.setId(1l);

        SourceDocument doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(2l);
        doc.setState(SourceDocumentState.ANNOTATION_IN_PROGRESS);

        AnnotationDocument ann = new AnnotationDocument();
        ann.setProject(project);
        ann.setId(3l);
        ann.setDocument(doc);
        ann.setState(AnnotationDocumentState.FINISHED);
        
        applicationEventPublisher.publishEvent(
                new ProjectStateChangedEvent(this, project, ProjectState.CURATION_FINISHED));
        applicationEventPublisher.publishEvent(
                new DocumentStateChangedEvent(this, doc, SourceDocumentState.NEW));
        applicationEventPublisher.publishEvent(
                new AnnotationStateChangeEvent(this, ann, AnnotationDocumentState.IN_PROGRESS));
        
        assertEquals(1, testService.projectStateChangeMsgs.size());
        assertEquals(1, testService.docStateChangeMsgs.size());
        assertEquals(1, testService.annStateChangeMsgs.size());
    }

    @RequestMapping("/test")
    @Controller
    public static class TestService
    {
        private List<ProjectStateChangeMessage> projectStateChangeMsgs = new ArrayList<>();
        private List<DocumentStateChangeMessage> docStateChangeMsgs = new ArrayList<>();
        private List<AnnotationStateChangeMessage> annStateChangeMsgs = new ArrayList<>();
        
        @RequestMapping(value = "/subscribe", 
                method = RequestMethod.POST, 
                headers = X_AERO_NOTIFICATION + "=" + PROJECT_STATE,
                consumes = APPLICATION_JSON_UTF8_VALUE, 
                produces = APPLICATION_JSON_UTF8_VALUE)
        public ResponseEntity<Void> onProjectStateEvent(
                @RequestBody ProjectStateChangeMessage aMsg)
            throws Exception
        {
            projectStateChangeMsgs.add(aMsg);
            return ResponseEntity.ok().build();
        }

        @RequestMapping(value = "/subscribe", 
                method = RequestMethod.POST, 
                headers = X_AERO_NOTIFICATION + "=" + DOCUMENT_STATE,
                consumes = APPLICATION_JSON_UTF8_VALUE, 
                produces = APPLICATION_JSON_UTF8_VALUE)
        public ResponseEntity<Void> onDocumentStateEvent(
                @RequestBody DocumentStateChangeMessage aMsg)
            throws Exception
        {
            docStateChangeMsgs.add(aMsg);
            return ResponseEntity.ok().build();
        }

        @RequestMapping(value = "/subscribe", 
                method = RequestMethod.POST, 
                headers = X_AERO_NOTIFICATION + "=" + ANNOTATION_STATE,
                consumes = APPLICATION_JSON_UTF8_VALUE, 
                produces = APPLICATION_JSON_UTF8_VALUE)
        public ResponseEntity<Void> onAnnotationStateEvent(
                @RequestBody AnnotationStateChangeMessage aMsg)
            throws Exception
        {
            annStateChangeMsgs.add(aMsg);
            return ResponseEntity.ok().build();
        }
    }

    @Configuration
    public static class TestContext
    {
        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }
}
