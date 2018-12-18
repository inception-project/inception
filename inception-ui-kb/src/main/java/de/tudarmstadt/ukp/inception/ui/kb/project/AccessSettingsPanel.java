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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;

public class AccessSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 1909643205422526142L;

    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    public AccessSettingsPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);
        setOutputMarkupId(true);

        kbModel = aModel;

        add(createTypeSelection("type", "kb.type"));
        add(createCheckbox("writeprotection", "kb.readOnly"));
    }

    private DropDownChoice<RepositoryType> createTypeSelection(String id, String property)
    {
        DropDownChoice<RepositoryType> typeChoice = new BootstrapSelect<>(id,
                kbModel.bind(property), asList(RepositoryType.values()),
                new EnumChoiceRenderer<>(this));
        typeChoice.setRequired(true);
        return typeChoice;
    }

    private CheckBox createCheckbox(String aId, String aProperty)
    {
        return new CheckBox(aId, kbModel.bind(aProperty));
    }
}
