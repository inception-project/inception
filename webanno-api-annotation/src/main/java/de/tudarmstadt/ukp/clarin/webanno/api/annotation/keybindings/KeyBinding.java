/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import wicket.contrib.input.events.key.KeyType;

public class KeyBinding
    implements Serializable
{
    private static final long serialVersionUID = 6394936950272849288L;

    private String keyCombo;
    
    private String value;

    public KeyBinding()
    {
        // Nothing to do
    }
    
    public KeyBinding(String aKeyCombo, String aValue)
    {
        keyCombo = aKeyCombo;
        value = aValue;
    }

    public String getKeyCombo()
    {
        return keyCombo;
    }

    public void setKeyCombo(String aKeyCombo)
    {
        keyCombo = aKeyCombo;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String aValue)
    {
        value = aValue;
    }

    public KeyType[] asKeyTypes() throws IllegalKeyComboException
    {
        Map<String, KeyType> keys = new HashMap<>();
        for (KeyType type : KeyType.values()) {
            keys.put(type.name().toUpperCase(Locale.US), type);
        }
        
        List<KeyType> combo = new ArrayList<>();
        if (isNotBlank(keyCombo)) {
            for (String key : keyCombo.split(" ")) {
                KeyType type = keys.get(key.toUpperCase(Locale.US));
                if (type == null) {
                    throw new IllegalKeyComboException(keyCombo);
                }
                combo.add(type);
            }
        }
        
        return combo.toArray(new KeyType[combo.size()]);
    }
}
