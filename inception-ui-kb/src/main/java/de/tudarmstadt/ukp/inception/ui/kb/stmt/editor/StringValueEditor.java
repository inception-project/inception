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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

// TODO broken state, don't use
public class StringValueEditor extends ValueEditor<Literal> {

    private static final long serialVersionUID = 6935837930064826698L;
    
    private IModel<Literal> model;
    private IModel<String> value;
    private IModel<String> language;
    
    private Component focusComponent;

    public StringValueEditor(String id, IModel<Literal> model) {
        super(id, model);
        
        this.model = model;
        value = Model.of();
        language = Model.of();
        
        TextArea<String> valueField = new TextArea<>("value", value);
        valueField.setOutputMarkupId(true);
        valueField.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", t -> t.add(getParent())));
        add(valueField);
        focusComponent = valueField;
        
        add(new TextField<>("language", language));
    }

    @Override
    public void convertInput() {
        String text = value.getObject();
        String lang = language.getObject();
        
        ValueFactory vf = SimpleValueFactory.getInstance();
        Literal literal = vf.createLiteral(text, lang);
        
        setConvertedInput(literal);
    }
    
    @Override
    protected void onBeforeRender() {
        // TODO STRAIGHT COPY PASTE FROM PRESENTER
        Object object = this.model.getObject();
        
        // if the model provides what it promises
        if (object instanceof Literal) {
            Literal literal = this.model.getObject();
            
            DatatypeMapper mapper = new DefaultDatatypeMapper();        
            value.setObject(mapper.getJavaObject(literal).toString());
            language.setObject(literal.getLanguage().orElse(null));
        } else {
            value.setObject(null);
            language.setObject(null);
            
            // TODO "wrong statement" notice
        }  
        super.onBeforeRender();
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }

}
