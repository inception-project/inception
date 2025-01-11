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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits;

import java.io.Serializable;

public class OptionSetting
    implements Serializable
{
    private static final long serialVersionUID = 639108348141364660L;

    private Option<?> option;
    private String value;

    public OptionSetting()
    {
        // For serialization
    }

    public OptionSetting(Option<?> aOption, Object aValue)
    {
        option = aOption;
        if (aValue != null) {
            value = String.valueOf(aValue);
        }
    }

    public Option<?> getOption()
    {
        return option;
    }

    public void setOption(Option<?> aOption)
    {
        option = aOption;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String aValue)
    {
        value = aValue;
    }
}
