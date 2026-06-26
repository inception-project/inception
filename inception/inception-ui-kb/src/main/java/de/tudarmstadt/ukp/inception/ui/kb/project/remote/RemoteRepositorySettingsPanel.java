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
package de.tudarmstadt.ukp.inception.ui.kb.project.remote;

import static de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType.BASIC;
import static de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType.OAUTH_CLIENT_CREDENTIALS;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.KendoDataSource;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;
import de.tudarmstadt.ukp.inception.security.client.auth.NoAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;
import de.tudarmstadt.ukp.inception.ui.kb.project.validators.Validators;

public class RemoteRepositorySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 8523349361503121641L;

    private static final String CID_AUTHENTICATION_TRAITS_EDITOR = "authenticationTraitsEditor";

    private @SpringBean KnowledgeBaseService kbService;

    private final TextField<String> urlField;
    private final CheckBox skipSslValidation;
    private final TextField<String> defaultDatasetField;
    private final MultiSelect<String> additionalDatasetsField;
    private final DropDownChoice<AuthenticationType> authenticationType;

    private AuthenticationTraitsEditor<?> authenticationTraitsEditor;

    // Named graphs fetched on-demand from the remote endpoint to offer as choices in addition to
    // any IRIs the user enters manually.
    private final List<String> availableDatasets = new ArrayList<>();

    public RemoteRepositorySettingsPanel(String aId,
            CompoundPropertyModel<KnowledgeBaseWrapper> kbModel,
            Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(aId, kbModel);

        urlField = new RequiredTextField<>("url");
        urlField.add(Validators.URL_VALIDATOR);
        queue(urlField);

        authenticationType = new DropDownChoice<>("authenticationType",
                asList(BASIC, OAUTH_CLIENT_CREDENTIALS), new EnumChoiceRenderer<>(this));
        authenticationType.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionAuthenticationTypeSelected));
        authenticationType.setNullValid(true);
        queue(authenticationType);

        authenticationTraitsEditor = updateAuthenticationPanel();

        skipSslValidation = new CheckBox("skipSslValidation", kbModel.bind("kb.skipSslValidation"));
        skipSslValidation.setOutputMarkupPlaceholderTag(true);
        queue(skipSslValidation);

        defaultDatasetField = new TextField<>("defaultDataset",
                kbModel.bind("kb.defaultDatasetIri"));
        defaultDatasetField.add(Validators.IRI_VALIDATOR);
        queue(defaultDatasetField);

        additionalDatasetsField = new MultiSelect<String>("additionalDatasets")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure(KendoDataSource aDataSource)
            {
                // This ensures that we get the user input in getChoices
                aDataSource.set("serverFiltering", true);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("filter", Options.asString("contains"));
                aBehavior.setOption("autoClose", false);
            }

            @Override
            protected List<String> getChoices(String aInput)
            {
                // There is no fixed vocabulary of dataset IRIs. We offer the graphs fetched from
                // the
                // endpoint (if any) plus whatever has been entered so far, and allow the user to
                // add
                // arbitrary IRIs via the current input.
                var choices = new LinkedHashSet<>(getModelObject());
                choices.addAll(availableDatasets);
                var result = new ArrayList<>(choices);
                if (isNotBlank(aInput)) {
                    result.remove(aInput);
                    result.add(0, aInput);
                }
                return result;
            }

            @Override
            public void convertInput()
            {
                var input = getInputAsArray();
                var list = new ArrayList<String>();
                if (input != null) {
                    list.addAll(asList(input));
                    list.removeIf(StringUtils::isBlank);
                }
                setConvertedInput(list);
            }

            @Override
            public void renderHead(IHeaderResponse response)
            {
                super.renderHead(response);
                // The Kendo MultiSelect with serverFiltering=true does not reliably sync chip
                // removals back to the underlying <select> element, so removed items still get
                // submitted with the form. Rebuild the <select> from the widget value on every
                // change to keep form submission in sync.
                var script = "(function(){var attach=function(){var ms=$('#" + getMarkupId()
                        + "').data('kendoMultiSelect');if(!ms){setTimeout(attach,50);return;}"
                        + "ms.bind('change',function(){var v=this.value();var s=$(this.element);"
                        + "s.empty();for(var i=0;i<v.length;i++){"
                        + "s.append($('<option selected></option>').val(v[i]).text(v[i]));}});};"
                        + "attach();})();";
                response.render(OnDomReadyHeaderItem.forScript(script));
            }
        };
        additionalDatasetsField.setOutputMarkupId(true);
        additionalDatasetsField.setModel(kbModel.bind("kb.additionalDatasetIris"));
        additionalDatasetsField.add(this::validateAdditionalDatasets);
        queue(additionalDatasetsField);

        // Enumerating the named graphs requires a connection to the endpoint, which is only
        // available once the knowledge base has been saved (i.e. registered).
        var loadDatasetsButton = new LambdaAjaxLink("loadDatasets", this::actionLoadDatasets);
        loadDatasetsButton
                .add(visibleWhen(() -> getModel().getObject().getKb().getRepositoryId() != null));
        loadDatasetsButton.add(new AttributeModifier("title",
                new StringResourceModel("kb.iri.additionalDatasets.load", this)));
        queue(loadDatasetsButton);
    }

    private void actionLoadDatasets(AjaxRequestTarget aTarget)
    {
        var kb = getModel().getObject().getKb();

        try {
            availableDatasets.clear();
            availableDatasets.addAll(kbService.listDatasets(kb));
            success("Loaded [" + availableDatasets.size() + "] graph(s) from the endpoint.");
        }
        catch (RuntimeException e) {
            availableDatasets.clear();
            error("Unable to load graphs from the endpoint: " + e.getMessage());
        }

        aTarget.add(additionalDatasetsField);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void validateAdditionalDatasets(IValidatable<Collection<String>> aValidatable)
    {
        for (var iri : aValidatable.getValue()) {
            if (!URIUtil.isValidURIReference(iri)) {
                aValidatable.error(
                        new ValidationError().setMessage("[" + iri + "] is not a valid IRI."));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public IModel<KnowledgeBaseWrapper> getModel()
    {
        return (IModel<KnowledgeBaseWrapper>) getDefaultModel();
    }

    private void actionAuthenticationTypeSelected(AjaxRequestTarget aTarget)
    {
        updateAuthenticationPanel();
        aTarget.add(authenticationTraitsEditor);
    }

    private AuthenticationTraitsEditor<?> updateAuthenticationPanel()
    {
        AuthenticationTraitsEditor<?> panel;

        KnowledgeBaseWrapper kbw = getModel().getObject();
        if (kbw.getAuthenticationType() != null) {
            switch (kbw.getAuthenticationType()) {
            case BASIC: {
                if (!(kbw.getTraits().getAuthentication() instanceof BasicAuthenticationTraits)) {
                    kbw.getTraits().setAuthentication(new BasicAuthenticationTraits());
                }

                panel = new BasicAuthenticationTraitsEditor(CID_AUTHENTICATION_TRAITS_EDITOR,
                        Model.of((BasicAuthenticationTraits) kbw.getTraits().getAuthentication()));
                break;
            }
            case OAUTH_CLIENT_CREDENTIALS: {
                if (!(kbw.getTraits()
                        .getAuthentication() instanceof OAuthClientCredentialsAuthenticationTraits)) {
                    kbw.getTraits()
                            .setAuthentication(new OAuthClientCredentialsAuthenticationTraits());
                }
                panel = new OAuthClientCredentialsAuthenticationTraitsEditor(
                        CID_AUTHENTICATION_TRAITS_EDITOR,
                        Model.of((OAuthClientCredentialsAuthenticationTraits) kbw.getTraits()
                                .getAuthentication()));
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
                authenticationTraitsEditor = (AuthenticationTraitsEditor<?>) authenticationTraitsEditor
                        .replaceWith(panel);
            }
            else {
                authenticationTraitsEditor = panel;
                queue(authenticationTraitsEditor);
            }
        }

        return authenticationTraitsEditor;
    }

    public void applyState()
    {
        authenticationTraitsEditor.commit();
    }
}
