/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public class StringLiteralValueEditor
    extends ValueEditor
{
    private static final long serialVersionUID = 6935837930064826698L;

    private TextArea<String> value;

    public StringLiteralValueEditor(String aId, IModel<KBStatement> aModel)
    {
        super(aId, CompoundPropertyModel.of(aModel));

        value = new TextArea<>("value");
        value.setOutputMarkupId(true);
        // Statement values cannot be null/empty - well, in theory they could be the empty string,
        // but we treat the empty string as null
        value.setRequired(true);
        add(value);

        add(new TextField<>("language"));
    }

    @Override
    public Component getFocusComponent()
    {
        return value;
    }
}
