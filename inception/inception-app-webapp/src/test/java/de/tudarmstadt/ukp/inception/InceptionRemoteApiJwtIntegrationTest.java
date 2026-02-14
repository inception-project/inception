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
import static java.lang.System.getProperty;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

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

    static @TempDir Path appHome;
    static MockOAuth2Server oauth2Server;
    static ConfigurableApplicationContext context;
    static String oldHeadless;
    static HttpClient client;
    static UserDao userService;

    @BeforeAll
    static void setup()
    {
        oauth2Server = new MockOAuth2Server();
        oauth2Server.start();

        var issuerUrl = oauth2Server.url(ISSUER_ID).toString();

        oldHeadless = getProperty("java.awt.headless");
        setProperty("java.awt.headless", "true");

        var properties = new java.util.Properties();
        properties.setProperty("server.port", "0");
        properties.setProperty("spring.main.banner-mode", "off");
        properties.setProperty("database.url", "jdbc:hsqldb:mem:testdb;hsqldb.tx=mvcc");
        properties.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        properties.setProperty("inception.home", appHome.toString());
        properties.setProperty("remote-api.enabled", "true");
        properties.setProperty("remote-api.oauth2.enabled", "true");
        properties.setProperty("remote-api.oauth2.realm", CLIENT_ID);
        properties.setProperty("remote-api.oauth2.user-name-attribute", PREFERRED_USERNAME_CLAIM);
        properties.setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuerUrl);
        // properties.setProperty("logging.level.org.springframework.security", "TRACE");

        context = INCEpTION.start( //
                new String[] {}, //
                properties, //
                INCEpTION.class, //
                LongTimeoutJwtConfig.class);

        client = HttpClient.newBuilder() //
                .connectTimeout(Duration.ofSeconds(60)) //
                .build();

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
        context.close();
        oauth2Server.shutdown();
        LogManager.shutdown();
        if (oldHeadless == null) {
            System.getProperties().remove("java.awt.headless");
        }
        else {
            setProperty("java.awt.headless", oldHeadless);
        }
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

    @TestConfiguration
    public static class LongTimeoutJwtConfig
    {
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private String issuerUri;

        @Bean
        public JwtDecoder jwtDecoder()
        {
            // Force a 90-second timeout to survive Windows CI starvation on GitHub actions
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(90_000);
            requestFactory.setReadTimeout(90_000);

            RestOperations rest = new RestTemplate(requestFactory);

            return NimbusJwtDecoder //
                    .withIssuerLocation(issuerUri) //
                    .restOperations(rest) //
                    .build();
        }
    }
}
