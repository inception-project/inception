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

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import wicket.contrib.input.events.key.KeyType;

/**
 * Utility methods for working with keyboard bindings.
 */
public class KeyBindingsUtil
{
    private KeyBindingsUtil()
    {
        // No instances
    }

    public static String formatShortcut(KeyCombo aCombo)
    {
        return formatShortcut(aCombo != null ? aCombo.getKeys() : null);
    }

    /**
     * Formats an array of KeyType values as a human-readable shortcut string.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code [Ctrl, z]} → "Ctrl+z"</li>
     * <li>{@code [Shift, Ctrl, z]} → "Shift+Ctrl+z"</li>
     * <li>{@code [Page_down]} → "Page-down"</li>
     * </ul>
     * 
     * @param aKeys
     *            the keyboard keys to format
     * @return a human-readable shortcut string
     */
    public static String formatShortcut(KeyType[] aKeys)
    {
        if (aKeys == null || aKeys.length == 0) {
            return "";
        }

        return Stream.of(aKeys) //
                .map(KeyType::name) //
                .map(name -> name.replace("_", "-")) //
                .collect(joining("+"));
    }
}
