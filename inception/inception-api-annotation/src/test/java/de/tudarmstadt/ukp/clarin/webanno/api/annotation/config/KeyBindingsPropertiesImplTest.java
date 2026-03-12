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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import wicket.contrib.input.events.key.KeyType;

class KeyBindingsPropertiesImplTest
{
    private KeyBindingsProperties sut;

    @BeforeEach
    void setup()
    {
        sut = new KeyBindingsPropertiesImpl();
    }

    @Test
    void testDefaultNavigationShortcuts()
    {
        var nextPage = sut.getNavigation().getNextPage().getKeys();
        assertThat(nextPage).containsExactly(Page_down);

        var previousAnnotation = sut.getNavigation().getPreviousAnnotation().getKeys();
        assertThat(previousAnnotation).containsExactly(Shift, KeyType.Left);
    }

    @Test
    void testDefaultEditingShortcuts()
    {
        var undo = sut.getEditing().getUndo().getKeys();
        assertThat(undo).containsExactly(Ctrl, z);

        var deleteAnnotation = sut.getEditing().getDeleteAnnotation().getKeys();
        assertThat(deleteAnnotation).containsExactly(Shift, Delete);
    }

    @Test
    void testDefaultAnchoringModeShortcuts()
    {
        var characters = sut.getAnchoringMode().getCharacters().getKeys();
        assertThat(characters).containsExactly(Shift, one);
    }

    @Test
    void testDefaultDialogShortcuts()
    {
        var closeDialog = sut.getDialog().getCloseDialog().getKeys();
        assertThat(closeDialog).containsExactly(Escape);
    }

    @Test
    void testCustomShortcutsCanBeSet()
    {
        // Test that custom shortcuts can be set and retrieved
        var customUndo = new KeyCombo(Ctrl, KeyType.u);
        var editingImpl = (KeyBindingsPropertiesImpl.EditingShortcutsImpl) sut.getEditing();
        editingImpl.setUndo(customUndo);

        var retrieved = sut.getEditing().getUndo();
        assertThat(retrieved).isSameAs(customUndo);
        assertThat(retrieved.getKeys()).containsExactly(Ctrl, KeyType.u);
    }

    @Test
    void testKeyTypeArraysAreReturned()
    {
        // Verify that the returned objects are indeed KeyType arrays
        var nextPage = sut.getNavigation().getNextPage().getKeys();
        assertThat(nextPage).isInstanceOf(KeyType[].class);

        var undo = sut.getEditing().getUndo().getKeys();
        assertThat(undo).isInstanceOf(KeyType[].class);
    }
}
