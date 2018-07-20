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

import java.util.List;

import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.IRIValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.IRIValuePresenter;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

@Component
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
        return asList(new ValueType("resource", "Resource", valueTypeSupportId));
    }

    @Override
    public boolean accepts(KBStatement aStatement, KBProperty aProperty)
    {   
        if (aStatement.getValue() != null) {
            return aStatement.getValue() instanceof IRI;
        }
        else {
            return aStatement.getInstance() instanceof IRI
                    || aStatement.getInstance() instanceof KBHandle;
        }
    }

//    @Override
//    public boolean accepts(String range)
//    {   
//       
//    }
    
    @Override
    public ValueEditor createEditor(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty, IModel<KnowledgeBase> kbModel)
    {
        return new IRIValueEditor(aId, aStatement, kbModel);
    }

    @Override
    public ValuePresenter createPresenter(String aId, IModel<KBStatement> aStatement,
            IModel<KBProperty> aProperty)
    {
        return new IRIValuePresenter(aId, aStatement);
    }
}
