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
package de.tudarmstadt.ukp.inception.testscenarios.accounts;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;

/**
 * The fixed set of user accounts the test scenarios may use besides the user who creates the
 * project.
 * <p>
 * User accounts are not bound to a project, so scenarios do not create their own. They request one
 * of these through {@link ScenarioAccountService#join}, which creates the account on first use and
 * grants it the project roles declared here. Every account can log in with the password
 * {@value ScenarioAccountService#PASSWORD}.
 * <p>
 * The annotator accounts double as data owners: a scenario that needs annotations by someone other
 * than the current user writes them as one of the annotators.
 */
public enum ScenarioAccount
{
    /**
     * A plain annotator with no other role - for anything a manager or curator would bypass, e.g.
     * the per-document access check.
     */
    ANNOTATOR_1("scenario-annotator-1", "Scenario Annotator 1", PermissionLevel.ANNOTATOR),

    /**
     * A second plain annotator - for a second data owner, or a second set of annotations to compare
     * or curate.
     */
    ANNOTATOR_2("scenario-annotator-2", "Scenario Annotator 2", PermissionLevel.ANNOTATOR),

    /**
     * A curator who is neither manager nor annotator. The project creator always stays a manager,
     * so this is the only way to see the project as a curator alone.
     */
    CURATOR("scenario-curator", "Scenario Curator", PermissionLevel.CURATOR),

    /**
     * A manager who is neither curator nor annotator.
     */
    MANAGER("scenario-manager", "Scenario Manager", PermissionLevel.MANAGER),

    /**
     * A manager who also annotates but is not a curator. The project creator is always a curator as
     * well, so this is the only way to see how a workload treats a manager's own annotation work.
     */
    MANAGER_ANNOTATOR("scenario-manager-annotator", "Scenario Manager-Annotator",
            PermissionLevel.MANAGER, PermissionLevel.ANNOTATOR);

    private final String username;
    private final String uiName;
    private final List<PermissionLevel> projectRoles;

    ScenarioAccount(String aUsername, String aUiName, PermissionLevel... aProjectRoles)
    {
        username = aUsername;
        uiName = aUiName;
        projectRoles = List.of(aProjectRoles);
    }

    public String getUsername()
    {
        return username;
    }

    public String getUiName()
    {
        return uiName;
    }

    /**
     * @return the roles the account is granted in every project it joins.
     */
    public List<PermissionLevel> getProjectRoles()
    {
        return projectRoles;
    }
}
