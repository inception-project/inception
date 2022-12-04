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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.service;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgServiceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgServiceResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgSession;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthAccessTokenResponse;
import jakarta.persistence.EntityManager;

public class ElgServiceImpl
    implements ElgService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ElgAuthenticationClient elgAuthenticationClient;
    private final ElgServiceClient elgServiceClient;
    private final EntityManager entityManager;

    public ElgServiceImpl(ElgAuthenticationClient aElgAuthenticationClient,
            ElgServiceClient aElgServiceClient, EntityManager aEntityManager)
    {
        elgServiceClient = aElgServiceClient;
        elgAuthenticationClient = aElgAuthenticationClient;
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public Optional<ElgSession> getSession(Project aProject)
    {
        String query = String.join("\n", //
                "FROM ElgSession", //
                "WHERE project = :project");

        return entityManager.createQuery(query, ElgSession.class) //
                .setParameter("project", aProject) //
                .getResultList().stream().findFirst();
    }

    @Override
    @Transactional
    public ElgSession signIn(Project aProject, String aSuccessCode) throws IOException
    {
        log.trace("Signing in to ELG session in project {}", aProject);

        ElgSession session = getSession(aProject).orElse(new ElgSession(aProject));

        session.update(elgAuthenticationClient.getToken(aSuccessCode));

        createOrUpdateSession(session);

        return session;
    }

    @Override
    @Transactional
    public void signOut(Project aProject)
    {
        log.trace("Signing out from ELG session in project {}", aProject);

        String query = String.join("\n", //
                "DELETE ElgSession", //
                "WHERE project = :project");

        entityManager.createQuery(query) //
                .setParameter("project", aProject) //
                .executeUpdate();
    }

    @Override
    @Transactional
    public void refreshSession(ElgSession aSession) throws IOException
    {
        if (aSession.getRefreshToken() == null) {
            return;
        }

        if (!elgAuthenticationClient.requiresRefresh(aSession)) {
            log.trace("Tokens for ELG session in project {} do not need refreshing yet",
                    aSession.getProject());
            return;
        }

        try {
            log.trace("Refreshing tokens for ELG session in project {}", aSession.getProject());
            OAuthAccessTokenResponse response = elgAuthenticationClient
                    .refreshToken(aSession.getRefreshToken());
            aSession.update(response);
            createOrUpdateSession(aSession);
        }
        catch (IOException e) {
            signOut(aSession.getProject());
            throw e;
        }
    }

    @Override
    @Transactional
    public ElgSession createOrUpdateSession(ElgSession aSession)
    {
        Validate.notNull(aSession, "Session must be specified");

        if (isNull(aSession.getId())) {
            entityManager.persist(aSession);
            return aSession;
        }
        else {
            return entityManager.merge(aSession);
        }
    }

    @Override
    @Transactional
    public ElgServiceResponse invokeService(ElgSession aSession, String aServiceSync, String aText)
        throws IOException
    {
        refreshSession(aSession);

        return elgServiceClient.invokeService(aServiceSync, aSession.getAccessToken(), aText);
    }
}
