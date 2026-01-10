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
import static wicket.contrib.input.events.key.KeyType.Delete;
import static wicket.contrib.input.events.key.KeyType.Escape;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Shift;
import static wicket.contrib.input.events.key.KeyType.one;
import static wicket.contrib.input.events.key.KeyType.z;

import org.junit.jupiter.api.Test;

import wicket.contrib.input.events.key.KeyType;

class KeyBindingsPropertiesImplTest
{
    @Test
    void testDefaultNavigationShortcuts()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Test that default navigation shortcuts are properly configured
        KeyType[] nextPage = properties.getNavigation().getNextPage();
        assertThat(nextPage).containsExactly(Page_down);

        KeyType[] previousAnnotation = properties.getNavigation().getPreviousAnnotation();
        assertThat(previousAnnotation).containsExactly(Shift, KeyType.Left);
    }

    @Test
    void testDefaultEditingShortcuts()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Test that default editing shortcuts are properly configured
        KeyType[] undo = properties.getEditing().getUndo();
        assertThat(undo).containsExactly(Ctrl, z);

        KeyType[] deleteAnnotation = properties.getEditing().getDeleteAnnotation();
        assertThat(deleteAnnotation).containsExactly(Shift, Delete);
    }

    @Test
    void testDefaultAnchoringModeShortcuts()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Test that default anchoring mode shortcuts are properly configured
        KeyType[] characters = properties.getAnchoringMode().getCharacters();
        assertThat(characters).containsExactly(Shift, one);
    }

    @Test
    void testDefaultDialogShortcuts()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Test that default dialog shortcuts are properly configured
        KeyType[] closeDialog = properties.getDialog().getCloseDialog();
        assertThat(closeDialog).containsExactly(Escape);
    }

    @Test
    void testCustomShortcutsCanBeSet()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Test that custom shortcuts can be set and retrieved
        KeyType[] customUndo = new KeyType[] { Ctrl, KeyType.u };
        var editingImpl = (KeyBindingsPropertiesImpl.EditingShortcutsImpl) properties.getEditing();
        editingImpl.setUndo(customUndo);

        KeyType[] retrieved = properties.getEditing().getUndo();
        assertThat(retrieved).isSameAs(customUndo);
        assertThat(retrieved).containsExactly(Ctrl, KeyType.u);
    }

    @Test
    void testKeyTypeArraysAreReturned()
    {
        var properties = new KeyBindingsPropertiesImpl();

        // Verify that the returned objects are indeed KeyType arrays
        Object nextPage = properties.getNavigation().getNextPage();
        assertThat(nextPage).isInstanceOf(KeyType[].class);

        Object undo = properties.getEditing().getUndo();
        assertThat(undo).isInstanceOf(KeyType[].class);
    }
}
