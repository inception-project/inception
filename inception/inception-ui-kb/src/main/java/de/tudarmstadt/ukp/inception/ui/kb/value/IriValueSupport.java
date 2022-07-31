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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.IRIValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.IRIValuePresenter;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#iriValueSupport}.
 * </p>
 */
public class IriValueSupport
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
        return asList(new ValueType(XSD.ANYURI.stringValue(), "Resource", valueTypeSupportId));
    }

    @Override
    public boolean accepts(KBStatement aStatement, KBProperty aProperty)
    {
        return aStatement.getValue() != null && aStatement.getValue() instanceof IRI;

    }

    @Override
    public boolean accepts(String range, Optional<KBObject> rangeKbObject)
    {
        if (rangeKbObject != null && rangeKbObject.isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    public ValueEditor createEditor(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new IRIValueEditor(aId, aStatement, aProperty, aKbModel);
    }

    @Override
    public ValuePresenter createPresenter(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> aKbModel)
    {
        return new IRIValuePresenter(aId, aStatement, aKbModel);
    }
}
