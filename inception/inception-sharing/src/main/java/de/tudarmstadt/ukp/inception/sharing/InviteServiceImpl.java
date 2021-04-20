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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.EMPTY_PASSWORD;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.REALM_PROJECT_PREFIX;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.NameUtil;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

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
    public static final int RANDOM_USERNAME_BYTE_LENGTH = 16;

    private @PersistenceContext EntityManager entityManager;

    private final UserDao userRepository;

    private final SecureRandom random;

    public InviteServiceImpl(UserDao aUserRepository)
    {
        random = new SecureRandom();
        userRepository = aUserRepository;
    }

    public InviteServiceImpl(UserDao aUserRepository, EntityManager aEntitymanager)
    {
        this(aUserRepository);
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

    private String generateRandomUsername()
    {
        nextName: while (true) {
            byte[] bytes = new byte[RANDOM_USERNAME_BYTE_LENGTH];
            random.nextBytes(bytes);
            String randomUserId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            // Do not accept base64 values which contain characters that are not safe for file
            // names / not valid as usernames
            if (!NameUtil.isNameValid(randomUserId)) {
                continue nextName;
            }

            if (!userRepository.exists(randomUserId)) {
                return randomUserId;
            }
        }
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

        String query = "SELECT p.inviteId FROM ProjectInvite p " //
                + "WHERE p.project = :givenProject " //
                + "AND p.expirationDate > :now";
        List<String> inviteIDs = entityManager.createQuery(query, String.class)
                .setParameter("givenProject", aProject) //
                .setParameter("now", new Date()) //
                .setMaxResults(1) //
                .getResultList();

        if (inviteIDs.isEmpty()) {
            return null;

        }
        return inviteIDs.get(0);
    }

    @Override
    public boolean isValidInviteLink(Project aProject, String aInviteId)
    {
        if (aInviteId == null) {
            return false;
        }

        String expectedId = getValidInviteID(aProject);
        return expectedId != null && aInviteId.equals(expectedId);
    }

    @Override
    public Date getExpirationDate(Project aProject)
    {
        ProjectInvite invite = readProjectInvite(aProject);
        if (invite == null) {
            return null;
        }
        return invite.getExpirationDate();
    }

    @Transactional
    @Override
    public void extendInviteLinkDate(Project aProject)
    {
        ProjectInvite invite = readProjectInvite(aProject);
        if (invite == null) {
            return;
        }
        Date newExpirationDate = getDateOneYearForth(invite.getExpirationDate());
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
        ProjectInvite invite = readProjectInvite(aProject);
        if (invite == null) {
            generateNewInvite(aProject, aExpirationDate);
            return true;
        }
        invite.setExpirationDate(aExpirationDate);
        entityManager.merge(invite);
        return true;
    }

    @Override
    @Transactional
    public User getOrCreateProjectUser(Project aProject, String aUsername)
    {
        String realm = REALM_PROJECT_PREFIX + aProject.getId();

        User user = userRepository.getUserByRealmAndUiName(realm, aUsername);

        if (user != null) {
            return user;
        }

        User u = new User();
        u.setUsername(generateRandomUsername());
        u.setUiName(aUsername);
        u.setPassword(EMPTY_PASSWORD);
        u.setRealm(REALM_PROJECT_PREFIX + aProject.getId());
        u.setEnabled(true);
        u.setRoles(Set.of(ROLE_USER));
        userRepository.create(u);

        return u;
    }

    @EventListener
    @Transactional
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        userRepository.deleteAllUsersFromRealm(REALM_PROJECT_PREFIX + aEvent.getProject().getId());
    }
}
