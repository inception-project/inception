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
package de.tudarmstadt.ukp.inception.pivot.aggregator;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.CellRenderer;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.table.CompoundKey;

public class ValueSetAggregatorSupport
    implements AggregatorSupport
{
    @SuppressWarnings("rawtypes")
    @Override
    public boolean accepts(List<Extractor> aContext)
    {
        for (var extractor : aContext) {
            if (!(String.class.isAssignableFrom(extractor.getResultType())
                    || CompoundKey.class.isAssignableFrom(extractor.getResultType()))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Aggregator<LinkedHashSet<Serializable>, Object> createAggregator()
    {
        return new ValueSetAggregator();
    }

    @Override
    public String getName()
    {
        return "Values";
    }

    @Override
    public boolean supportsCells()
    {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CellRenderer createCellRenderer()
    {
        return (componentId, model) -> {
            var object = model.getObject();

            if (object == null) {
                return new Label(componentId);
            }

            if (object instanceof LinkedHashSet) {
                return new ValueSetCellPanel(componentId,
                        (IModel<LinkedHashSet<Serializable>>) model);
            }

            throw new IllegalArgumentException(
                    "Value set aggregator accepts only LinkedHashSet but was [" + object.getClass()
                            + "]");
        };
    }
}
