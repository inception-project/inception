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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "elg_session", uniqueConstraints = { @UniqueConstraint(columnNames = { "project" }) })
public class ElgSession
    implements Serializable, OAuthSession
{
    private static final long serialVersionUID = 5378118174761262214L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @Column(length = 64000)
    private String accessToken;

    @Temporal(TemporalType.TIMESTAMP)
    private Date accessTokenValidUntil;

    @Column(length = 64000)
    private String refreshToken;

    @Temporal(TemporalType.TIMESTAMP)
    private Date refreshTokenValidUntil;

    public ElgSession()
    {
        // Required for JPA
    }

    public ElgSession(Project aProject)
    {
        project = aProject;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    @Override
    public String getAccessToken()
    {
        return accessToken;
    }

    @Override
    public void setAccessToken(String aAccessToken)
    {
        accessToken = aAccessToken;
    }

    @Override
    public Date getAccessTokenValidUntil()
    {
        return accessTokenValidUntil;
    }

    @Override
    public void setAccessTokenValidUntil(Date aAccessTokenValidUntil)
    {
        accessTokenValidUntil = aAccessTokenValidUntil;
    }

    @Override
    public String getRefreshToken()
    {
        return refreshToken;
    }

    @Override
    public void setRefreshToken(String aRefreshToken)
    {
        refreshToken = aRefreshToken;
    }

    @Override
    public Date getRefreshTokenValidUntil()
    {
        return refreshTokenValidUntil;
    }

    @Override
    public void setRefreshTokenValidUntil(Date aRefreshTokenValidUntil)
    {
        refreshTokenValidUntil = aRefreshTokenValidUntil;
    }

    public void update(ElgTokenResponse response)
    {
        setAccessToken(response.getAccessToken());
        if (response.getExpiresIn() > 0) {
            setAccessTokenValidUntil(
                    new Date(response.getSubmitTime() + (response.getExpiresIn() * 1000)));
        }
        else {
            setAccessTokenValidUntil(null);
        }
        setRefreshToken(response.getRefreshToken());
        if (response.getRefreshExpiresIn() > 0) {
            setRefreshTokenValidUntil(
                    new Date(response.getSubmitTime() + (response.getRefreshExpiresIn() * 1000)));
        }
        else {
            setRefreshTokenValidUntil(null);
        }
    }

    @Override
    public Instant getLastUpdate()
    {
        throw new NotImplementedException();
    }

    @Override
    public void setLastUpdate(Instant aDate)
    {
        // Ignore
    }

    @Override
    public Duration getAccessTokenExpiresIn()
    {
        return Duration.ofMillis(new Date().getTime() - getAccessTokenValidUntil().getTime());
    }

    @Override
    public void setAccessTokenExpiresIn(Duration aTime)
    {
        // Ignore
    }

    @Override
    public Duration getRefreshTokenExpiresIn()
    {
        return Duration.ofMillis(new Date().getTime() - getRefreshTokenValidUntil().getTime());
    }

    @Override
    public void setRefreshTokenExpiresIn(Duration aTime)
    {
        // Ignore
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ElgSession)) {
            return false;
        }
        ElgSession castOther = (ElgSession) other;
        return Objects.equals(project, castOther.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project);
    }
}
