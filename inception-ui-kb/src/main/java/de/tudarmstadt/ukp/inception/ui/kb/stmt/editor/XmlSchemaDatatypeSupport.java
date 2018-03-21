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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.datatypes.XMLSchemaDatatypeHandler;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;

public class XmlSchemaDatatypeSupport implements DatatypeSupport {
    
    private static final XMLSchemaDatatypeHandler HANDLER = new XMLSchemaDatatypeHandler();

    @Override
    public boolean isSupported(IRI datatype) {
        return HANDLER.isRecognizedDatatype(datatype);
    }
    
    @Override
    public boolean isValid(IRI datatype, Value value) {
        return HANDLER.verifyDatatype(value.stringValue(), datatype);
    }

    @Override
    public ValueEditor<?> createEditor(IRI datatype, String id, IModel<Value> model) {
        return new FallbackEditorPresenter(id, model, true);
    }

    @Override
    public WebMarkupContainer createPresenter(IRI datatype, String id, IModel<Value> model) {
        IModel<Literal> literalModel = new LambdaModelAdapter<Literal>(
                () -> (Literal) model.getObject(), (lit) -> model.setObject(lit));
        return new LiteralValuePresenter(id, literalModel);
    }

}
