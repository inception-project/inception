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

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public class XmlSchemaDatatypeSupport implements DatatypeSupport {
    
    private static final Set<IRI> SUPPORTED_DATATYPES;
    
    static {
        SUPPORTED_DATATYPES = new HashSet<>();
//        SUPPORTED_DATATYPES.add(XMLSchema.STRING);
    }

    public XmlSchemaDatatypeSupport() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isSupported(IRI datatype) {
        return SUPPORTED_DATATYPES.contains(datatype);
    }

    @Override
    public ValueEditor<?> createEditor(IRI datatype, String id, IModel<Value> model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WebMarkupContainer createPresenter(IRI datatype, String id, IModel<Value> model) {
        // TODO Auto-generated method stub
        return null;
    }

}
