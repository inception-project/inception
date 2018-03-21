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
package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import java.io.Serializable;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

/**
 * A {@link DatatypeSupport} reports if it supports a given datatype (string, int, etc., identified
 * by IRIs). It provides Wicket components for presenting and editing values of supported datatypes.
 */
public interface DatatypeSupport extends Serializable {

    // TODO could rely on DatatypeHandlerRegistry from org.eclipse.rdf4j.rio

    public boolean isSupported(IRI datatype);
    
    public boolean isValid(IRI datatype, Value value);

    /**
     * Returns a {@link ValueEditor} instance given a datatype IRI (most likely the range of a
     * property or the datatype of a statement).
     * 
     * @param datatype
     *            the IRI of the datatype
     * @param id
     *            Wicket markup id received by the editor instances
     * 
     * @return a {@link ValueEditor} instance
     */
    public ValueEditor<?> createEditor(IRI datatype, String id, IModel<Value> model);
    
    public WebMarkupContainer createPresenter(IRI datatype, String id, IModel<Value> model);
}
