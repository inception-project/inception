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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static java.util.Arrays.asList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;
import de.tudarmstadt.ukp.inception.security.client.auth.NoAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.header.HeaderAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.header.HeaderAuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class LookupFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 2147578323003887302L;

    private static final String CID_AUTHENTICATION_TRAITS_EDITOR = "authenticationTraitsEditor";
    private static final String MID_FORM = "form";

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LookupServiceProperties properties;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<LookupFeatureTraits> model;

    private AuthenticationTraitsEditor authenticationTraitsEditor;

    public LookupFeatureTraitsEditor(String aId, FeatureSupport<LookupFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        featureSupportId = aFS.getId();
        feature = aFeature;

        model = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        Form<LookupFeatureTraits> form = new Form<LookupFeatureTraits>(MID_FORM, model)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();

                TextField<String> remoteUrl = new TextField<>("remoteUrl");
                remoteUrl.setRequired(true);
                remoteUrl.add(new UrlValidator());
                queue(remoteUrl);

                var authenticationType = new DropDownChoice<>("authenticationType",
                        asList(AuthenticationType.HEADER), new EnumChoiceRenderer<>(this));
                authenticationType.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        this::actionAuthenticationTypeSelected));
                authenticationType.setNullValid(true);
                queue(authenticationType);

                NumberTextField<Integer> limit = new NumberTextField<>("limit", Integer.class,
                        new Options().set("format", "'#'"));
                limit.setModel(PropertyModel.of(model, "limit"));
                limit.setMinimum(1);
                limit.setMaximum(properties.getHardMaxResults());
                queue(limit);

                CheckBox includeQueryContext = new CheckBox("includeQueryContext");
                includeQueryContext.setOutputMarkupId(true);
                queue(includeQueryContext);

                authenticationTraitsEditor = updateAuthenticationPanel();
            }

            private void actionAuthenticationTypeSelected(AjaxRequestTarget aTarget)
            {
                updateAuthenticationPanel();
                aTarget.add(authenticationTraitsEditor);
            }

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                authenticationTraitsEditor.commit();
                getFeatureSupport().writeTraits(feature.getObject(), model.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    private AuthenticationTraitsEditor updateAuthenticationPanel()
    {
        AuthenticationTraitsEditor panel;

        LookupFeatureTraits traits = model.getObject();
        if (traits.getAuthenticationType() != null) {
            switch (traits.getAuthenticationType()) {
            case HEADER: {
                if (!(traits.getAuthentication() instanceof HeaderAuthenticationTraits)) {
                    traits.setAuthentication(new HeaderAuthenticationTraits());
                }

                panel = new HeaderAuthenticationTraitsEditor(CID_AUTHENTICATION_TRAITS_EDITOR,
                        Model.of((HeaderAuthenticationTraits) traits.getAuthentication()));
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

    @SuppressWarnings("unchecked")
    private FeatureSupport<LookupFeatureTraits> getFeatureSupport()
    {
        return (FeatureSupport<LookupFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }
}
