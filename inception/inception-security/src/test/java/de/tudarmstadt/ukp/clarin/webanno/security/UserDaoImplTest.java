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
package de.tudarmstadt.ukp.clarin.webanno.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@DataJpaTest(showSql = false)
@EnableAutoConfiguration
@ImportAutoConfiguration( //
        exclude = { LiquibaseAutoConfiguration.class }, //
        classes = { SecurityAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class UserDaoImplTest
{
    @Autowired
    SecurityPropertiesImpl securityProperties;

    @Autowired
    UserDaoImpl userDao;

    @Test
    void thatInvalidDefaultUserNameIsRejected()
    {
        securityProperties.setDefaultAdminUsername("blörg $[]");
        securityProperties.setDefaultAdminPassword("blah");

        assertThat(NameUtil.isNameValidUserName(securityProperties.getDefaultAdminUsername()))
                .isFalse();
        assertThatIllegalStateException() //
                .isThrownBy(() -> userDao.installDefaultAdminUser());
    }

    @Test
    void thatCustomAdminUserIsCreated()
    {
        var username = "overlord";
        var password = "super-secret";
        securityProperties.setDefaultAdminUsername(username);
        securityProperties.setDefaultAdminPassword(password);

        userDao.installDefaultAdminUser();

        User user = userDao.get(username);
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    void thatCustomAdminUserIsCreatedWithDefaultUsername()
    {
        var username = "admin";
        var password = "super-secret";
        securityProperties.setDefaultAdminUsername(null);
        securityProperties.setDefaultAdminPassword(password);

        userDao.installDefaultAdminUser();

        User user = userDao.get(username);
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    void thatCustomAdminUserIsOnlyCreatedWhenPasswordIsSet()
    {
        var username = "overlord";
        securityProperties.setDefaultAdminUsername(username);
        securityProperties.setDefaultAdminPassword(null);

        userDao.installDefaultAdminUser();

        assertThat(userDao.get(username)).isNull();
        assertThat(userDao.get("admin")).isNull();
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
