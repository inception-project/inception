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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBinding;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingTrait;

/**
 * Traits for input field text features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StringFeatureTraits
    implements Serializable, KeyBindingTrait
{
    private static final long serialVersionUID = -8450181605003189055L;

    public enum EditorType
    {
        AUTO("Auto (depending on tagset size)"), //
        RADIOGROUP("Radio group (small tagsets)"), //
        COMBOBOX("Combo box (mid-size tagsets)"), //
        AUTOCOMPLETE("Autocomplete (large tagsets)");

        private final String name;

        EditorType(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private boolean multipleRows = false;
    private boolean dynamicSize = false;
    private int collapsedRows = 1;
    private int expandedRows = 1;
    private EditorType editorType = EditorType.AUTO;
    private List<KeyBinding> keyBindings = new ArrayList<>();

    public StringFeatureTraits()
    {
        // Nothing to do
    }

    public boolean isMultipleRows()
    {
        return multipleRows;
    }

    public void setMultipleRows(boolean multipleRows)
    {
        this.multipleRows = multipleRows;
    }

    public boolean isDynamicSize()
    {
        return dynamicSize;
    }

    public void setDynamicSize(boolean dynamicSize)
    {
        this.dynamicSize = dynamicSize;
    }

    public int getCollapsedRows()
    {
        return collapsedRows;
    }

    public void setCollapsedRows(int collapsedRows)
    {
        this.collapsedRows = collapsedRows;
    }

    public int getExpandedRows()
    {
        return expandedRows;
    }

    public void setExpandedRows(int expandedRows)
    {
        this.expandedRows = expandedRows;
    }

    public EditorType getEditorType()
    {
        return editorType;
    }

    public void setEditorType(EditorType aEditorType)
    {
        editorType = aEditorType;
    }

    @Override
    public List<KeyBinding> getKeyBindings()
    {
        return keyBindings;
    }

    @Override
    public void setKeyBindings(List<KeyBinding> aKeyBindings)
    {
        if (aKeyBindings == null) {
            keyBindings = new ArrayList<>();
        }
        else {
            keyBindings = aKeyBindings;
        }
    }
}
