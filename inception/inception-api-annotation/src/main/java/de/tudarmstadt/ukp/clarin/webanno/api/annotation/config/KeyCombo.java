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

import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.key.KeyType;

public class KeyCombo
{
    private KeyType[] keys;
    private boolean disabledInInputs = false;

    public KeyCombo()
    {
        // Used during loading
    }

    public KeyCombo(KeyType... aKeys)
    {
        keys = aKeys;
    }

    public KeyCombo(boolean aDisabledInInputs, KeyType... aKeys)
    {
        keys = aKeys;
        disabledInInputs = aDisabledInInputs;
    }

    public KeyType[] getKeys()
    {
        return keys;
    }

    public void setKeys(KeyType[] aKeys)
    {
        keys = aKeys;
    }

    public boolean isDisabledInInputs()
    {
        return disabledInInputs;
    }

    public void setDisabledInInputs(boolean aDisabledInInputs)
    {
        disabledInInputs = aDisabledInInputs;
    }

    public InputBehavior toInputBehavior(EventType aEvent)
    {
        return new InputBehavior(keys, aEvent).setDisabledInInput(disabledInInputs);
    }
}
