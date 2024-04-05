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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.ADMIN_DEFAULT_USERNAME;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.Objects;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class InitialAccountCreationPanel
    extends Panel
{
    private static final long serialVersionUID = -4851128376067531994L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userService;
    private @SpringBean LoginProperties loginProperties;

    @SuppressWarnings("unused")
    private String username = ADMIN_DEFAULT_USERNAME;
    private String password;
    private String password2;

    public InitialAccountCreationPanel(String aId)
    {
        super(aId);

        queue(new WebMarkupContainer("noUserWelcomeMessage")
                .add(visibleWhen(() -> userService.isEmpty())));

        queue(new WebMarkupContainer("recoveryWelcomeMessage")
                .add(visibleWhen(() -> userService.isAdminAccountRecoveryMode())));

        var form = new StatelessForm<InitialAccountCreationPanel>("form",
                new CompoundPropertyModel<>(this))
        {
            private static final long serialVersionUID = 1050852689215968429L;

            @Override
            protected void onSubmit()
            {
                actionCreateInitialAccount();
            }
        };

        form.add(new RequiredTextField<String>("username").setOutputMarkupId(true)
                .setEnabled(false));
        form.add(new PasswordTextField("password").add(this::validatePassword).setRequired(true)
                .setOutputMarkupId(true));
        form.add(new PasswordTextField("password2").add(this::validatePassword).setRequired(true)

                .setOutputMarkupId(true));
        form.add(new Button("createAccount"));

        add(form);
    }

    private void validatePassword(IValidatable<String> aValidatable)
    {
        userService.validatePassword(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void actionCreateInitialAccount()
    {
        if (!Objects.equals(password, password2)) {
            error("Passwords do not match. Please enter the same password in both fields.");
            return;
        }

        if (userService.exists(ADMIN_DEFAULT_USERNAME)) {
            var admin = userService.get(ADMIN_DEFAULT_USERNAME);
            resetAdminAccount(admin);
            userService.update(admin);
            var msg = "Default admin account password has been updated and permissions have be reset.";
            success(msg);
            LOG.info(msg);
            return;
        }

        createAdminAccount();

        var msg = "Default admin account password and permissions have be reset.";
        warn(msg);
        LOG.info(msg);
    }

    private void createAdminAccount()
    {
        var admin = new User();
        admin.setUsername(ADMIN_DEFAULT_USERNAME);
        resetAdminAccount(admin);
        userService.create(admin);
    }

    private void resetAdminAccount(User admin)
    {
        admin.setPassword(password);
        admin.setEnabled(true);
        admin.setRoles(EnumSet.of(ROLE_ADMIN, ROLE_USER));
    }
}
