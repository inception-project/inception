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
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.BooleanLiteralValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.BooleanLiteralValuePresenter;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#booleanLiteralValueSupport}.
 * </p>
 */
public class BooleanLiteralValueSupport
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
        return asList(new ValueType(XSD.BOOLEAN.stringValue(), "Boolean", valueTypeSupportId));
    }

    @Override
    public boolean accepts(KBStatement aStatement, KBProperty aProperty)
    {
        if (aStatement.getValue() == null) {
            return false;
        }

        IRI iri = DefaultDatatypeMapper.getDatatypeURI((aStatement.getValue()).getClass());

        return iri != null && XSD.BOOLEAN.equals(iri);
    }

    @Override
    public boolean accepts(String range, Optional<KBObject> rangeKbObject)
    {
        if (range != null && range.equals(XSD.BOOLEAN.stringValue())) {
            return true;
        }
        return false;
    }

    @Override
    public ValueEditor createEditor(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new BooleanLiteralValueEditor(aId, aStatement);
    }

    @Override
    public ValuePresenter createPresenter(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new BooleanLiteralValuePresenter(aId, aStatement);
    }
}
