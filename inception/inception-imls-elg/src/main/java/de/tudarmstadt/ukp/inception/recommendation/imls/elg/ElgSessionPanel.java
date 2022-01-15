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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.recommendation.imls.elg.ElgSessionState.KEY_ELG_SESSION_STATE;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import java.io.IOException;
import java.text.SimpleDateFormat;

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
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;

public class ElgSessionPanel
    extends Panel
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final SimpleDateFormat DURATION_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    private static final String MID_SIGN_IN_FORM = "signInForm";

    private @SpringBean ElgAuthenticationClient elgAuthenticationClient;
    private @SpringBean PreferencesService preferencesService;

    private IModel<Project> project;

    private String tokenCode;

    public ElgSessionPanel(String aId, IModel<Project> aProject)
    {
        super(aId);
        
        project = aProject;
        
        setOutputMarkupId(true);
        
        setDefaultModel(Model.of(preferencesService
                .loadDefaultTraitsForProject(KEY_ELG_SESSION_STATE, aProject.getObject())));

        refreshSessionIfPossible(getModelObject());

        IModel<Boolean> requiresSignIn = LoadableDetachableModel
                .of(() -> elgAuthenticationClient.requiresSignIn(getModelObject()));
        
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
        
        Label signInValidUntil = new Label("signInValidUntil");
        signInValidUntil.setDefaultModel(getModel() //
                .map(ElgSessionState::getRefreshTokenValidUntil) //
                .map(DURATION_FORMAT::format));
        signInValidUntil.add(visibleWhen(getModel() //
                .map(ElgSessionState::getRefreshTokenValidUntil) //
                .map(v -> v > 0)));
        signOutPanel.add(signInValidUntil);

        Label expiresIn = new Label("expiresIn");
        expiresIn.setDefaultModel(getModel() //
                .map(s -> currentTimeMillis() - s.getRefreshTokenValidUntil())
                .map(d -> formatDurationWords(d, true, true)));
        signOutPanel.add(expiresIn);
        add(signOutPanel);
    }

    private void refreshSessionIfPossible(ElgSessionState aSession)
    {
        if (aSession.getRefreshToken() == null) {
            return;
        }
        
        try {
            elgAuthenticationClient.refreshToken(aSession.getRefreshToken());
        }
        catch (IOException e) {
            aSession.clear();
            error("Signing out from ELG as session count not be refreshed: " + getRootCauseMessage(e));
        }
        
        preferencesService.saveDefaultTraitsForProject(KEY_ELG_SESSION_STATE,
                project.getObject(), aSession);
    }

    @SuppressWarnings("unchecked")
    public IModel<ElgSessionState> getModel()
    {
        return (IModel<ElgSessionState>) getDefaultModel();
    }

    public ElgSessionState getModelObject()
    {
        return (ElgSessionState) getDefaultModelObject();
    }

    private void actionSignOut(AjaxRequestTarget aTarget)
    {
        try {
            ElgSessionState session = getModelObject();
            
            session.clear();

            preferencesService.saveDefaultTraitsForProject(KEY_ELG_SESSION_STATE,
                    project.getObject(), session);

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
            ElgSessionState session = getModelObject();
            
            session.update(elgAuthenticationClient.getToken(tokenCode));

            preferencesService.saveDefaultTraitsForProject(KEY_ELG_SESSION_STATE,
                    project.getObject(), session);

            aTarget.add(this);
        }
        catch (Exception e) {
            error("Unable to sign in: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }
    
    private IModel<String> getUserIdentity() {
        return getModel().map(ElgSessionState::getAccessToken).map(accessToken -> {
            try {
                return elgAuthenticationClient.getUserInfo(accessToken).getPreferredUsername();
            }
            catch (IOException e) {
                return "Unable to retrieve identity: " + ExceptionUtils.getRootCauseMessage(e);
            }
        });
    }
}
