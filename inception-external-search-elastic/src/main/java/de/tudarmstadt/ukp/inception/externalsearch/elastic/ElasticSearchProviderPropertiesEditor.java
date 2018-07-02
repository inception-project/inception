/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ElasticSearchProviderPropertiesEditor
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean ExternalSearchProviderFactory<ElasticSearchProviderProperties> 
            externalSearchProviderFactory;
    private final DocumentRepository documentRepository;
    private final ElasticSearchProviderProperties properties;

    public ElasticSearchProviderPropertiesEditor(String aId,
            IModel<DocumentRepository> aDocumentRepository)
    {
        super(aId, aDocumentRepository);
        documentRepository = aDocumentRepository.getObject();
        properties = externalSearchProviderFactory.readProperties(documentRepository);

        Form<ElasticSearchProviderProperties> form = new Form<ElasticSearchProviderProperties>(
                MID_FORM, CompoundPropertyModel.of(Model.of(properties)))
        {
            private static final long serialVersionUID = -3109239608742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                externalSearchProviderFactory.writeProperties(documentRepository, properties);
            }
        };

        TextField<String> remoteUrl = new TextField<>("remoteUrl");
        remoteUrl.setRequired(true);
        remoteUrl.add(new UrlValidator());
        form.add(remoteUrl);

        TextField<String> indexName = new TextField<>("indexName");
        indexName.setRequired(true);
        form.add(indexName);

        TextField<String> searchPath = new TextField<>("searchPath");
        searchPath.setRequired(true);
        form.add(searchPath);

        TextField<String> objectType = new TextField<>("objectType");
        objectType.setRequired(true);
        form.add(objectType);

        add(form);
    }
}
