/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxPreventSubmitBehavior;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;

public class DynamicTextAreaFeatureEditor
        extends TextFeatureEditorBase
{
    private static final long serialVersionUID = -4798925191252992223L;
    
    public DynamicTextAreaFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);
    }
    
    @Override
    protected AbstractTextComponent createInputField()
    {
        TextArea<String> textarea = new TextArea<>("value");
        textarea.add(new AjaxPreventSubmitBehavior());
        return textarea;
    }
}
