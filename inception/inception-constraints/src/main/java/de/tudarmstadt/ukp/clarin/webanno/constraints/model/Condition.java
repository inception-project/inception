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

package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.asFlatString;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ASTCondition;

/**
 * Class containing object representation for Condition in a rule.
 */
public class Condition
    implements Serializable
{
    private static final long serialVersionUID = 5229065580264733470L;

    private final String path;
    private final String value;

    public Condition(String aPath, String aValue)
    {
        path = aPath;
        value = aValue;
    }

    public Condition(ASTCondition aCondition)
    {
        path = asFlatString(aCondition.getPath());
        value = aCondition.getValue();
    }

    public String getPath()
    {
        return path;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "Condition [[" + path + "] = [" + value + "]]";
    }

    public boolean matchesAny(List<String> listOfValues)
    {
        return listOfValues.contains(value);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Condition)) {
            return false;
        }
        Condition castOther = (Condition) other;
        return new EqualsBuilder().append(path, castOther.path).append(value, castOther.value)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(path).append(value).toHashCode();
    }
}
