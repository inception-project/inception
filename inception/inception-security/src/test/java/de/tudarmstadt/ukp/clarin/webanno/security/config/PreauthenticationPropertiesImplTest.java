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
package de.tudarmstadt.ukp.clarin.webanno.security.config;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

class PreauthenticationPropertiesImplTest
{
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    private final User dummyUser = User.builder().withUsername("testuser").build();

    @Test
    void thatDefaultRolesContainOnlyRoleUser()
    {
        contextRunner.run(ctx -> {
            var props = ctx.getBean(PreauthenticationPropertiesImpl.class);
            assertThat(props.getNewUserRoles(dummyUser)).containsExactly(ROLE_USER);
        });
    }

    @Test
    void thatCsvRolesAreParsed()
    {
        contextRunner.withPropertyValues("auth.preauth.newuser.roles=ROLE_USER,ROLE_ADMIN")
                .run(ctx -> {
                    var props = ctx.getBean(PreauthenticationPropertiesImpl.class);
                    assertThat(props.getNewUserRoles(dummyUser))
                            .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN);
                });
    }

    @Test
    void thatIndexedRolesAreParsed()
    {
        contextRunner.withPropertyValues("auth.preauth.newuser.roles[0]=ROLE_USER",
                "auth.preauth.newuser.roles[1]=ROLE_ADMIN").run(ctx -> {
                    var props = ctx.getBean(PreauthenticationPropertiesImpl.class);
                    assertThat(props.getNewUserRoles(dummyUser))
                            .containsExactlyInAnyOrder(ROLE_USER, ROLE_ADMIN);
                });
    }

    @Test
    void thatUnknownRoleNamesAreIgnored()
    {
        contextRunner.withPropertyValues("auth.preauth.newuser.roles=ROLE_USER,ROLE_DOES_NOT_EXIST")
                .run(ctx -> {
                    var props = ctx.getBean(PreauthenticationPropertiesImpl.class);
                    assertThat(props.getNewUserRoles(dummyUser)).containsExactly(ROLE_USER);
                });
    }

    @EnableConfigurationProperties(PreauthenticationPropertiesImpl.class)
    static class TestConfig
    {
        // Minimal context: just the properties bean under test
    }
}
