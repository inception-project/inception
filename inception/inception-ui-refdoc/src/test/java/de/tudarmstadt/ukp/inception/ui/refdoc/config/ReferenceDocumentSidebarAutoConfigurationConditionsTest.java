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
package de.tudarmstadt.ukp.inception.ui.refdoc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import de.tudarmstadt.ukp.inception.ui.refdoc.ReferenceDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.inception.ui.refdoc.ReferenceDocumentSidebarFactory;

/**
 * Ensures the reference document sidebar stays behind its feature flag: it is an opt-in
 * experimental feature, so nothing it contributes may reach the UI unless
 * {@code ui.reference-document.enabled} is set.
 */
class ReferenceDocumentSidebarAutoConfigurationConditionsTest
{
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(ReferenceDocumentSidebarAutoConfiguration.class));

    @Test
    void thatSidebarIsDisabledByDefault()
    {
        contextRunner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ReferenceDocumentSidebarFactory.class);
            assertThat(ctx).doesNotHaveBean(ReferenceDocumentNavigatorActionBarExtension.class);
        });
    }

    @Test
    void thatSidebarIsDisabledWhenFlagIsFalse()
    {
        contextRunner.withPropertyValues("ui.reference-document.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ReferenceDocumentSidebarFactory.class);
            assertThat(ctx).doesNotHaveBean(ReferenceDocumentNavigatorActionBarExtension.class);
        });
    }

    @Test
    void thatSidebarAndNavigatorAreEnabledTogether()
    {
        contextRunner.withPropertyValues("ui.reference-document.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ReferenceDocumentSidebarFactory.class);
            assertThat(ctx).hasSingleBean(ReferenceDocumentNavigatorActionBarExtension.class);
        });
    }
}
