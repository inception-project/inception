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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.Literal;

public class LiteralValuePresenter
    extends Panel
    implements ValuePresenter<Literal>
{
    private static final long serialVersionUID = -6774637988828817203L;

    private IModel<Literal> model;
    private IModel<String> value;
    private IModel<String> language;

    public LiteralValuePresenter(String id, IModel<Literal> model)
    {
        super(id, model);

        this.model = model;

        value = Model.of();
        language = Model.of();

        add(new Label("value", value));
        add(new Label("language", language)
        {
            private static final long serialVersionUID = 3436068825093393740L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                String language = (String) this.getDefaultModelObject();
                setVisible(StringUtils.isNotEmpty(language));
            }
        });
    }

    @Override
    protected void onBeforeRender()
    {
        Literal literal = this.model.getObject();

        DatatypeMapper mapper = new DefaultDatatypeMapper();
        value.setObject(mapper.getJavaObject(literal).toString());
        language.setObject(literal.getLanguage().orElse(null));

        super.onBeforeRender();
    }
}
