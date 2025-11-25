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
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;

@ActiveProfiles(DeploymentModeServiceImpl.PROFILE_AUTH_MODE_DATABASE)
@SpringBootTest( //
        webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                // "debug=true", // "
                "spring.main.banner-mode=off", //
                "search.enabled=false", //
                "remote-api.enabled=true", //
                "remote-api.tasks.enabled=true" })
@EnableWebSecurity
@EnableAutoConfiguration( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                EventLoggingAutoConfiguration.class, //
                SearchServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class AeroTaskControllerTest
{
    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired SchedulingService schedulingService;

    private MockAeroClient adminActor;
    private MockAeroClient userActor;

    private Project project;

    private User adminUser;

    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry aRegistry)
    {
        aRegistry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    @BeforeEach
    void setup() throws Exception
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN", "REMOTE");
        userActor = new MockAeroClient(context, "user", "USER", "REMOTE");

        adminUser = new User("admin", ROLE_ADMIN, ROLE_REMOTE);
        userRepository.create(adminUser);
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        project = Project.builder() //
                .withName("Test Project") //
                .build();
        projectService.createProject(project);
    }

    @Test
    void testListTasksEmpty() throws Exception
    {
        adminActor.listTasks(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages").isEmpty()) //
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    void testListTasks() throws Exception
    {
        var task = TestTask.builder() //
                .withProject(project) //
                .withSessionOwner(adminUser) //
                .build();
        schedulingService.enqueue(task);

        await().atMost(ofSeconds(3)).alias("Task started").untilAsserted(() -> {
            assertThat(schedulingService.getRunningTasks()).isNotEmpty();
        });

        adminActor.listTasks(project.getId()) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages").isEmpty()) //
                .andExpect(jsonPath("$.body[0].type").value("TestTask"));

        schedulingService.stopAllTasksForProject(project);

        await().atMost(ofSeconds(3)).alias("Task stopped").untilAsserted(() -> {
            assertThat(schedulingService.getRunningTasks()).isEmpty();
            assertThat(task.getMonitor().isDestroyed());
        });
    }

    @Test
    void testCancelNonExistingTask() throws Exception
    {
        adminActor.cancelTask(project.getId(), 12345) //
                .andExpect(status().isNotFound()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages").isNotEmpty());
    }

    @Test
    void testCancelTask() throws Exception
    {
        var task = TestTask.builder() //
                .withProject(project) //
                .withSessionOwner(adminUser) //
                .build();
        schedulingService.enqueue(task);

        await().atMost(ofSeconds(3)).alias("Task started").untilAsserted(() -> {
            assertThat(schedulingService.getRunningTasks()).isNotEmpty();
        });

        adminActor.cancelTask(project.getId(), task.getId()) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages[0].message").value("Task cancelled"));

        await().atMost(ofSeconds(3)).alias("Task destroyed").untilAsserted(() -> {
            assertThat(schedulingService.getRunningTasks()).isEmpty();
            assertThat(task.getMonitor().isDestroyed());
        });
    }

    static class TestTask
        extends Task
    {

        protected TestTask(Builder<? extends Builder<?>> aBuilder)
        {
            super(aBuilder);
        }

        @Override
        public void execute() throws Exception
        {
            while (!getMonitor().isCancelled()) {
                Thread.sleep(100);
            }
        }

        public static Builder<Builder<?>> builder()
        {
            return new Builder<>();
        }

        public static class Builder<T extends Builder<?>>
            extends Task.Builder<T>
        {
            protected Builder()
            {
                withCancellable(true);
                withType("TestTask");
                withTrigger("Test");
            }

            public TestTask build()
            {
                return new TestTask(this);
            }
        }
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config
    }
}
