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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;
import de.tudarmstadt.ukp.inception.security.client.auth.NoAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class OpenSearchProviderTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String CID_AUTHENTICATION_TRAITS_EDITOR = "authenticationTraitsEditor";
    private static final String MID_FORM = "form";

    private @SpringBean ExternalSearchProviderFactory<OpenSearchProviderTraits> externalSearchProviderFactory;
    private final DocumentRepository documentRepository;
    private final OpenSearchProviderTraits properties;

    private AuthenticationTraitsEditor authenticationTraitsEditor;

    private final IModel<OpenSearchProviderTraits> model;

    public OpenSearchProviderTraitsEditor(String aId,
            IModel<DocumentRepository> aDocumentRepository)
    {
        super(aId, aDocumentRepository);
        documentRepository = aDocumentRepository.getObject();
        properties = externalSearchProviderFactory.readTraits(documentRepository);

        model = CompoundPropertyModel.of(Model.of(properties));
        Form<OpenSearchProviderTraits> form = new Form<OpenSearchProviderTraits>(MID_FORM, model)
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

        CheckBox sslVerification = new CheckBox("sslVerification");
        sslVerification.setOutputMarkupId(true);
        form.add(sslVerification);

        TextField<String> indexName = new TextField<>("indexName");
        indexName.setRequired(true);
        form.add(indexName);

        var authenticationType = new DropDownChoice<>("authenticationType",
                asList(AuthenticationType.BASIC), new EnumChoiceRenderer<>(this));
        authenticationType.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionAuthenticationTypeSelected));
        authenticationType.setNullValid(true);
        queue(authenticationType);

        authenticationTraitsEditor = updateAuthenticationPanel();

        TextField<String> searchPath = new TextField<>("searchPath");
        searchPath.setRequired(true);
        form.add(searchPath);

        TextField<String> defaultField = new TextField<>("defaultField");
        defaultField.setRequired(true);
        form.add(defaultField);

        NumberTextField<Integer> resultSize = new NumberTextField<>("resultSize", Integer.class);
        resultSize.setMinimum(1);
        resultSize.setMaximum(10000);
        resultSize.setRequired(true);
        form.add(resultSize);

        NumberTextField<Integer> seed = new NumberTextField<Integer>("seed", Integer.class);
        seed.setMinimum(0);
        seed.setMaximum(Integer.MAX_VALUE);
        seed.add(visibleWhen(() -> properties.isRandomOrder()));
        seed.add(new AttributeModifier("title", new ResourceModel("seedTooltip")));
        seed.setOutputMarkupPlaceholderTag(true);
        seed.setRequired(true);
        form.add(seed);

        CheckBox randomOrder = new CheckBox("randomOrder");
        randomOrder.setOutputMarkupId(true);
        randomOrder.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                t -> t.add(seed, randomOrder)));
        form.add(randomOrder);

        add(form);
    }

    private void actionAuthenticationTypeSelected(AjaxRequestTarget aTarget)
    {
        updateAuthenticationPanel();
        aTarget.add(authenticationTraitsEditor);
    }

    private AuthenticationTraitsEditor updateAuthenticationPanel()
    {
        AuthenticationTraitsEditor panel;

        OpenSearchProviderTraits traits = model.getObject();
        if (traits.getAuthenticationType() != null) {
            switch (traits.getAuthenticationType()) {
            case BASIC: {
                if (!(traits.getAuthentication() instanceof BasicAuthenticationTraits)) {
                    traits.setAuthentication(new BasicAuthenticationTraits());
                }

                panel = new BasicAuthenticationTraitsEditor(CID_AUTHENTICATION_TRAITS_EDITOR,
                        Model.of((BasicAuthenticationTraits) traits.getAuthentication()));
                break;
            }
            default:
                panel = new NoAuthenticationTraitsEditor(CID_AUTHENTICATION_TRAITS_EDITOR);
                break;
            }
        }
        else {
            panel = new NoAuthenticationTraitsEditor(CID_AUTHENTICATION_TRAITS_EDITOR);
        }

        panel.setOutputMarkupId(true);

        if (authenticationTraitsEditor == null
                || panel.getClass() != authenticationTraitsEditor.getClass()) {
            if (authenticationTraitsEditor != null) {
                authenticationTraitsEditor = (AuthenticationTraitsEditor) authenticationTraitsEditor
                        .replaceWith(panel);
            }
            else {
                authenticationTraitsEditor = panel;
                queue(authenticationTraitsEditor);
            }
        }

        return authenticationTraitsEditor;
    }
}
