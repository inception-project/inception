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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgSession;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.service.ElgService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class ElgSessionPanel
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final SimpleDateFormat DURATION_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    private static final String MID_SIGN_IN_FORM = "signInForm";

    private @SpringBean ElgAuthenticationClient elgAuthenticationClient;
    private @SpringBean ElgService elgService;

    private IModel<Project> project;
    private IModel<Boolean> requiresSignIn;

    private String tokenCode;

    public ElgSessionPanel(String aId, IModel<Project> aProject)
    {
        super(aId);

        project = aProject;

        setOutputMarkupId(true);

        setDefaultModel(Model.of(elgService.getSession(aProject.getObject()).orElse(null)));

        refreshSessionIfPossible(getModelObject());

        requiresSignIn = LoadableDetachableModel.of(this::isSignInRequired);

        Form<Void> signInForm = new Form<Void>(MID_SIGN_IN_FORM)
        {
            private static final long serialVersionUID = 1494895206703898448L;

            @Override
            protected boolean wantSubmitOnParentFormSubmit()
            {
                return false;
            }
        };
        signInForm.setOutputMarkupPlaceholderTag(true);
        signInForm.add(visibleWhen(requiresSignIn));

        signInForm.add(new ExternalLink("signInLink", elgAuthenticationClient.getCodeUrl()));

        TextArea<String> code = new TextArea<>("code",
                new PropertyModel<String>(this, "tokenCode"));
        code.setRequired(true);
        signInForm.add(code);

        signInForm.add(new LambdaAjaxButton<>("signIn", this::actionSignIn));
        add(signInForm);

        WebMarkupContainer signOutPanel = new WebMarkupContainer("signOutPanel");
        signOutPanel.add(visibleWhenNot(requiresSignIn));

        signOutPanel.add(new LambdaAjaxLink("signOut", this::actionSignOut));

        Label identity = new Label("identity", getUserIdentity());
        identity.add(visibleWhenNot(requiresSignIn));
        signOutPanel.add(identity);

        Label accessTokenValidUntil = new Label("accessTokenValidUntil");
        accessTokenValidUntil.setDefaultModel(getModel() //
                .map(ElgSession::getAccessTokenValidUntil) //
                .map(DURATION_FORMAT::format));
        accessTokenValidUntil.add(visibleWhen(getModel() //
                .map(ElgSession::getAccessTokenValidUntil) //
                .isPresent()));
        accessTokenValidUntil
                .setVisibilityAllowed(getApplication().getConfigurationType() == DEVELOPMENT);
        signOutPanel.add(accessTokenValidUntil);

        LambdaAjaxLink invalidateAccessToken = new LambdaAjaxLink("invalidateAccessToken",
                this::actionInvalidateAccessToken);
        invalidateAccessToken.add(visibleWhen(getModel() //
                .map(ElgSession::getAccessTokenValidUntil) //
                .isPresent()));
        invalidateAccessToken
                .setVisibilityAllowed(getApplication().getConfigurationType() == DEVELOPMENT);
        signOutPanel.add(invalidateAccessToken);

        LambdaAjaxLink refreshAccessToken = new LambdaAjaxLink("refreshAccessToken",
                this::actionRefreshAccessToken);
        refreshAccessToken.add(visibleWhen(getModel() //
                .map(ElgSession::getAccessTokenValidUntil) //
                .isPresent()));
        refreshAccessToken
                .setVisibilityAllowed(getApplication().getConfigurationType() == DEVELOPMENT);
        signOutPanel.add(refreshAccessToken);

        Label signInValidUntil = new Label("signInValidUntil");
        signInValidUntil.setDefaultModel(getModel() //
                .map(ElgSession::getRefreshTokenValidUntil) //
                .map(DURATION_FORMAT::format));
        signInValidUntil.add(visibleWhen(getModel() //
                .map(ElgSession::getRefreshTokenValidUntil) //
                .isPresent()));
        signOutPanel.add(signInValidUntil);

        Label refreshTokenExpiresIn = new Label("expiresIn");
        refreshTokenExpiresIn.setDefaultModel(getModel() //
                .map(ElgSession::getRefreshTokenValidUntil) //
                .map(t -> MILLIS.between(now(), t.toInstant())) //
                .map(d -> formatDurationWords(d, true, true)));
        signOutPanel.add(refreshTokenExpiresIn);
        add(signOutPanel);
    }

    @SuppressWarnings("unchecked")
    public IModel<ElgSession> getModel()
    {
        return (IModel<ElgSession>) getDefaultModel();
    }

    public void setModelObject(ElgSession aSession)
    {
        setDefaultModelObject(aSession);
    }

    public ElgSession getModelObject()
    {
        return (ElgSession) getDefaultModelObject();
    }

    private boolean isSignInRequired()
    {
        ElgSession session = getModelObject();
        if (session == null) {
            return true;
        }

        return elgAuthenticationClient.requiresSignIn(session);
    }

    private void refreshSessionIfPossible(ElgSession aSession)
    {
        if (aSession == null) {
            return;
        }

        try {
            elgService.refreshSession(aSession);
        }
        catch (IOException e) {
            error("Signing out from ELG as session count not be refreshed: "
                    + getRootCauseMessage(e));
        }
    }

    private void actionInvalidateAccessToken(AjaxRequestTarget aTarget)
    {
        getModelObject().setAccessToken("dummy");
        getModelObject().setAccessTokenValidUntil(Date.from(Instant.now().minusSeconds(120)));
        elgService.createOrUpdateSession(getModelObject());
        success("Access token invalidated");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(this);
    }

    private void actionRefreshAccessToken(AjaxRequestTarget aTarget)
    {
        refreshSessionIfPossible(getModelObject());
        success("Access token refreshed");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(this);
    }

    private void actionSignOut(AjaxRequestTarget aTarget)
    {
        try {
            elgService.signOut(project.getObject());
            setModelObject(null);
            tokenCode = null;
            requiresSignIn.detach();
            aTarget.add(this);
        }
        catch (Exception e) {
            error("Unable to sign out: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionSignIn(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        try {
            setModelObject(elgService.signIn(project.getObject(), tokenCode));
            requiresSignIn.detach();
            aTarget.add(this);
        }
        catch (Exception e) {
            error("Unable to sign in: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private IModel<String> getUserIdentity()
    {
        return getModel().map(ElgSession::getAccessToken).map(accessToken -> {
            try {
                return elgAuthenticationClient.getUserInfo(accessToken).getPreferredUsername();
            }
            catch (IOException e) {
                return "Unable to retrieve identity: " + ExceptionUtils.getRootCauseMessage(e);
            }
        });
    }
}
