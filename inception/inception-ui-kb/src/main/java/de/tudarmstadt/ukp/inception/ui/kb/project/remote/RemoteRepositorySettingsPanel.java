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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;
import de.tudarmstadt.ukp.inception.security.client.auth.NoAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseInfoPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseWrapper;
import de.tudarmstadt.ukp.inception.ui.kb.project.validators.Validators;

public class RemoteRepositorySettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 8523349361503121641L;

    private static final String CID_AUTHENTICATION_TRAITS_EDITOR = "authenticationTraitsEditor";

    private static final int MAXIMUM_REMOTE_REPO_SUGGESTIONS = 10;

    private final Map<String, KnowledgeBaseProfile> knowledgeBaseProfiles;

    private CompoundPropertyModel<KnowledgeBaseInfo> kbInfoModel;

    private final KnowledgeBaseInfoPanel infoContainerRemote;
    private final TextField<String> urlField;
    private final CheckBox skipSslValidation;
    private final TextField<String> defaultDatasetField;
    private final DropDownChoice<AuthenticationType> authenticationType;

    private AuthenticationTraitsEditor authenticationTraitsEditor;

    public RemoteRepositorySettingsPanel(String aId,
            CompoundPropertyModel<KnowledgeBaseWrapper> kbModel,
            Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(aId, kbModel);

        knowledgeBaseProfiles = aKnowledgeBaseProfiles;

        kbInfoModel = CompoundPropertyModel.of(Model.of());

        urlField = new RequiredTextField<>("url");
        urlField.add(Validators.URL_VALIDATOR);
        queue(urlField);

        authenticationType = new DropDownChoice<>("authenticationType",
                asList(AuthenticationType.values()), new EnumChoiceRenderer<>(this));
        authenticationType.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionAuthenticationTypeSelected));
        authenticationType.setNullValid(true);
        queue(authenticationType);

        authenticationTraitsEditor = updateAuthenticationPanel();

        skipSslValidation = new CheckBox("skipSslValidation", kbModel.bind("kb.skipSslValidation"));
        skipSslValidation.setOutputMarkupPlaceholderTag(true);
        queue(skipSslValidation);

        infoContainerRemote = new KnowledgeBaseInfoPanel("infoContainer", kbInfoModel);
        infoContainerRemote.setOutputMarkupId(true);
        queue(infoContainerRemote);

        queue(remoteSuggestionsList("suggestions"));

        defaultDatasetField = new TextField<>("defaultDataset",
                kbModel.bind("kb.defaultDatasetIri"));
        defaultDatasetField.add(Validators.IRI_VALIDATOR);
        queue(defaultDatasetField);
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

    private AuthenticationTraitsEditor updateAuthenticationPanel()
    {
        AuthenticationTraitsEditor panel;

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

    private ListView<KnowledgeBaseProfile> remoteSuggestionsList(String aId)
    {
        // for up to MAXIMUM_REMOTE_REPO_SUGGESTIONS of knowledge bases, create a link which
        // directly fills in the URL field (convenient for both developers AND users :))
        List<KnowledgeBaseProfile> suggestions = knowledgeBaseProfiles.values().stream() //
                .filter(kb -> REMOTE == kb.getType()) //
                .limit(MAXIMUM_REMOTE_REPO_SUGGESTIONS) //
                .collect(toList());

        var list = new ListView<KnowledgeBaseProfile>(aId, suggestions)
        {
            private static final long serialVersionUID = 4179629475064638272L;

            @Override
            protected void populateItem(ListItem<KnowledgeBaseProfile> item)
            {
                // add a link for one knowledge base with proper label
                var link = new LambdaAjaxLink("suggestionLink",
                        t -> actionPopulate(t, item.getModelObject()));
                link.add(new Label("suggestionLabel", item.getModelObject().getName()));
                item.add(link);
            }
        };

        list.add(visibleWhen(getModel().map(KnowledgeBaseWrapper::getKb) //
                .map(kb -> kb.getRepositoryId() == null) //
                .orElse(false)));

        return list;
    }

    private void actionPopulate(AjaxRequestTarget aTarget, KnowledgeBaseProfile aProfile)
    {
        // set all the fields according to the chosen profile
        KnowledgeBaseWrapper kbw = getModel().getObject();
        kbw.setUrl(aProfile.getAccess().getAccessUrl());
        // sets root concepts list - if null then an empty list otherwise change the
        // values to IRI and populate the list
        KnowledgeBase kb = kbw.getKb();
        kb.applyRootConcepts(aProfile);
        kb.applyAdditionalMatchingProperties(aProfile);
        kb.applyMapping(aProfile.getMapping());
        kbInfoModel.setObject(aProfile.getInfo());
        kb.setFullTextSearchIri(aProfile.getAccess().getFullTextSearchIri());
        kb.setDefaultLanguage(aProfile.getDefaultLanguage());
        kb.setDefaultDatasetIri(aProfile.getDefaultDataset());
        kb.setReification(aProfile.getReification());
        aTarget.add(urlField, defaultDatasetField, infoContainerRemote);
    }

    public void applyState()
    {
        authenticationTraitsEditor.commit();
    }
}
