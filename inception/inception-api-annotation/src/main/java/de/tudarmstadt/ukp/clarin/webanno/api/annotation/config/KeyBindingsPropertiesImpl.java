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
        private KeyCombo nextPage = new KeyCombo(Page_down);
        private KeyCombo previousPage = new KeyCombo(Page_up);
        private KeyCombo firstPage = new KeyCombo(Home);
        private KeyCombo lastPage = new KeyCombo(End);
        private KeyCombo nextDocument = new KeyCombo(Shift, Page_down);
        private KeyCombo previousDocument = new KeyCombo(Shift, Page_up);
        private KeyCombo nextAnnotation = new KeyCombo(Shift, Right);
        private KeyCombo previousAnnotation = new KeyCombo(Shift, Left);

        @Override
        public KeyCombo getNextPage()
        {
            return nextPage;
        }

        public void setNextPage(KeyCombo aNextPage)
        {
            nextPage = aNextPage;
        }

        @Override
        public KeyCombo getPreviousPage()
        {
            return previousPage;
        }

        public void setPreviousPage(KeyCombo aPreviousPage)
        {
            previousPage = aPreviousPage;
        }

        @Override
        public KeyCombo getFirstPage()
        {
            return firstPage;
        }

        public void setFirstPage(KeyCombo aFirstPage)
        {
            firstPage = aFirstPage;
        }

        @Override
        public KeyCombo getLastPage()
        {
            return lastPage;
        }

        public void setLastPage(KeyCombo aLastPage)
        {
            lastPage = aLastPage;
        }

        @Override
        public KeyCombo getNextDocument()
        {
            return nextDocument;
        }

        public void setNextDocument(KeyCombo aNextDocument)
        {
            nextDocument = aNextDocument;
        }

        @Override
        public KeyCombo getPreviousDocument()
        {
            return previousDocument;
        }

        public void setPreviousDocument(KeyCombo aPreviousDocument)
        {
            previousDocument = aPreviousDocument;
        }

        @Override
        public KeyCombo getNextAnnotation()
        {
            return nextAnnotation;
        }

        public void setNextAnnotation(KeyCombo aNextAnnotation)
        {
            nextAnnotation = aNextAnnotation;
        }

        @Override
        public KeyCombo getPreviousAnnotation()
        {
            return previousAnnotation;
        }

        public void setPreviousAnnotation(KeyCombo aPreviousAnnotation)
        {
            previousAnnotation = aPreviousAnnotation;
        }
    }

    public static class EditingShortcutsImpl
        implements EditingShortcuts
    {
        private KeyCombo undo = new KeyCombo(Ctrl, z);
        private KeyCombo redo = new KeyCombo(Shift, Ctrl, z);
        private KeyCombo deleteAnnotation = new KeyCombo(Shift, Delete);
        private KeyCombo clearSelection = new KeyCombo(Shift, Escape);
        private KeyCombo toggleSelection = new KeyCombo(Shift, Space);

        @Override
        public KeyCombo getUndo()
        {
            return undo;
        }

        public void setUndo(KeyCombo aUndo)
        {
            undo = aUndo;
        }

        @Override
        public KeyCombo getRedo()
        {
            return redo;
        }

        public void setRedo(KeyCombo aRedo)
        {
            redo = aRedo;
        }

        @Override
        public KeyCombo getDeleteAnnotation()
        {
            return deleteAnnotation;
        }

        public void setDeleteAnnotation(KeyCombo aDeleteAnnotation)
        {
            deleteAnnotation = aDeleteAnnotation;
        }

        @Override
        public KeyCombo getClearSelection()
        {
            return clearSelection;
        }

        public void setClearSelection(KeyCombo aClearSelection)
        {
            clearSelection = aClearSelection;
        }

        @Override
        public KeyCombo getToggleSelection()
        {
            return toggleSelection;
        }

        public void setToggleSelection(KeyCombo aToggleSelection)
        {
            toggleSelection = aToggleSelection;
        }
    }

    public static class AnchoringModeShortcutsImpl
        implements AnchoringModeShortcuts
    {
        private KeyCombo characters = new KeyCombo(true, Shift, one);
        private KeyCombo singleToken = new KeyCombo(true, Shift, two);
        private KeyCombo tokens = new KeyCombo(true, Shift, three);
        private KeyCombo sentences = new KeyCombo(true, Shift, four);

        @Override
        public KeyCombo getCharacters()
        {
            return characters;
        }

        public void setCharacters(KeyCombo aCharacters)
        {
            characters = aCharacters;
        }

        @Override
        public KeyCombo getSingleToken()
        {
            return singleToken;
        }

        public void setSingleToken(KeyCombo aSingleToken)
        {
            singleToken = aSingleToken;
        }

        @Override
        public KeyCombo getTokens()
        {
            return tokens;
        }

        public void setTokens(KeyCombo aTokens)
        {
            tokens = aTokens;
        }

        @Override
        public KeyCombo getSentences()
        {
            return sentences;
        }

        public void setSentences(KeyCombo aSentences)
        {
            sentences = aSentences;
        }
    }

    public static class DialogShortcutsImpl
        implements DialogShortcuts
    {
        private KeyCombo closeDialog = new KeyCombo(Escape);

        @Override
        public KeyCombo getCloseDialog()
        {
            return closeDialog;
        }

        public void setCloseDialog(KeyCombo aCloseDialog)
        {
            closeDialog = aCloseDialog;
        }
    }
}
