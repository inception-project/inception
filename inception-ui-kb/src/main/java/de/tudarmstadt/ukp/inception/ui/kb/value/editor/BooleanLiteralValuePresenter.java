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
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public class BooleanLiteralValuePresenter extends ValuePresenter
{
    private static final long serialVersionUID = -1749133670964165564L;
    
    public BooleanLiteralValuePresenter(String aId, IModel<KBStatement> aModel)
    {
        super(aId, aModel);
        
        add(new CheckBox("value")
                .add(LambdaBehavior.onConfigure(it -> it.setEnabled(false))));
    }

    
    
    
}
