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

import wicket.contrib.input.events.key.KeyType;

class KeyBindingsUtilTest
{
    @Test
    void testFormatShortcut_SingleKey()
    {
        assertThat(KeyBindingsUtil.formatShortcut(new KeyType[] { Page_down }))
                .isEqualTo("Page-down");
    }

    @Test
    void testFormatShortcut_TwoKeys()
    {
        assertThat(KeyBindingsUtil.formatShortcut(new KeyType[] { Ctrl, z })).isEqualTo("Ctrl+z");
    }

    @Test
    void testFormatShortcut_ThreeKeys()
    {
        assertThat(KeyBindingsUtil.formatShortcut(new KeyType[] { Shift, Ctrl, z }))
                .isEqualTo("Shift+Ctrl+z");
    }

    @Test
    void testFormatShortcut_EmptyArray()
    {
        assertThat(KeyBindingsUtil.formatShortcut(new KeyType[] {})).isEmpty();
    }

    @Test
    void testFormatShortcut_Null()
    {
        assertThat(KeyBindingsUtil.formatShortcut(null)).isEmpty();
    }
}
