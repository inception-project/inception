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

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InviteServiceAutoConfiguration#inviteService()}.
 * </p>
 */
public class InviteServiceImpl
    implements InviteService
{
    private @PersistenceContext EntityManager entityManager;

    private final SecureRandom random;

    public InviteServiceImpl()
    {
        random = new SecureRandom();
    }

    public InviteServiceImpl(EntityManager aEntitymanager)
    {
        this();
        entityManager = aEntitymanager;
    }

    @Override
    @Transactional
    public String generateInviteID(Project aProject)
    {
        // set expiration date one year from now
        Date expirationDate = getDateOneYearForth(new Date());
        // generate id
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String inviteID = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        removeInviteID(aProject);
        entityManager.flush();
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
        ProjectInvite invite = getProjectInvite(aProject);

        if (invite == null) {
            return;
        }

        entityManager.remove(invite);
    }

    private ProjectInvite getProjectInvite(Project aProject)
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
        ProjectInvite invite = getProjectInvite(aProject);
        if (invite == null) {
            return null;
        }
        return invite.getExpirationDate();
    }

    @Transactional
    @Override
    public void extendInviteLinkDate(Project aProject)
    {
        ProjectInvite invite = getProjectInvite(aProject);
        if (invite == null) {
            return;
        }
        Date newExpirationDate = getDateOneYearForth(invite.getExpirationDate());
        invite.setExpirationDate(newExpirationDate);
        entityManager.merge(invite);
    }
}
