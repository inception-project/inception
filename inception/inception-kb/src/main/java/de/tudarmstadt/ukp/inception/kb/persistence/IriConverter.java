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

package de.tudarmstadt.ukp.inception.kb.persistence;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IriConverter
    implements AttributeConverter<IRI, String>
{

    @Override
    public String convertToDatabaseColumn(IRI aIri)
    {
        if (aIri == null) {
            return null;
        }

        return aIri.stringValue();
    }

    @Override
    public IRI convertToEntityAttribute(String iriString)
    {
        if (iriString == null || iriString.length() == 0) {
            return null;
        }

        ValueFactory factory = SimpleValueFactory.getInstance();
        return factory.createIRI(iriString);
    }
}
