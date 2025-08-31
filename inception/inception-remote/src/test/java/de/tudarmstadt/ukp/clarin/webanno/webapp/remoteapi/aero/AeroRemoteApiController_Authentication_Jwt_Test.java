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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase.API_BASE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileSystemUtils;

import com.giffing.wicket.spring.boot.starter.WicketAutoConfiguration;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.config.DashboardAutoConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@ActiveProfiles(DeploymentModeServiceImpl.PROFILE_AUTH_MODE_DATABASE)
@SpringBootTest(webEnvironment = RANDOM_PORT, //
        properties = { //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off", //
                "remote-api.enabled=true", //
                "remote-api.http-basic.enabled=false", //
                "remote-api.oauth2.enabled=true", //
                "remote-api.oauth2.user-name-attribute=preferred-username", //
                "remote-api.oauth2.realm=" + AeroRemoteApiController_Authentication_Jwt_Test.REALM, //
                "repository.path="
                        + AeroRemoteApiController_Authentication_Jwt_Test.TEST_OUTPUT_FOLDER })
@EnableAutoConfiguration( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                DashboardAutoConfiguration.class, //
                EventLoggingAutoConfiguration.class, //
                SearchServiceAutoConfiguration.class, //
                WicketAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@TestMethodOrder(MethodOrderer.MethodName.class)
class AeroRemoteApiController_Authentication_Jwt_Test
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/AeroRemoteApiController_Authentication_Jwt_Test";
    static final String REALM = "test";

    private static final long EXPIRATIONTIME = 864_000_000; // 10 days
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_STRING = "Authorization";
    private String token;

    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;

    private @Autowired TestRestTemplate template;

    private static Project project;
    private static String password;
    private static User remoteApiAdminUser;
    private static User remoteApiNormalUser;
    private static User nonRemoteNormalUser;

    @BeforeAll
    static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    void setup() throws Exception
    {
        setupOnce();

        template.getRestTemplate().setInterceptors(asList((request, body, execution) -> {
            request.getHeaders().add(HEADER_STRING, TOKEN_PREFIX + getToken());
            return execution.execute(request, body);
        }));
    }

    public static String jwt(String aUsername /* , String... aRoles */)
    {
        return Jwts.builder() //
                .setSubject(aUsername) //
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATIONTIME)) //
                .claim("preferred-username", aUsername)
                // .claim("role", asList(aRoles))
                // .signWith(SignatureAlgorithm.HS512, SECRET) //
                .compact();
    }

    String getToken()
    {
        return token;
    }

    void setupOnce() throws Exception
    {
        if (project != null) {
            return;
        }

        password = RandomStringUtils.random(16, true, true);

        project = new Project("project1");
        projectService.createProject(project);

        remoteApiAdminUser = new User("admin", ROLE_ADMIN, ROLE_REMOTE);
        remoteApiAdminUser.setRealm(Realm.REALM_EXTERNAL_PREFIX + REALM);
        remoteApiAdminUser.setPassword(password);
        userRepository.create(remoteApiAdminUser);

        remoteApiNormalUser = new User("user", ROLE_USER, ROLE_REMOTE);
        remoteApiNormalUser.setRealm(Realm.REALM_EXTERNAL_PREFIX + REALM);
        remoteApiNormalUser.setPassword(password);
        userRepository.create(remoteApiNormalUser);

        nonRemoteNormalUser = new User("non-remote-user", ROLE_USER);
        nonRemoteNormalUser.setRealm(Realm.REALM_EXTERNAL_PREFIX + REALM);
        nonRemoteNormalUser.setPassword(password);
        userRepository.create(nonRemoteNormalUser);
    }

    @Test
    void thatRemoteApiAdminUserCanAuthenticate()
    {
        // token = jwt(remoteApiAdminUser.getUsername(), "USER", "ADMIN", "REMOTE");
        token = jwt(remoteApiAdminUser.getUsername());

        var response = template.getForEntity(API_BASE + "/projects", String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains("\"project1\"");
    }

    @Test
    void thatRemoteApiNormalUserCanAuthenticate()
    {
        // token = jwt(remoteApiNormalUser.getUsername(), "USER", "REMOTE");
        token = jwt(remoteApiNormalUser.getUsername());

        var response = template.getForEntity(API_BASE + "/projects", String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).as("Returns empty project list").contains("\"body\":[]");
    }

    @Test
    void thatUserCannotAuthenticateWithoutRemoteApiRole()
    {
        // token = jwt(nonRemoteNormalUser.getUsername(), "USER");
        token = jwt(nonRemoteNormalUser.getUsername());

        var response = template.getForEntity(API_BASE + "/projects", String.class);

        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
        // assertThat(response.getBody()).contains("\"Forbidden\"");
    }

    @Test
    void thatNonExistingUserCannotAuthenticate()
    {
        // token = jwt("some-user", "USER", "REMOTE");
        token = jwt("some-user");

        var response = template.getForEntity(API_BASE + "/projects", String.class);

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
        // assertThat(response.getBody()).contains("\"Forbidden\"");
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config

        @Bean
        public ApplicationContextProvider applicationContextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        public JwtDecoder jwtDecoder()
        {
            // This anonymous class needs for the possibility of using SpyBean in test methods
            // Lambda cannot be a spy with spring @SpyBean annotation
            return new JwtDecoder()
            {
                @Override
                public Jwt decode(String aToken)
                {
                    var parser = Jwts.parser().unsecured().build();
                    var jwt = parser.parse(aToken);

                    return new Jwt(aToken, null, null, jwt.getHeader(), (Claims) jwt.getPayload());
                }
            };
        }
    }
}
