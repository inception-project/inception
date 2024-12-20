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
package de.tudarmstadt.ukp.inception.annotation.feature.number;

import static de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureTraits.EditorType.SPINNER;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Traits for number features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NumberFeatureTraits
    implements Serializable
{
    private static final long serialVersionUID = -2395185084802071593L;

    public enum EditorType
    {
        @JsonEnumDefaultValue
        SPINNER("Spinner"), //
        RADIO_BUTTONS("Radio Buttons");

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

    private boolean limited = false;
    private Number minimum = 0;
    private Number maximum = 0;
    private EditorType editorType = SPINNER;

    public NumberFeatureTraits()
    {
        // Nothing to do
    }

    public boolean isLimited()
    {
        return limited;
    }

    public void setLimited(boolean aLimited)
    {
        limited = aLimited;
    }

    public Number getMinimum()
    {
        return minimum;
    }

    public void setMinimum(Number aMinimum)
    {
        minimum = aMinimum;
    }

    public Number getMaximum()
    {
        return maximum;
    }

    public void setMaximum(Number aMaximum)
    {
        maximum = aMaximum;
    }

    public EditorType getEditorType()
    {
        return editorType;
    }

    public void setEditorType(EditorType aEditorType)
    {
        editorType = aEditorType;
    }
}
