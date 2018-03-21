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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;

public class ClassDatatypeSupport implements DatatypeSupport {
    
    private static final long serialVersionUID = -5260014734312217472L;
    
    private IRI classIRI;    
    
    public ClassDatatypeSupport(IRI classIRI) {
        this.classIRI = classIRI;
    }    

    @Override
    public boolean isSupported(IRI datatype) {
        return classIRI.equals(datatype);
    }

    @Override
    public boolean isValid(IRI datatype, Value value) {
        // FIXME sloppy!
        return isSupported(datatype);
    }

    @Override
    public ValueEditor<?> createEditor(IRI datatype, String id, IModel<Value> model) {
        return new FallbackEditorPresenter(id, model, true);
    }

    @Override
    public WebMarkupContainer createPresenter(IRI datatype, String id, IModel<Value> model) {
        IModel<IRI> iriModel = new LambdaModelAdapter<IRI>(
                () -> (IRI) model.getObject(), (iri) -> model.setObject(iri));
        return new IRIValuePresenter<IRI>(id, iriModel);
    }

}
