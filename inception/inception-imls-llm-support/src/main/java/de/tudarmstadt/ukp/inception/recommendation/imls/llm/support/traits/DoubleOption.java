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

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Map;

public class DoubleOption
    extends Option<Double>
{
    private static final long serialVersionUID = -8312176008704969446L;

    private final double minValue;
    private final double maxValue;

    public DoubleOption(String aName)
    {
        this(aName, MIN_VALUE, MAX_VALUE);
    }

    public DoubleOption(String aName, double aMinValue, double aMaxValue)
    {
        super(Double.class, aName);

        minValue = aMinValue;
        maxValue = aMaxValue;
    }

    @Override
    public Double get(Map<Option<?>, Object> aOptions)
    {
        var value = aOptions.get(this);

        if (value == null) {
            return null;
        }

        return min(max(minValue, (double) value), maxValue);
    }
}
