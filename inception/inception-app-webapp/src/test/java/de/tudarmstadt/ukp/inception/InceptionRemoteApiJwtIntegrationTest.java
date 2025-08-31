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
package de.tudarmstadt.ukp.inception;

import static com.nimbusds.jose.JOSEObjectType.JWT;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static java.lang.System.setProperty;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;

import com.nimbusds.jwt.SignedJWT;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;

public class InceptionRemoteApiJwtIntegrationTest
{
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String REMOTE_ADMIN_USER_NAME = "remote-admin";
    private static final String ISSUER_ID = "default";
    private static final String CLIENT_ID = "inception-client";

    static final String SUB = UUID.randomUUID().toString();

    static String oldServerPort;
    static @TempDir Path appHome;
    static MockOAuth2Server oauth2Server;
    static ConfigurableApplicationContext context;
    static HttpClient client;
    static UserDao userService;

    @BeforeAll
    static void setup()
    {
        oldServerPort = System.setProperty("server.port", "0");

        oauth2Server = new MockOAuth2Server();
        oauth2Server.start();

        var issuerUrl = oauth2Server.url(ISSUER_ID).toString();

        setProperty("spring.main.banner-mode", "off");
        setProperty("java.awt.headless", "true");
        setProperty("database.url", "jdbc:hsqldb:mem:testdb;hsqldb.tx=mvcc");
        setProperty("inception.home", appHome.toString());
        setProperty("remote-api.enabled", "true");
        setProperty("remote-api.oauth2.enabled", "true");
        setProperty("remote-api.oauth2.realm", CLIENT_ID);
        setProperty("remote-api.oauth2.user-name-attribute", PREFERRED_USERNAME_CLAIM);
        setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuerUrl);
        // setProperty("logging.level.org.springframework.security", "TRACE");

        context = INCEpTION.start(new String[] {}, INCEpTION.class);

        client = HttpClient.newBuilder().build();

        userService = context.getBean(UserDao.class);

        userService.create(User.builder().withUsername(REMOTE_ADMIN_USER_NAME)
                .withRealm(Realm.REALM_EXTERNAL_PREFIX + CLIENT_ID) //
                .withRoles(ROLE_REMOTE, ROLE_USER, ROLE_ADMIN) //
                .withEnabled(true) //
                .build());
    }

    @AfterAll
    static void teardown()
    {
        if (oldServerPort == null) {
            System.getProperties().remove("server.port");

        }
        else {
            System.setProperty("server.port", oldServerPort);
        }
        context.close();
        oauth2Server.shutdown();
        LogManager.shutdown();
    }

    @Test
    void thatRemoteApiAccessWorks() throws Exception
    {
        var token = oauth2Server.issueToken(ISSUER_ID, SUB,
                new DefaultOAuth2TokenCallback(ISSUER_ID, SUB, JWT.getType(), emptyList(),
                        Map.of(PREFERRED_USERNAME_CLAIM, REMOTE_ADMIN_USER_NAME)));

        var request = listProjects(token);

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Tag("slow")
    @Test
    void thatExpiredTokenIsRejected() throws Exception
    {
        var expiry = 1;
        // JwtTimestampValidator.DEFAULT_MAX_CLOCK_SKEW = 60 seconds
        var tokenExpiredAt = Instant.now().plus(Duration.ofSeconds(expiry + 60));

        var token = oauth2Server.issueToken(ISSUER_ID, SUB,
                new DefaultOAuth2TokenCallback(ISSUER_ID, SUB, JWT.getType(), emptyList(),
                        Map.of(PREFERRED_USERNAME_CLAIM, REMOTE_ADMIN_USER_NAME), expiry));

        await().atMost(expiry + 65, SECONDS).until(() -> Instant.now().isAfter(tokenExpiredAt));

        var request = listProjects(token);

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpRequest listProjects(SignedJWT token)
    {
        var port = context.getEnvironment().getProperty("local.server.port");

        var request = HttpRequest.newBuilder() //
                .GET() //
                .uri(URI.create("http://localhost:" + port + "/api/aero/v1/projects")) //
                .setHeader("Authorization", "Bearer " + token.serialize()) //
                .build();
        return request;
    }
}
