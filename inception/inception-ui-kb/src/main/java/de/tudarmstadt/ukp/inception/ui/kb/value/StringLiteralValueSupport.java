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
package de.tudarmstadt.ukp.inception.ui.kb.value;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.StringLiteralValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.StringLiteralValuePresenter;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#stringLiteralValueSupport}.
 * </p>
 */
public class StringLiteralValueSupport
    implements ValueTypeSupport
{
    private String valueTypeSupportId;

    @Override
    public String getId()
    {
        return valueTypeSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        valueTypeSupportId = aBeanName;
    }

    @Override
    public List<ValueType> getSupportedValueTypes()
    {
        return asList(new ValueType(XSD.STRING.stringValue(), "String", valueTypeSupportId));
    }

    @Override
    public boolean accepts(KBStatement aStatement, KBProperty aProperty)
    {
        // accept statements with null as value so that the StringEditor appears as default case
        if (aStatement.getValue() == null) {
            return false;
        }
        IRI iri = DefaultDatatypeMapper.getDatatypeURI((aStatement.getValue()).getClass());
        // Conditions for different datatype URI apart from String
        boolean accept = XSD.STRING.equals(iri);

        return iri != null && accept;
    }

    @Override
    public boolean accepts(String range, Optional<KBObject> rangeKbObject)
    {
        if (rangeKbObject != null && rangeKbObject.isPresent()) {
            return true;
        }
        else if (range != null && range.equals(XSD.STRING.stringValue())) {
            return true;
        }

        return false;
    }

    @Override
    public ValueEditor createEditor(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new StringLiteralValueEditor(aId, aStatement);
    }

    @Override
    public ValuePresenter createPresenter(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new StringLiteralValuePresenter(aId, aStatement);
    }
}
