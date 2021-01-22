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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class InviteServiceImpl
    implements InviteService
{
    private @PersistenceContext EntityManager entityManager;

    private final SecureRandom random;
    private final Base64.Encoder urlEncoder;

    public InviteServiceImpl()
    {
        random = new SecureRandom();
        urlEncoder = Base64.getUrlEncoder().withoutPadding();
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
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.YEAR, 1);
        long expirationDate = calendar.getTime().getTime();
        // generate id
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String inviteID = urlEncoder.encodeToString(bytes);

        entityManager.merge(new ProjectInvite(aProject.getId(), inviteID, expirationDate));

        return inviteID;
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

        String query = "FROM ProjectInvite " + "WHERE projectId = :project";
        List<ProjectInvite> inviteIDs = entityManager.createQuery(query, ProjectInvite.class)
                .setParameter("project", aProject.getId()).getResultList();

        if (inviteIDs.isEmpty()) {
            return null;
        }
        return inviteIDs.get(0);
    }

    @Override
    public String getValidInviteID(Long aProjectId)
    {
        Validate.notNull(aProjectId, "Project ID must be given");

        String query = "SELECT p.inviteId FROM ProjectInvite p " + "WHERE p.projectId = :project "
                + "AND p.expirationDate > :now";
        List<String> inviteIDs = entityManager.createQuery(query, String.class)
                .setParameter("project", aProjectId).setParameter("now", new Date().getTime())
                .getResultList();

        if (inviteIDs.isEmpty()) {
            return null;
        }
        return inviteIDs.get(0);
    }

    @Override
    public boolean isValidInviteLink(Long aProjectId, String aInviteId)
    {
        if (aInviteId == null) {
            return false;
        }
        String expectedId = getValidInviteID(aProjectId);
        return expectedId != null && aInviteId.equals(expectedId);
    }

}
