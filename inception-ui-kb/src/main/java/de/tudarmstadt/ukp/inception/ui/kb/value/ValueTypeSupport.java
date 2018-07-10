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

import java.util.List;

import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValuePresenter;

/**
 * A {@link ValueTypeSupport} reports if it supports a given datatype (string, int, etc., identified
 * by IRIs). It provides Wicket components for presenting and editing values of supported datatypes.
 */
public interface ValueTypeSupport
    extends BeanNameAware
{
    String getId();
    
    List<ValueType> getSupportedValueTypes();
    
    boolean accepts(KBStatement aStatement, KBProperty aProperty);

    /**
     * Returns a {@link ValueEditor} instance given a datatype IRI (most likely the range of a
     * property or the datatype of a statement).
     * 
     * @param aId
     *            Wicket markup id received by the editor instances
     * 
     * @return a {@link ValueEditor} instance
     */
    ValueEditor createEditor(String aId, IModel<KBStatement> aModel, IModel<KBProperty> aDatatype,
            IModel<KnowledgeBase> kbModel);

    ValuePresenter createPresenter(String aId, IModel<KBStatement> aModel,
            IModel<KBProperty> aDatatype);
}
