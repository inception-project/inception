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
package de.tudarmstadt.ukp.inception.security.client.auth.oauth;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class OAuthClientCredentialsAuthenticationTraitsEditor
    extends AuthenticationTraitsEditor<OAuthClientCredentialsAuthenticationTraits>
{
    private static final long serialVersionUID = 6915129149290353201L;

    private IModel<Boolean> editMode;
    private transient String newClientSecret;

    public OAuthClientCredentialsAuthenticationTraitsEditor(String aId,
            IModel<OAuthClientCredentialsAuthenticationTraits> aModel)
    {
        super(aId, CompoundPropertyModel.of(aModel));

        setOutputMarkupId(true);

        editMode = Model.of(getModelObject().getClientId() == null);

        queue(new Label("status", LoadableDetachableModel.of(this::getStatus))
                .add(visibleWhenNot(editMode)));
        queue(new LambdaAjaxLink("edit", this::actionToggleEditMode) //
                .add(visibleWhenNot(editMode)));
        queue(new TextField<String>("tokenEndpointUrl") //
                .add(new UrlValidator(new String[] { "http", "https" })) //
                .setRequired(true));
        queue(new TextField<String>("clientId") //
                .setRequired(true) //
                .add(visibleWhen(editMode)));
        queue(new PasswordTextField("clientSecret", PropertyModel.of(this, "newClientSecret")) //
                .add(visibleWhen(editMode)));
    }

    private String getStatus()
    {
        var sb = new StringBuilder();
        sb.append("Client ID is ");
        sb.append(getModel().map(OAuthClientCredentialsAuthenticationTraits::getClientId) //
                .map(username -> "[" + username + "]")//
                .orElse("not set").getObject());
        sb.append("; client secret is ");
        sb.append(getModel().map(OAuthClientCredentialsAuthenticationTraits::getClientSecret) //
                .map(username -> "set")//
                .orElse("not set").getObject());
        return sb.toString();
    }

    private void actionToggleEditMode(AjaxRequestTarget aTarget)
    {
        editMode.setObject(!editMode.getObject());
        aTarget.add(this);
    }

    @Override
    public void setEditMode(boolean aMode)
    {
        editMode.setObject(aMode);
        newClientSecret = null;
    }

    @Override
    public void commit()
    {
        if (newClientSecret != null) {
            getModel().getObject().setClientSecret(newClientSecret);
        }

        setEditMode(false);
    }

    @Override
    protected void onModelChanged()
    {
        setEditMode(false);
    }
}
