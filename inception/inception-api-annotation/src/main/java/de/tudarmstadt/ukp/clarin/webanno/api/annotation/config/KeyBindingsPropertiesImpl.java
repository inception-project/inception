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

import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.Delete;
import static wicket.contrib.input.events.key.KeyType.End;
import static wicket.contrib.input.events.key.KeyType.Escape;
import static wicket.contrib.input.events.key.KeyType.Home;
import static wicket.contrib.input.events.key.KeyType.Left;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Right;
import static wicket.contrib.input.events.key.KeyType.Shift;
import static wicket.contrib.input.events.key.KeyType.Space;
import static wicket.contrib.input.events.key.KeyType.four;
import static wicket.contrib.input.events.key.KeyType.one;
import static wicket.contrib.input.events.key.KeyType.three;
import static wicket.contrib.input.events.key.KeyType.two;
import static wicket.contrib.input.events.key.KeyType.z;

import org.springframework.boot.context.properties.ConfigurationProperties;

import wicket.contrib.input.events.key.KeyType;

/**
 * Implementation of keyboard shortcuts configuration with default values matching the current
 * hard-coded shortcuts.
 */
@ConfigurationProperties("ui.keybindings")
public class KeyBindingsPropertiesImpl
    implements KeyBindingsProperties
{
    private final NavigationShortcutsImpl navigation = new NavigationShortcutsImpl();
    private final EditingShortcutsImpl editing = new EditingShortcutsImpl();
    private final AnchoringModeShortcutsImpl anchoringMode = new AnchoringModeShortcutsImpl();
    private final DialogShortcutsImpl dialog = new DialogShortcutsImpl();

    @Override
    public NavigationShortcuts getNavigation()
    {
        return navigation;
    }

    @Override
    public EditingShortcuts getEditing()
    {
        return editing;
    }

    @Override
    public AnchoringModeShortcuts getAnchoringMode()
    {
        return anchoringMode;
    }

    @Override
    public DialogShortcuts getDialog()
    {
        return dialog;
    }

    public static class NavigationShortcutsImpl
        implements NavigationShortcuts
    {
        private KeyType[] nextPage = new KeyType[] { Page_down };
        private KeyType[] previousPage = new KeyType[] { Page_up };
        private KeyType[] firstPage = new KeyType[] { Home };
        private KeyType[] lastPage = new KeyType[] { End };
        private KeyType[] nextDocument = new KeyType[] { Shift, Page_down };
        private KeyType[] previousDocument = new KeyType[] { Shift, Page_up };
        private KeyType[] nextAnnotation = new KeyType[] { Shift, Right };
        private KeyType[] previousAnnotation = new KeyType[] { Shift, Left };

        @Override
        public KeyType[] getNextPage()
        {
            return nextPage;
        }

        public void setNextPage(KeyType[] aNextPage)
        {
            nextPage = aNextPage;
        }

        @Override
        public KeyType[] getPreviousPage()
        {
            return previousPage;
        }

        public void setPreviousPage(KeyType[] aPreviousPage)
        {
            previousPage = aPreviousPage;
        }

        @Override
        public KeyType[] getFirstPage()
        {
            return firstPage;
        }

        public void setFirstPage(KeyType[] aFirstPage)
        {
            firstPage = aFirstPage;
        }

        @Override
        public KeyType[] getLastPage()
        {
            return lastPage;
        }

        public void setLastPage(KeyType[] aLastPage)
        {
            lastPage = aLastPage;
        }

        @Override
        public KeyType[] getNextDocument()
        {
            return nextDocument;
        }

        public void setNextDocument(KeyType[] aNextDocument)
        {
            nextDocument = aNextDocument;
        }

        @Override
        public KeyType[] getPreviousDocument()
        {
            return previousDocument;
        }

        public void setPreviousDocument(KeyType[] aPreviousDocument)
        {
            previousDocument = aPreviousDocument;
        }

        @Override
        public KeyType[] getNextAnnotation()
        {
            return nextAnnotation;
        }

        public void setNextAnnotation(KeyType[] aNextAnnotation)
        {
            nextAnnotation = aNextAnnotation;
        }

        @Override
        public KeyType[] getPreviousAnnotation()
        {
            return previousAnnotation;
        }

        public void setPreviousAnnotation(KeyType[] aPreviousAnnotation)
        {
            previousAnnotation = aPreviousAnnotation;
        }
    }

    public static class EditingShortcutsImpl
        implements EditingShortcuts
    {
        private KeyType[] undo = new KeyType[] { Ctrl, z };
        private KeyType[] redo = new KeyType[] { Shift, Ctrl, z };
        private KeyType[] deleteAnnotation = new KeyType[] { Shift, Delete };
        private KeyType[] clearSelection = new KeyType[] { Shift, Escape };
        private KeyType[] toggleSelection = new KeyType[] { Shift, Space };

        @Override
        public KeyType[] getUndo()
        {
            return undo;
        }

        public void setUndo(KeyType[] aUndo)
        {
            undo = aUndo;
        }

        @Override
        public KeyType[] getRedo()
        {
            return redo;
        }

        public void setRedo(KeyType[] aRedo)
        {
            redo = aRedo;
        }

        @Override
        public KeyType[] getDeleteAnnotation()
        {
            return deleteAnnotation;
        }

        public void setDeleteAnnotation(KeyType[] aDeleteAnnotation)
        {
            deleteAnnotation = aDeleteAnnotation;
        }

        @Override
        public KeyType[] getClearSelection()
        {
            return clearSelection;
        }

        public void setClearSelection(KeyType[] aClearSelection)
        {
            clearSelection = aClearSelection;
        }

        @Override
        public KeyType[] getToggleSelection()
        {
            return toggleSelection;
        }

        public void setToggleSelection(KeyType[] aToggleSelection)
        {
            toggleSelection = aToggleSelection;
        }
    }

    public static class AnchoringModeShortcutsImpl
        implements AnchoringModeShortcuts
    {
        private KeyType[] characters = new KeyType[] { Shift, one };
        private KeyType[] singleToken = new KeyType[] { Shift, two };
        private KeyType[] tokens = new KeyType[] { Shift, three };
        private KeyType[] sentences = new KeyType[] { Shift, four };

        @Override
        public KeyType[] getCharacters()
        {
            return characters;
        }

        public void setCharacters(KeyType[] aCharacters)
        {
            characters = aCharacters;
        }

        @Override
        public KeyType[] getSingleToken()
        {
            return singleToken;
        }

        public void setSingleToken(KeyType[] aSingleToken)
        {
            singleToken = aSingleToken;
        }

        @Override
        public KeyType[] getTokens()
        {
            return tokens;
        }

        public void setTokens(KeyType[] aTokens)
        {
            tokens = aTokens;
        }

        @Override
        public KeyType[] getSentences()
        {
            return sentences;
        }

        public void setSentences(KeyType[] aSentences)
        {
            sentences = aSentences;
        }
    }

    public static class DialogShortcutsImpl
        implements DialogShortcuts
    {
        private KeyType[] closeDialog = new KeyType[] { Escape };

        @Override
        public KeyType[] getCloseDialog()
        {
            return closeDialog;
        }

        public void setCloseDialog(KeyType[] aCloseDialog)
        {
            closeDialog = aCloseDialog;
        }
    }
}
