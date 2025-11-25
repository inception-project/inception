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
package de.tudarmstadt.ukp.inception.pivot.table;

import java.io.Serializable;
import java.util.List;

import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public class CompoundKeyBuilder<T>
    implements Serializable
{
    private static final long serialVersionUID = -1463683100039093536L;

    private final List<? extends Extractor<T, ? extends Serializable>> extractors;
    private final CompoundKeySchema schema;

    public CompoundKeyBuilder(List<? extends Extractor<T, ? extends Serializable>> aExtractors)
    {
        extractors = aExtractors;

        if (extractors.isEmpty()) {
            schema = new CompoundKeySchema(false, "<aggregation>");
        }
        else {
            schema = new CompoundKeySchema(aExtractors.toArray(Extractor[]::new));
        }
    }

    public CompoundKeySchema getSchema()
    {
        return schema;
    }

    public CompoundKey buildKey(T aSource, Aggregator<?, ?> aAggregator)
    {
        if (extractors.isEmpty()) {
            return new CompoundKey(schema, false, new Serializable[] { aAggregator.getName() });
        }

        var values = new Serializable[schema.size()];

        for (var i = 0; i < extractors.size(); i++) {
            var extractor = extractors.get(i);
            var index = schema.getIndex(extractor.getName());
            if (extractor.accepts(aSource)) {
                values[index] = extractor.extract(aSource);
            }
        }

        return new CompoundKey(schema, false, values);
    }
}
