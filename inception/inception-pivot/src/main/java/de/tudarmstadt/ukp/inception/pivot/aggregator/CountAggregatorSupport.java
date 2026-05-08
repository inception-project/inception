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

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;

import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.CellRenderer;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public class CountAggregatorSupport
    implements AggregatorSupport
{
    @SuppressWarnings("rawtypes")
    @Override
    public boolean accepts(List<Extractor> aContext)
    {
        return true;
    }

    @Override
    public Aggregator<?, ?> createAggregator()
    {
        return new CountAggregator();
    }

    @Override
    public String getName()
    {
        return "Count";
    }

    @Override
    public boolean supportsCells()
    {
        return false;
    }

    @Override
    public CellRenderer createCellRenderer()
    {
        return (componentId, model) -> new Label(componentId, model);
    }
}
