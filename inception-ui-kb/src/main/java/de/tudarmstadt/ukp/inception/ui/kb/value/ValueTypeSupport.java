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
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
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
    /**
     * @return id
     */
    String getId();
    
    
    /**
     * @return list of {@link ValueType} supported
     */
    List<ValueType> getSupportedValueTypes();
    
    
    /**
     * Check if the {@link ValueTypeSupport} accepts {@link KBStatement} with {@link KBProperty}
     * @param aStatement a {@link KBStatement} value
     * @param aProperty a {@link KBProperty} value
     * @return
     */
    boolean accepts(KBStatement aStatement, KBProperty aProperty);

    /**
     * Check if the range or rangeKbObject is accepted by the {@link ValueTypeSupport} 
     * @param range a range value
     * @param rangeKbObject a range value from KB
     * @return
     */
    boolean accepts(String range, Optional<KBObject> rangeKbObject);
    
    
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

    /**
     * Returns a {@link ValuePresenter} instance given a datatype IRI (most likely the range of a
     * property or the datatype of a statement).
     * 
     * @param aId
     *            Wicket markup id received by the presenter instances
     * 
     * @return a {@link ValuePresenter} instance
     */
    ValuePresenter createPresenter(String aId, IModel<KBStatement> aModel,
            IModel<KBProperty> aDatatype);
}
