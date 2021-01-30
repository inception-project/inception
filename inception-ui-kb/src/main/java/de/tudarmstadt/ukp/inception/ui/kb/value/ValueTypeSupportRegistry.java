/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.ui.kb.value;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public interface ValueTypeSupportRegistry
{
    default List<ValueType> getAllTypes()
    {
        List<ValueType> allTypes = new ArrayList<>();

        for (ValueTypeSupport valueSupport : getValueSupports()) {
            List<ValueType> types = valueSupport.getSupportedValueTypes();
            types.stream().forEach(allTypes::add);
        }

        allTypes.sort(comparing(ValueType::getUiName));

        return allTypes;
    }

    default List<ValueType> getRangeTypes(String range, Optional<KBObject> rangeKbObject)
    {
        List<ValueType> rangeTypes = new ArrayList<>();

        for (ValueTypeSupport valueSupport : getValueSupports()) {
            if (valueSupport.accepts(range, rangeKbObject)) {
                List<ValueType> types = valueSupport.getSupportedValueTypes();
                types.stream().forEach(rangeTypes::add);
            }
        }
        rangeTypes.sort(comparing(ValueType::getUiName));
        return rangeTypes;
    }

    ValueType getValueType(KBStatement aStatement, KBProperty aProperty);

    List<ValueTypeSupport> getValueSupports();

    ValueTypeSupport getValueSupport(KBStatement aStatement, KBProperty aProperty);

    ValueTypeSupport getValueSupport(ValueType type);

}
