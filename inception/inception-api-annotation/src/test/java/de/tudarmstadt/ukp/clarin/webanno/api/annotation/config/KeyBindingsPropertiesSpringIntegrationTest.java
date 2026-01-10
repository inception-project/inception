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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Shift;
import static wicket.contrib.input.events.key.KeyType.z;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import wicket.contrib.input.events.key.KeyType;

/**
 * Integration test to verify that Spring Boot can properly bind configuration properties to KeyType
 * arrays using the StringToEnumArrayConverter.
 */
@SpringBootTest(classes = KeyBindingsPropertiesSpringIntegrationTest.TestConfig.class)
@TestPropertySource(properties = { //
        "ui.keybindings.navigation.nextPage.keys=Page_down", //
        "ui.keybindings.navigation.previousPage.keys=Page_up", //
        "ui.keybindings.editing.undo.keys=Ctrl,z", //
        "ui.keybindings.editing.undo.disabled-in-inputs=true", //
        "ui.keybindings.editing.redo.keys=Ctrl,y" })
class KeyBindingsPropertiesSpringIntegrationTest
{
    @Configuration
    @EnableConfigurationProperties(KeyBindingsPropertiesImpl.class)
    static class TestConfig
    {
        @Bean
        public StringToEnumArrayConverter stringToEnumArrayConverter()
        {
            return new StringToEnumArrayConverter();
        }
    }

    private @Autowired KeyBindingsProperties keyBindings;

    @Test
    void testSpringBootInjection()
    {
        // Verify that Spring can inject the KeyBindingsProperties bean
        assertThat(keyBindings).isNotNull();
        assertThat(keyBindings).isInstanceOf(KeyBindingsProperties.class);
    }

    @Test
    void testDefaultKeyTypeArraysAreAccessible()
    {
        // Test that KeyType[] arrays can be retrieved from the properties
        var nextPage = keyBindings.getNavigation().getNextPage();
        assertThat(nextPage).isNotNull();
        assertThat(nextPage.getKeys()).isInstanceOf(KeyType[].class);
        assertThat(nextPage.isDisabledInInputs()).isFalse();
        assertThat(nextPage.getKeys()).containsExactly(Page_down);

        var undo = keyBindings.getEditing().getUndo();
        assertThat(undo).isNotNull();
        assertThat(undo.getKeys()).isInstanceOf(KeyType[].class);
        assertThat(undo.isDisabledInInputs()).isTrue();
        assertThat(undo.getKeys()).containsExactly(Ctrl, z);
    }

    @Test
    void testNestedPropertiesAreAccessible()
    {
        // Verify that all nested property categories are accessible
        assertThat(keyBindings.getNavigation()).isNotNull();
        assertThat(keyBindings.getEditing()).isNotNull();
        assertThat(keyBindings.getAnchoringMode()).isNotNull();
        assertThat(keyBindings.getDialog()).isNotNull();
    }

    @Test
    void testSingleKeyTypeConversion()
    {
        // Test that single KeyType values from properties are correctly converted
        var nextPage = keyBindings.getNavigation().getNextPage().getKeys();
        assertThat(nextPage).containsExactly(Page_down);

        var previousPage = keyBindings.getNavigation().getPreviousPage().getKeys();
        assertThat(previousPage).containsExactly(KeyType.Page_up);
    }

    @Test
    void testCommaSeparatedKeyTypeConversion()
    {
        // Test that comma-separated KeyType combinations are correctly converted
        var undo = keyBindings.getEditing().getUndo().getKeys();
        assertThat(undo).containsExactly(Ctrl, z);

        var redo = keyBindings.getEditing().getRedo().getKeys();
        assertThat(redo).containsExactly(Ctrl, KeyType.y);
    }

    @Test
    void testConfiguredPropertiesOverrideDefaults()
    {
        // Verify that configured properties override the hardcoded defaults
        // Default redo is Shift+Ctrl+z, but we've configured it as Ctrl+y
        var redo = keyBindings.getEditing().getRedo().getKeys();
        assertThat(redo).doesNotContain(Shift);
        assertThat(redo).containsExactly(Ctrl, KeyType.y);
    }
}
