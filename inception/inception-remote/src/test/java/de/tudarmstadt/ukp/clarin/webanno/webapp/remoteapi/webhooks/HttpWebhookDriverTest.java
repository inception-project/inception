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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookDriver.X_AERO_NOTIFICATION;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.ANNOTATION_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.DOCUMENT_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.WebhookService.PROJECT_STATE;
import static de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils.assumeEndpointIsAvailable;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.MethodNotAllowed;
import org.springframework.web.client.ResourceAccessException;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.HttpWebhookDriverTest.TestService;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.AnnotationStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.DocumentStateChangeMessage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json.ProjectStateChangeMessage;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@SpringBootTest( //
        webEnvironment = RANDOM_PORT, //
        properties = { //
                // Not really used atm but if manage to set up a testing badssl via testcontainers,
                // it might come in useful.
                // "jdk.net.hosts.file=src/test/resources/hosts", //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off",
                "repository.path=" + HttpWebhookDriverTest.TEST_OUTPUT_FOLDER })
@ImportAutoConfiguration( //
        exclude = { //
                SecurityAutoConfiguration.class, //
                LiquibaseAutoConfiguration.class }, //
        classes = { //
                ServletWebServerFactoryAutoConfiguration.class, //
                RestTemplateAutoConfiguration.class, //
                DispatcherServletAutoConfiguration.class, //
                WebMvcAutoConfiguration.class, //
                RemoteApiAutoConfiguration.class, //
                TestService.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Disabled("Should not call badssl.com during automated testing")
public class HttpWebhookDriverTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/WebhookServiceTest";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @LocalServerPort int port;
    private @Autowired WebhooksConfiguration webhooksConfiguration;
    private @Autowired TestService testService;
    private @Autowired HttpWebhookDriver webhookDriver;

    private Project project;
    private SourceDocument doc;
    private AnnotationDocument ann;

    private Webhook hook;

    @BeforeEach
    void setup()
    {
        testService.reset();

        hook = new Webhook();
        hook.setUrl("http://localhost:" + port + "/test/subscribe");
        hook.setEnabled(true);

        webhooksConfiguration.setGlobalHooks(asList(hook));

        project = new Project("test");
        project.setState(ProjectState.NEW);
        project.setId(1l);

        doc = new SourceDocument();
        doc.setProject(project);
        doc.setName("testDoc");
        doc.setId(2l);
        doc.setState(SourceDocumentState.ANNOTATION_IN_PROGRESS);

        ann = new AnnotationDocument();
        ann.setUser("user");
        ann.setId(3l);
        ann.setDocument(doc);
        ann.setState(AnnotationDocumentState.FINISHED);
        ann.setAnnotatorState(AnnotationDocumentState.IN_PROGRESS);
        ann.setAnnotatorComment("Test comment");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_expired()
    {
        assumeEndpointIsAvailable("https://expired.badssl.com/");

        hook.setUrl("https://expired.badssl.com/");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(ResourceAccessException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("PKIX path validation failed");

        hook.setVerifyCertificates(false);
        assertThatExceptionOfType(HttpClientErrorException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_wrongHost()
    {
        assumeEndpointIsAvailable("https://wrong.host.badssl.com/");

        hook.setUrl("https://wrong.host.badssl.com/");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(ResourceAccessException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("subject alternative");

        hook.setVerifyCertificates(false);
        assertThatExceptionOfType(HttpClientErrorException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_selfSigned()
    {
        assumeEndpointIsAvailable("https://self-signed.badssl.com/");

        hook.setUrl("https://self-signed.badssl.com/");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(ResourceAccessException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("PKIX path building failed");

        hook.setVerifyCertificates(false);
        assertThatExceptionOfType(HttpClientErrorException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_untrusted()
    {
        assumeEndpointIsAvailable("https://untrusted-root.badssl.com/");

        hook.setUrl("https://untrusted-root.badssl.com/");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(ResourceAccessException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("PKIX path building failed");

        hook.setVerifyCertificates(false);
        assertThatExceptionOfType(HttpClientErrorException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @Tag("slow")
    @Test
    @Disabled("For some reason, we don't get the PKIX error even when verify certificates is enabled...")
    void thatDisablingCertificateValidationWorks_revoked()
    {
        assumeEndpointIsAvailable("https://revoked.badssl.com");

        hook.setUrl("https://revoked.badssl.com");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(ResourceAccessException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("PKIX path validation failed");

        hook.setVerifyCertificates(false);
        assertThatExceptionOfType(MethodNotAllowed.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @Tag("slow")
    @Test
    void thatCertificateValidationWorks()
    {
        assumeEndpointIsAvailable("https://tls-v1-2.badssl.com:1012/");

        hook.setUrl("https://tls-v1-2.badssl.com:1012/");

        hook.setVerifyCertificates(true);
        assertThatExceptionOfType(HttpClientErrorException.class) //
                .isThrownBy(() -> webhookDriver.sendNotification("test", "test", hook)) //
                .withMessageContaining("405 Not Allowed");
    }

    @RequestMapping("/test")
    @Controller
    public static class TestService
    {
        private List<Pair<ProjectStateChangeMessage, HttpHeaders>> projectStateChangeMsgs = new ArrayList<>();
        private List<Pair<DocumentStateChangeMessage, HttpHeaders>> docStateChangeMsgs = new ArrayList<>();
        private List<Pair<AnnotationStateChangeMessage, HttpHeaders>> annStateChangeMsgs = new ArrayList<>();
        private int callCount;

        public void reset()
        {
            projectStateChangeMsgs.clear();
            docStateChangeMsgs.clear();
            annStateChangeMsgs.clear();
            callCount = 0;
        }

        @PostMapping( //
                value = "/subscribe", //
                headers = X_AERO_NOTIFICATION + "=" + PROJECT_STATE, //
                consumes = APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> onProjectStateEvent( //
                @RequestHeader HttpHeaders aHeaders, //
                @RequestBody ProjectStateChangeMessage aMsg)
            throws Exception
        {
            LOG.info("Received: {}", aMsg);
            projectStateChangeMsgs.add(Pair.of(aMsg, aHeaders));
            return ResponseEntity.ok().build();
        }

        @PostMapping( //
                value = "/subscribe", //
                headers = X_AERO_NOTIFICATION + "=" + DOCUMENT_STATE, //
                consumes = APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> onDocumentStateEvent( //
                @RequestHeader HttpHeaders aHeaders, //
                @RequestBody DocumentStateChangeMessage aMsg)
            throws Exception
        {
            LOG.info("Received: {}", aMsg);
            docStateChangeMsgs.add(Pair.of(aMsg, aHeaders));
            return ResponseEntity.ok().build();
        }

        @PostMapping( //
                value = "/subscribe", //
                headers = X_AERO_NOTIFICATION + "=" + ANNOTATION_STATE, //
                consumes = APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> onAnnotationStateEvent( //
                @RequestHeader HttpHeaders aHeaders, //
                @RequestBody AnnotationStateChangeMessage aMsg)
            throws Exception
        {
            LOG.info("Received: {}", aMsg);
            annStateChangeMsgs.add(Pair.of(aMsg, aHeaders));
            return ResponseEntity.ok().build();
        }

        @PostMapping( //
                value = "/failFirstTime", //
                headers = X_AERO_NOTIFICATION + "=" + ANNOTATION_STATE, //
                consumes = APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> failFirstTime( //
                @RequestHeader HttpHeaders aHeaders, //
                @RequestBody AnnotationStateChangeMessage aMsg)
            throws Exception
        {
            callCount++;

            if (callCount == 1) {
                return ResponseEntity.internalServerError().build();
            }

            annStateChangeMsgs.add(Pair.of(aMsg, aHeaders));
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
