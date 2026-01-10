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

import wicket.contrib.input.events.key.KeyType;

/**
 * Configuration properties for keyboard shortcuts in the annotation UI.
 */
public interface KeyBindingsProperties
{
    NavigationShortcuts getNavigation();

    EditingShortcuts getEditing();

    AnchoringModeShortcuts getAnchoringMode();

    DialogShortcuts getDialog();

    /**
     * Navigation-related keyboard shortcuts.
     */
    interface NavigationShortcuts
    {
        KeyType[] getNextPage();

        KeyType[] getPreviousPage();

        KeyType[] getFirstPage();

        KeyType[] getLastPage();

        KeyType[] getNextDocument();

        KeyType[] getPreviousDocument();

        KeyType[] getNextAnnotation();

        KeyType[] getPreviousAnnotation();
    }

    /**
     * Editing-related keyboard shortcuts.
     */
    interface EditingShortcuts
    {
        KeyType[] getUndo();

        KeyType[] getRedo();

        KeyType[] getDeleteAnnotation();

        KeyType[] getClearSelection();

        KeyType[] getToggleSelection();
    }

    /**
     * Anchoring mode selection shortcuts.
     */
    interface AnchoringModeShortcuts
    {
        KeyType[] getCharacters();

        KeyType[] getSingleToken();

        KeyType[] getTokens();

        KeyType[] getSentences();
    }

    /**
     * Dialog-related keyboard shortcuts.
     */
    interface DialogShortcuts
    {
        KeyType[] getCloseDialog();
    }
}
