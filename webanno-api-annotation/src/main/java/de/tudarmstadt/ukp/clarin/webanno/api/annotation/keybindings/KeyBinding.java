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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import wicket.contrib.input.events.key.KeyType;

public class KeyBinding
    implements Serializable
{
    private static final long serialVersionUID = 6394936950272849288L;

    private static final Map<String, KeyType> KEY_MAP;

    private String keyCombo;
    private String value;

    static {
        HashMap<String, KeyType> keyMap = new HashMap<>();
        for (KeyType type : KeyType.values()) {
            keyMap.put(type.getKeyCode().toUpperCase(Locale.US), type);
        }
        KEY_MAP = Collections.unmodifiableMap(keyMap);
    }

    public KeyBinding()
    {
        // Nothing to do here
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

    @JsonIgnore
    public boolean isValid()
    {
        if (isBlank(keyCombo)) {
            return false;
        }

        for (String key : keyCombo.split(" ")) {
            KeyType type = KEY_MAP.get(key.toUpperCase(Locale.US));
            if (type == null) {
                return false;
            }
        }

        return true;
    }

    @JsonIgnore
    public KeyType[] asKeyTypes()
    {
        List<KeyType> combo = new ArrayList<>();
        if (isNotBlank(keyCombo)) {
            for (String key : keyCombo.split(" ")) {
                KeyType type = KEY_MAP.get(key.toUpperCase(Locale.US));
                combo.add(type);
            }
        }

        return combo.toArray(new KeyType[combo.size()]);
    }

    @JsonIgnore
    public String asHtml()
    {
        return "<kbd>" + escapeMarkup(Arrays.stream(asKeyTypes())
                .map(keyType -> keyType.getKeyCode().toUpperCase(Locale.US))
                .collect(Collectors.joining(" "))) + "</kbd>";
    }
}
