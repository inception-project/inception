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
package de.tudarmstadt.ukp.inception.externalsearch.elastic.traits;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ElasticSearchProviderTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean ExternalSearchProviderFactory<ElasticSearchProviderTraits> 
            externalSearchProviderFactory;
    private final DocumentRepository documentRepository;
    private final ElasticSearchProviderTraits properties;

    public ElasticSearchProviderTraitsEditor(String aId,
            IModel<DocumentRepository> aDocumentRepository)
    {
        super(aId, aDocumentRepository);
        documentRepository = aDocumentRepository.getObject();
        properties = externalSearchProviderFactory.readTraits(documentRepository);

        Form<ElasticSearchProviderTraits> form = new Form<ElasticSearchProviderTraits>(
                MID_FORM, CompoundPropertyModel.of(Model.of(properties)))
        {
            private static final long serialVersionUID = -3109239608742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                externalSearchProviderFactory.writeTraits(documentRepository, properties);
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
    
        NumberTextField<Integer> resultSize =
                new NumberTextField<>("resultSize", Integer.class);
        resultSize.setMinimum(1);
        resultSize.setMaximum(10000);
        resultSize.setRequired(true);
        form.add(resultSize);

        NumberTextField<Integer> seed = new NumberTextField<Integer>("seed", Integer.class);
        seed.setMinimum(0);
        seed.setMaximum(Integer.MAX_VALUE);
        seed.add(visibleWhen(() -> properties.isRandomOrder()));
        seed.add(new AttributeModifier("title", getString("seedTooltip")));
        seed.setOutputMarkupPlaceholderTag(true);
        seed.setRequired(true);
        form.add(seed);

        CheckBox randomOrder = new CheckBox("randomOrder");
        randomOrder.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> 
                t.add(seed, randomOrder)));
        form.add(randomOrder);

        add(form);
    }
}
