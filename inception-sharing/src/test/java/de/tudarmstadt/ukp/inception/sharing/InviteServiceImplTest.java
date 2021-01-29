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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@RunWith(SpringRunner.class)
@DataJpaTest
public class InviteServiceImplTest
{
    private InviteService sut;
    private @Autowired TestEntityManager testEntityManager;
    
    private Project testProject;
    
    @Before
    public void setUp() throws Exception
    {
        sut = new InviteServiceImpl(testEntityManager.getEntityManager());
        testProject = new Project("testProject");
        testEntityManager.persist(testProject);
    }

    @After
    public void tearDown()
    {
        testEntityManager.clear();
    }
    
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.inception.sharing",
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    public static class SpringConfig
    {
        // No content
    }
    
    @Test
    public void createInviteId_ShouldReturnId() {
        String urlBase64Chars = "[0-9A-Za-z\\-_]+";
        
        String inviteId = sut.generateInviteID(testProject);
        assertThat(inviteId).matches(urlBase64Chars).hasSize(22);
        String inviteId2 = sut.generateInviteID(testProject);
        assertThat(inviteId2).matches(urlBase64Chars).hasSize(22).isNotEqualTo(inviteId);
    }
    
    @Test
    public void isValidInviteId_ShouldReturnTrue() {
        sut.generateInviteID(testProject);
        String retrievedId = sut.getValidInviteID(testProject);
        
        assertThat(sut.isValidInviteLink(testProject, retrievedId)).isTrue();
    }
    
    @Test
    public void isValidInviteIdOfInvalidId_ShouldReturnFalse() {
        assertThat(sut.isValidInviteLink(testProject, "notValid")).isFalse();
    }
    
    @Test
    public void getValidInviteId_ShouldReturnCreatedId() {
        String inviteId = sut.generateInviteID(testProject);
        String retrievedId = sut.getValidInviteID(testProject);
        
        assertThat(retrievedId).isEqualTo(inviteId);
    }
    
    @Test
    public void getInvalidInviteId_ShouldReturnNull() {
        // get expired date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.YEAR, -1);
        long expirationDate = calendar.getTime().getTime();
        testEntityManager.persist(new ProjectInvite(testProject, "testId", expirationDate));
        
        String retrievedId = sut.getValidInviteID(testProject);
        
        assertThat(retrievedId).isNull();
    }
    
    @Test
    public void getDeletedInviteId_ShouldReturnNull() {
        sut.generateInviteID(testProject);
        sut.removeInviteID(testProject);
        
        String retrievedId = sut.getValidInviteID(testProject);
        
        assertThat(retrievedId).isNull();
    }
}
