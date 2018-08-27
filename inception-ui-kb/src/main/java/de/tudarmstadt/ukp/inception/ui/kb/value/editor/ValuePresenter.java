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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public abstract class ValuePresenter
    extends Panel
{
    private static final long serialVersionUID = -7446040894278213264L;

    public ValuePresenter(String aId, IModel<KBStatement> aModel)
    {
        super(aId, aModel);
    }
    
    public KBStatement getModelObject()
    {
        return (KBStatement) getDefaultModelObject();
    }
}
