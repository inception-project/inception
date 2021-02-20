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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

public class ValuesPattern
    implements GraphPattern
{
    private final Variable variable;
    private final List<RdfValue> values;

    public ValuesPattern(Variable aVariable, Collection<RdfValue> aValues)
    {
        variable = aVariable;
        values = new ArrayList<>(aValues);
    }

    public ValuesPattern(Variable aVariable, RdfValue... aValues)
    {
        variable = aVariable;
        values = asList(aValues);
    }

    @Override
    public String getQueryString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("VALUES ( ");
        sb.append(variable.getQueryString());
        sb.append(" ) ");
        sb.append("{ ");
        for (RdfValue value : values) {
            sb.append("(");
            sb.append(value.getQueryString());
            sb.append(") ");
        }
        sb.append("} ");
        return sb.toString();
    }

    @Override
    public boolean isEmpty()
    {
        return values.isEmpty();
    }
}
