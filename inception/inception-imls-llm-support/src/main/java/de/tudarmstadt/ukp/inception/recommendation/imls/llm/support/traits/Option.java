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
import java.util.Map;

public class Option<T>
    implements Serializable
{
    private static final long serialVersionUID = 2775326891245407181L;

    private final Class<T> valueClass;
    private final String name;

    public Option(Class<T> aValueClass, String aName)
    {
        valueClass = aValueClass;
        name = aName;
    }

    public Class<T> getValueClass()
    {
        return valueClass;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "[" + name + "]";
    }

    public T parseValue(String aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (String.class.isAssignableFrom(valueClass)) {
            return valueClass.cast(aValue);
        }

        if (Double.class.isAssignableFrom(valueClass)) {
            return valueClass.cast(Double.parseDouble(aValue));
        }

        if (Integer.class.isAssignableFrom(valueClass)) {
            return valueClass.cast(Integer.parseInt(aValue));
        }

        throw new IllegalStateException("Unable to parse values of [" + valueClass + "]");
    }

    public T get(Map<Option<?>, Object> aOptions)
    {
        return valueClass.cast(aOptions.get(this));
    }
}
