/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.value;

import static java.util.Arrays.asList;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.DOUBLE;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.FLOAT;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.INT;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.INTEGER;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.LONG;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.NEGATIVE_INTEGER;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.NON_NEGATIVE_INTEGER;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.NON_POSITIVE_INTEGER;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.POSITIVE_INTEGER;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.SHORT;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.UNSIGNED_INT;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.UNSIGNED_LONG;
import static org.eclipse.rdf4j.model.vocabulary.XMLSchema.UNSIGNED_SHORT;

import java.util.List;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.NumericLiteralValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.NumericLiteralValuePresenter;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

@Component
public class NumericLiteralValueSupport
    implements ValueTypeSupport
{
    private static final List<IRI> NUMERIC_TYPES = asList(INTEGER, INT, NON_NEGATIVE_INTEGER,
            NON_POSITIVE_INTEGER, LONG, FLOAT, NEGATIVE_INTEGER, POSITIVE_INTEGER, UNSIGNED_INT,
            UNSIGNED_LONG, UNSIGNED_SHORT, SHORT, DOUBLE);
    
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
        return asList(new ValueType(XMLSchema.DOUBLE.stringValue(), "Numeric", valueTypeSupportId));
    }
    
    @Override
    public boolean accepts(KBStatement aStatement, KBProperty aProperty)
    {
        if (aStatement.getValue() == null) {
            return false;
        }
        IRI iri = DefaultDatatypeMapper.getDatatypeURI((aStatement.getValue()).getClass());
        return NUMERIC_TYPES.contains(iri);
    }
    
    @Override
    public boolean accepts(String range, Optional<KBObject> rangeKbObject)
    {
        return range != null
                && NUMERIC_TYPES.contains(SimpleValueFactory.getInstance().createIRI(range));
    }

    @Override
    public ValueEditor createEditor(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new NumericLiteralValueEditor(aId, aStatement);
    }

    @Override
    public ValuePresenter createPresenter(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new NumericLiteralValuePresenter(aId, aStatement);
    }
}
