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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration.LEGACY_REMOTE_API_ENABLED_CONDITION;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration.REMOTE_API_ENABLED_CONDITION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures that the deprecated legacy remote API is not switched on by merely enabling the remote
 * API - it requires the additional {@code remote-api.legacy.enabled} opt-in.
 */
class RemoteApiAutoConfigurationConditionsTest
{
    private static final String AERO_MARKER = "aeroMarker";
    private static final String LEGACY_MARKER = "legacyMarker";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void thatNothingIsEnabledByDefault()
    {
        contextRunner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(AERO_MARKER);
            assertThat(ctx).doesNotHaveBean(LEGACY_MARKER);
        });
    }

    @Test
    void thatEnablingRemoteApiDoesNotEnableLegacyApi()
    {
        contextRunner.withPropertyValues("remote-api.enabled=true").run(ctx -> {
            assertThat(ctx).hasBean(AERO_MARKER);
            assertThat(ctx).doesNotHaveBean(LEGACY_MARKER);
        });
    }

    @Test
    void thatLegacyApiRequiresRemoteApiToBeEnabled()
    {
        contextRunner.withPropertyValues("remote-api.legacy.enabled=true").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(AERO_MARKER);
            assertThat(ctx).doesNotHaveBean(LEGACY_MARKER);
        });
    }

    @Test
    void thatLegacyApiCanBeEnabledExplicitly()
    {
        contextRunner
                .withPropertyValues("remote-api.enabled=true", "remote-api.legacy.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean(AERO_MARKER);
                    assertThat(ctx).hasBean(LEGACY_MARKER);
                });
    }

    @Test
    void thatLegacyApiIsAlsoAvailableViaTheLegacyWebAnnoSwitch()
    {
        contextRunner.withPropertyValues("webanno.remote-api.enable=true",
                "remote-api.legacy.enabled=true").run(ctx -> {
                    assertThat(ctx).hasBean(AERO_MARKER);
                    assertThat(ctx).hasBean(LEGACY_MARKER);
                });
    }

    /**
     * Mirrors the conditions used by {@link RemoteApiAutoConfiguration} on marker beans so that the
     * conditions can be exercised without having to provide all the collaborators required by the
     * actual controllers.
     */
    @Configuration(proxyBeanMethods = false)
    static class TestConfig
    {
        @ConditionalOnExpression(REMOTE_API_ENABLED_CONDITION)
        @Bean(AERO_MARKER)
        Object aeroMarker()
        {
            return new Object();
        }

        @ConditionalOnExpression(LEGACY_REMOTE_API_ENABLED_CONDITION)
        @Bean(LEGACY_MARKER)
        Object legacyMarker()
        {
            return new Object();
        }
    }
}
