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
package de.tudarmstadt.ukp.inception.sharing;

import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_IN_PROGRESS;
import static java.util.Calendar.YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" }, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class InviteServiceImplTest
{
    private InviteService sut;

    private @Autowired TestEntityManager testEntityManager;

    private ProjectService projectService;
    private WorkloadManagementService workloadManagementService;
    private WorkloadManagerExtension<?> workloadManagerExtension;

    private Project testProject;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BeforeEach
    public void setUp() throws Exception
    {
        testProject = new Project("test-project");
        testProject.setState(ANNOTATION_IN_PROGRESS);
        testEntityManager.persist(testProject);

        projectService = mock(ProjectService.class);
        when(projectService.getProject(any(Long.class))).thenReturn(testProject);

        workloadManagerExtension = mock(WorkloadManagerExtension.class);
        when(workloadManagerExtension.freshenStatus(any())).thenAnswer(_call -> {
            Project project = (Project) _call.getArgument(0);
            return project.getState();
        });

        workloadManagementService = mock(WorkloadManagementService.class);
        when(workloadManagementService.getWorkloadManagerExtension(any()))
                .thenReturn((WorkloadManagerExtension) workloadManagerExtension);

        sut = new InviteServiceImpl(null, projectService, null, workloadManagementService,
                testEntityManager.getEntityManager());
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.inception.sharing.model",
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    public static class SpringConfig
    {
        // No content
    }

    @Test
    public void createInviteId_ShouldReturnId()
    {
        String urlBase64Chars = "[0-9A-Za-z\\-_]+";

        String inviteId = sut.generateInviteID(testProject);
        assertThat(inviteId).matches(urlBase64Chars).hasSize(22);
        String inviteId2 = sut.generateInviteID(testProject);
        assertThat(inviteId2).matches(urlBase64Chars).hasSize(22).isNotEqualTo(inviteId);
    }

    @Test
    public void isValidInviteId_ShouldReturnTrue()
    {
        sut.generateInviteID(testProject);
        String retrievedId = sut.getValidInviteID(testProject);

        assertThat(sut.isValidInviteLink(testProject, retrievedId)).isTrue();
    }

    @Test
    public void isValidInviteIdOfInvalidId_ShouldReturnFalse()
    {
        assertThat(sut.isValidInviteLink(testProject, "notValid")).isFalse();
    }

    @Test
    public void getValidInviteId_ShouldReturnCreatedId()
    {
        String inviteId = sut.generateInviteID(testProject);
        String retrievedId = sut.getValidInviteID(testProject);

        assertThat(retrievedId).isEqualTo(inviteId);
    }

    @Test
    public void getInvalidInviteId_ShouldReturnNull()
    {
        // get expired date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.YEAR, -1);
        Date expirationDate = calendar.getTime();
        testEntityManager.persist(new ProjectInvite(testProject, "testId", expirationDate));

        String retrievedId = sut.getValidInviteID(testProject);

        assertThat(retrievedId).isNull();
    }

    @Test
    public void getDeletedInviteId_ShouldReturnNull()
    {
        sut.generateInviteID(testProject);
        sut.removeInviteID(testProject);

        String retrievedId = sut.getValidInviteID(testProject);

        assertThat(retrievedId).isNull();
    }

    @Test
    public void extendExpirationDate_ShouldReturnDateInAYear()
    {
        sut.generateInviteID(testProject);
        Date oldDate = sut.getExpirationDate(testProject);
        Calendar oldCalendar = Calendar.getInstance();
        oldCalendar.setTime(oldDate);

        sut.extendInviteLinkDate(testProject);

        Date newDate = sut.getExpirationDate(testProject);
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTime(newDate);
        assertThat(newCalendar.get(YEAR) - oldCalendar.get(YEAR)).isEqualTo(1);
    }

    @Test
    public void generateInviteWithExpirationDate_ShouldReturnSpecificDate() throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // Note: test fails once this date has passed - it must be a future date!
        Date expectedDate = dateFormat.parse("2066-01-15");
        sut.generateInviteWithExpirationDate(testProject, expectedDate);

        Date generatedDate = sut.getExpirationDate(testProject);
        assertThat(generatedDate).isEqualTo(expectedDate);
    }
}
