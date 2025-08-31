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

import static de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl.DEFAULT_MAXIMUM_USERNAME_LENGTH;
import static de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl.DEFAULT_MINIMUM_PASSWORD_LENGTH;
import static de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl.DEFAULT_MINIMUM_USERNAME_LENGTH;
import static de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl.DEFAULT_PASSWORD_PATTERN;
import static de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityPropertiesImpl.DEFAULT_USERNAME_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.regex.Pattern;

import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
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

@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
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

    @BeforeEach
    void setup()
    {
        securityProperties.setUsernamePattern(Pattern.compile(DEFAULT_USERNAME_PATTERN));
        securityProperties.setPasswordPattern(Pattern.compile(DEFAULT_PASSWORD_PATTERN));
        securityProperties.setMinimumUsernameLength(DEFAULT_MINIMUM_USERNAME_LENGTH);
        securityProperties.setMaximumUsernameLength(DEFAULT_MAXIMUM_USERNAME_LENGTH);
        securityProperties.setMinimumPasswordLength(DEFAULT_MINIMUM_PASSWORD_LENGTH);
        securityProperties.setMaximumPasswordLength(DEFAULT_MINIMUM_PASSWORD_LENGTH);
        securityProperties.setSpaceAllowedInUsername(false);
    }

    @Test
    void thatTooShortAndTooLongPasswordsAreRejected()
    {
        securityProperties.setMinimumPasswordLength(2);
        securityProperties.setMaximumPasswordLength(3);
        assertThat(userDao.isValidPassword("")).isFalse();
        assertThat(userDao.isValidPassword("a")).isFalse();
        assertThat(userDao.isValidPassword("ab")).isTrue();
        assertThat(userDao.isValidPassword("abc")).isTrue();
        assertThat(userDao.isValidPassword("abcd")).isFalse();
    }

    @Test
    void thatPasswordPatternIsRespected()
    {
        securityProperties.setMinimumPasswordLength(0);
        securityProperties.setPasswordPattern(Pattern.compile("a.*z"));
        assertThat(userDao.isValidPassword("")).isFalse();
        assertThat(userDao.isValidPassword("a")).isFalse();
        assertThat(userDao.isValidPassword("az")).isTrue();
        assertThat(userDao.isValidPassword("a z")).isTrue();
        assertThat(userDao.isValidPassword("ay")).isFalse();
    }

    @Test
    void thatComplexPasswordPattern()
    {
        securityProperties.setMinimumPasswordLength(0);
        securityProperties.setPasswordPattern(
                Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*\\p{Punct}).*"));
        assertThat(userDao.isValidPassword("aaa")).isFalse();
        assertThat(userDao.isValidPassword("aBa")).isFalse();
        assertThat(userDao.isValidPassword("aB0")).isFalse();
        assertThat(userDao.isValidPassword("aB0.")).isTrue();
    }

    @Test
    void thatUsernamePatternIsRespected()
    {
        securityProperties.setMinimumUsernameLength(0);
        securityProperties.setUsernamePattern(Pattern.compile("a.*z"));
        assertThat(userDao.isValidUsername("")).isFalse();
        assertThat(userDao.isValidUsername("a")).isFalse();
        assertThat(userDao.isValidUsername("az")).isTrue();
        assertThat(userDao.isValidUsername("a131z")).isTrue();
        assertThat(userDao.isValidUsername("a z")).isFalse();
        assertThat(userDao.isValidUsername("ay")).isFalse();
    }

    @Test
    void thatAllowingSpaceInUsernameWorks()
    {
        securityProperties.setMinimumUsernameLength(0);

        securityProperties.setSpaceAllowedInUsername(false);
        assertThat(userDao.isValidUsername("a z")).isFalse();

        securityProperties.setSpaceAllowedInUsername(true);
        assertThat(userDao.isValidUsername("a z")).isTrue();
    }

    @Test
    void thatInvalidDefaultUserNameIsRejected()
    {
        securityProperties.setMinimumUsernameLength(1);
        securityProperties.setMaximumUsernameLength(2);
        securityProperties.setDefaultAdminUsername("blörg $[]");
        securityProperties.setDefaultAdminPassword("blah");

        assertThat(userDao.isValidUsername(securityProperties.getDefaultAdminUsername())).isFalse();
        assertThatIllegalStateException() //
                .isThrownBy(() -> userDao.installDefaultAdminUser());
    }

    @Test
    void testUsernameValidationErrorMessages()
    {
        assertThat(userDao.validateUsername("")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("empty");

        assertThat(userDao.validateUsername(" ")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("empty");

        assertThat(userDao.validateUsername(" john")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain whitespace");

        assertThat(userDao.validateUsername("john ")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain whitespace");

        assertThat(userDao.validateUsername("jo hn")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain whitespace");

        assertThat(userDao.validateUsername("jo\thn")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain whitespace");

        assertThat(userDao.validateUsername("john\n")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain whitespace");

        assertThat(userDao.validateUsername("john\0")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("contain any of these characters");

        assertThat(userDao.validateUsername("john\u001B")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("control characters");

        assertThat(userDao.validateUsername("loveme".repeat(2000))) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("too long");

        assertThat(userDao.validateUsername("/etc/passwd")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("contain any of these characters");

        assertThat(userDao.validateUsername("../../bomb.zip")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start with any of these characters");

        assertThat(userDao.validateUsername(".hidden")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start with any of these characters");
    }

    @Test
    void thatTooShortAndTooLongUsernamesAreRejected()
    {
        securityProperties.setMinimumUsernameLength(2);
        securityProperties.setMaximumUsernameLength(3);
        assertThat(userDao.isValidUsername("")).isFalse();
        assertThat(userDao.isValidUsername("a")).isFalse();
        assertThat(userDao.isValidUsername("ab")).isTrue();
        assertThat(userDao.isValidUsername("abc")).isTrue();
        assertThat(userDao.isValidUsername("abcd")).isFalse();
    }

    @Test
    void thatEmptyUsernamesAre()
    {
        securityProperties.setMinimumUsernameLength(0);
        securityProperties.setMaximumUsernameLength(2);
        assertThat(userDao.isValidUsername("")).isFalse();
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

    @Test
    void thatRealmsCanBeListed()
    {
        userDao.create(User.builder() //
                .withUsername("user1") //
                .withRealm(Realm.local()) //
                .build());

        userDao.create(User.builder() //
                .withUsername("user2") //
                .withRealm(Realm.local()) //
                .build());

        userDao.create(User.builder() //
                .withUsername("user3") //
                .withRealm(new Realm(Realm.REALM_EXTERNAL_PREFIX + "client", "My SSO")) //
                .build());

        assertThat(userDao.listRealms()) //
                .contains(null, "external:client");
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
