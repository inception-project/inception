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
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Entity
@Table(name = "elg_session", uniqueConstraints = { @UniqueConstraint(columnNames = { "project" }) })
public class ElgSession
    implements Serializable
{
    private static final long serialVersionUID = 5378118174761262214L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @Lob
    @Column(length = 4096)
    private String accessToken;

    @Temporal(TemporalType.TIMESTAMP)
    private Date accessTokenValidUntil;

    @Lob
    @Column(length = 4096)
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

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String aAccessToken)
    {
        accessToken = aAccessToken;
    }

    public Date getAccessTokenValidUntil()
    {
        return accessTokenValidUntil;
    }

    public void setAccessTokenValidUntil(Date aAccessTokenValidUntil)
    {
        accessTokenValidUntil = aAccessTokenValidUntil;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String aRefreshToken)
    {
        refreshToken = aRefreshToken;
    }

    public Date getRefreshTokenValidUntil()
    {
        return refreshTokenValidUntil;
    }

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

    public void clear()
    {
        setAccessToken(null);
        setAccessTokenValidUntil(null);
        setRefreshToken(null);
        setRefreshTokenValidUntil(null);
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
