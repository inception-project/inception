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
package de.tudarmstadt.ukp.inception.sharing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.NOT_ALLOWED;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceProperties;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InviteServiceAutoConfiguration#inviteService}.
 * </p>
 */
public class InviteServiceImpl
    implements InviteService
{
    public static final int INVITE_ID_BYTE_LENGTH = 16;

    private @PersistenceContext EntityManager entityManager;

    private final UserDao userRepository;
    private final ProjectService projectService;
    private final InviteServiceProperties inviteProperties;
    private final WorkloadManagementService workloadManagementService;

    private final SecureRandom random;

    public InviteServiceImpl(UserDao aUserRepository, ProjectService aProjectService,
            InviteServiceProperties aInviteProperties,
            WorkloadManagementService aWorkloadManagementService)
    {
        random = new SecureRandom();
        userRepository = aUserRepository;
        projectService = aProjectService;
        inviteProperties = aInviteProperties;
        workloadManagementService = aWorkloadManagementService;
    }

    public InviteServiceImpl(UserDao aUserRepository, ProjectService aProjectService,
            InviteServiceProperties aInviteProperties,
            WorkloadManagementService aWorkloadManagementService, EntityManager aEntitymanager)
    {
        this(aUserRepository, aProjectService, aInviteProperties, aWorkloadManagementService);
        entityManager = aEntitymanager;
    }

    @Override
    @Transactional
    public String generateInviteID(Project aProject)
    {
        // remove old invite if necessary
        removeInviteID(aProject);
        // set expiration date one year from now
        Date expirationDate = getDateOneYearForth(new Date());
        String inviteID = generateNewInvite(aProject, expirationDate);

        return inviteID;
    }

    private String generateNewInvite(Project aProject, Date expirationDate)
    {
        // generate id
        byte[] bytes = new byte[INVITE_ID_BYTE_LENGTH];
        random.nextBytes(bytes);
        String inviteID = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        entityManager.persist(new ProjectInvite(aProject, inviteID, expirationDate));
        return inviteID;
    }

    /**
     * Get date one year from the given one
     */
    private Date getDateOneYearForth(Date aDate)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(aDate);
        calendar.add(Calendar.YEAR, 1);
        Date futureDate = calendar.getTime();
        return futureDate;
    }

    @Override
    @Transactional
    public void removeInviteID(Project aProject)
    {
        ProjectInvite invite = readProjectInvite(aProject);

        if (invite == null) {
            return;
        }

        entityManager.remove(invite);
        entityManager.flush();
    }

    @Override
    @Transactional
    public ProjectInvite readProjectInvite(Project aProject)
    {
        Validate.notNull(aProject, "Project must be given");

        String query = "FROM ProjectInvite " //
                + "WHERE project = :givenProject";
        List<ProjectInvite> inviteIDs = entityManager.createQuery(query, ProjectInvite.class)
                .setParameter("givenProject", aProject) //
                .getResultList();

        if (inviteIDs.isEmpty()) {
            return null;
        }

        return inviteIDs.get(0);
    }

    @Override
    @Transactional
    public void writeProjectInvite(ProjectInvite aInvite)
    {
        if (!aInvite.isGuestAccessible()) {
            aInvite.setAskForEMail(NOT_ALLOWED);
        }

        if (aInvite.getId() == null) {
            entityManager.persist(aInvite);
        }
        else {
            entityManager.merge(aInvite);
        }
    }

    @Override
    public String getValidInviteID(Project aProject)
    {
        Validate.notNull(aProject, "Project must be given");
        Validate.notNull(aProject.getId(), "Project be saved");

        ProjectInvite invite = readProjectInvite(aProject);

        if (invite == null) {
            return null;
        }

        if (isDateExpired(invite) || isProjectAnnotationComplete(invite)
                || isMaxAnnotatorCountReached(invite)) {
            return null;
        }

        return invite.getInviteId();
    }

    @Override
    public boolean isProjectAnnotationComplete(ProjectInvite aInvite)
    {
        if (!aInvite.isDisableOnAnnotationComplete()) {
            return false;
        }

        // Get the freshest project state from the DB
        ProjectState state = workloadManagementService
                .getWorkloadManagerExtension(aInvite.getProject())
                .freshenStatus(aInvite.getProject());

        return state.isAnnotationFinal();
    }

    @Override
    public boolean isDateExpired(ProjectInvite aInvite)
    {
        if (aInvite.getExpirationDate() == null) {
            return false;
        }

        return aInvite.getExpirationDate().before(new Date());
    }

    @Override
    public boolean isMaxAnnotatorCountReached(ProjectInvite aInvite)
    {
        if (aInvite.getMaxAnnotatorCount() <= 0) {
            return false;
        }

        var annotatorCount = projectService
                .listUsersWithRoleInProject(aInvite.getProject(), ANNOTATOR).size();

        return annotatorCount >= aInvite.getMaxAnnotatorCount();
    }

    @Override
    public boolean isValidInviteLink(Project aProject, String aInviteId)
    {
        if (aInviteId == null) {
            return false;
        }

        var expectedId = getValidInviteID(aProject);
        return expectedId != null && aInviteId.equals(expectedId);
    }

    @Override
    public Date getExpirationDate(Project aProject)
    {
        var invite = readProjectInvite(aProject);
        if (invite == null) {
            return null;
        }
        return invite.getExpirationDate();
    }

    @Transactional
    @Override
    public void extendInviteLinkDate(Project aProject)
    {
        var invite = readProjectInvite(aProject);
        if (invite == null) {
            return;
        }

        var newExpirationDate = getDateOneYearForth(invite.getExpirationDate());
        invite.setExpirationDate(newExpirationDate);
        entityManager.merge(invite);
    }

    @Override
    @Transactional
    public boolean generateInviteWithExpirationDate(Project aProject, Date aExpirationDate)
    {
        if (aExpirationDate.getTime() < new Date().getTime()) {
            return false;
        }

        var invite = readProjectInvite(aProject);
        if (invite == null) {
            generateNewInvite(aProject, aExpirationDate);
            return true;
        }

        invite.setExpirationDate(aExpirationDate);
        entityManager.merge(invite);
        return true;
    }

    @Override
    public String getFullInviteLinkUrl(ProjectInvite aInvite)
    {
        // If a base URL is set in the properties, we use that.
        if (isNotBlank(inviteProperties.getInviteBaseUrl())) {
            var url = new StringBuilder();
            url.append(inviteProperties.getInviteBaseUrl().trim());
            while (url.charAt(url.length() - 1) == '/') {
                url.setLength(url.length() - 1);
            }

            var project = aInvite.getProject();
            var projectSegment = project.getSlug() != null ? project.getSlug()
                    : String.valueOf(project.getId());

            url.append(NS_PROJECT);
            url.append("/");
            url.append(projectSegment);
            url.append("/");
            url.append("join-project");
            url.append("/");
            url.append(aInvite.getInviteId());
            return url.toString();
        }

        // If we are in a Wicket context, we can use Wicket to render the URL for us
        var cycle = RequestCycle.get();
        if (cycle != null) {
            var pageParameters = new PageParameters();
            AcceptInvitePage.setProjectPageParameter(pageParameters, aInvite.getProject());
            pageParameters.set(AcceptInvitePage.PAGE_PARAM_INVITE_ID, aInvite.getInviteId());

            var url = cycle.urlFor(AcceptInvitePage.class, pageParameters);

            return RequestCycle.get().getUrlRenderer().renderFullUrl(Url.parse(url));
        }

        throw new IllegalStateException("Please set the property [sharing.invites.invite-base-url] "
                + "in the settings.properties file");
    }

    @EventListener
    @Transactional
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        userRepository
                .deleteAllUsersFromRealm(Realm.REALM_PROJECT_PREFIX + aEvent.getProject().getId());
    }
}
