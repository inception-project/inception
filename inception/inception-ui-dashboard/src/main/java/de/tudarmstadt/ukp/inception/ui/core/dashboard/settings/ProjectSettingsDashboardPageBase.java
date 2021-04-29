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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.preferences.Key;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;

/**
 * Project settings menu page.
 */
public class ProjectSettingsDashboardPageBase
    extends ProjectPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Key<Boolean> KEY_PINNED = new Key<>(Boolean.class,
            "project-settings-menu/pinned");

    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean PreferencesService preferencesService;

    private DashboardMenu menu;
    private ChallengeResponseDialog deleteProjectDialog;

    public ProjectSettingsDashboardPageBase(final PageParameters aParameters)
    {
        super(aParameters);

        User user = userRepository.getCurrentUser();
        if (!userRepository.isAdministrator(user)) {
            requireProjectRole(user, MANAGER);
        }
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        menu = new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems));
        menu.setPinState(new LambdaModelAdapter.Builder<Boolean>() //
                .getting(() -> preferencesService.loadTraitsForUser(KEY_PINNED,
                        userRepository.getCurrentUser())) //
                .setting(v -> preferencesService.saveTraitsForUser(KEY_PINNED,
                        userRepository.getCurrentUser(), v)) //
                .build());
        add(menu);

        add(new Label("projectName", LoadableDetachableModel.of(() -> getProject().getName())));

        add(new LambdaAjaxLink("delete", this::actionDelete).onConfigure(
                (_this) -> _this.setEnabled(getProject() != null && getProject().getId() != null)));

        IModel<String> projectNameModel = PropertyModel.of(getProject(), "name");
        add(deleteProjectDialog = new ChallengeResponseDialog("deleteProjectDialog",
                new StringResourceModel("DeleteProjectDialog.title", this),
                new StringResourceModel("DeleteProjectDialog.text", this)
                        .setModel(getProjectModel()).setParameters(projectNameModel),
                projectNameModel));
        deleteProjectDialog.setConfirmAction(this::actionDeletePerform);
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        deleteProjectDialog.show(aTarget);
    }

    private void actionDeletePerform(AjaxRequestTarget aTarget)
    {
        try {
            projectService.removeProject(getProject());
            setResponsePage(getApplication().getHomePage());
        }
        catch (IOException e) {
            LOG.error("Unable to remove project :" + ExceptionUtils.getRootCauseMessage(e));
            error("Unable to remove project " + ":" + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream()
                .filter(item -> item.getPath().matches("/settings/[^/]+"))
                .collect(Collectors.toList());
    }
}
