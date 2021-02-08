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
package de.tudarmstadt.ukp.inception.kb;

import org.apache.commons.lang3.Validate;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.URIUtil;

import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public class InceptionValueMapper
{

    public Value mapStatementValue(KBStatement aStatement, ValueFactory vf)
    {
        Validate.notNull(aStatement, "Statement cannot be null");

        Object value = aStatement.getValue();
        String language = aStatement.getLanguage();

        return mapValue(value, language, vf);
    }

    public Value mapQualifierValue(KBQualifier aQualifier, ValueFactory vf)
    {
        Validate.notNull(aQualifier, "Qualifier cannot be null");

        Object value = aQualifier.getValue();
        String language = aQualifier.getLanguage();

        return mapValue(value, language, vf);
    }

    private Value mapValue(Object value, String language, ValueFactory vf)
    {
        if (value instanceof IRI) {
            return (IRI) value;
        }
        else if (value instanceof String && URIUtil.isValidURIReference((String) value)) {
            return vf.createIRI((String) value);
        }
        else if (language != null) {
            return vf.createLiteral((String) value, language);
        }
        else {
            DatatypeMapper mapper = new DefaultDatatypeMapper();
            return mapper.getRDFValue(value, vf);
        }
    }
}
