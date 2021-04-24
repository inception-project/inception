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
package de.tudarmstadt.ukp.clarin.webanno.security.model;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;

/**
 * User entity corresponding to the Spring standard schema. Conformance to this schema is the reason
 * why a plural is used for the table name.
 *
 * @see <a href=
 *      "http://static.springsource.org/spring-security/site/docs/3.0.x/reference/appendix-schema.html">Spring
 *      standard schema</a>
 */
@Entity
@Table(name = "users")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User
    implements Serializable
{
    private static final long serialVersionUID = -5668208834434334005L;

    private @Autowired @Transient transient PasswordEncoder passwordEncoder;

    @Id
    private String username;

    private String password;

    private boolean enabled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date lastLogin;

    @Column(nullable = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authorities", joinColumns = {
            @JoinColumn(name = "username", referencedColumnName = "username") })
    @Column(nullable = true, name = "authority")
    @Enumerated(EnumType.STRING)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Role> roles = new HashSet<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

    public User()
    {
        // No-args constructor required for ORM.
    }

    /**
     * This constructor is mainly intended for testing.
     */
    public User(String aName, Role... aRoles)
    {
        username = aName;
        enabled = true;
        if (aRoles != null) {
            roles = new HashSet<>(asList(aRoles));
        }
    }

    private String encodePassword(String aPassword)
    {
        if (passwordEncoder == null) {
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            passwordEncoder = context.getBean("passwordEncoder", PasswordEncoder.class);
        }
        return passwordEncoder.encode(aPassword);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        }
        else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String aLogin)
    {
        username = aLogin;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String aPassword)
    {
        password = encodePassword(aPassword);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public Set<Role> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<Role> roles)
    {
        this.roles = roles;
    }

    public Date getLastLogin()
    {
        return lastLogin;
    }

    public void setLastLogin(Date aLastLogin)
    {
        lastLogin = aLastLogin;
    }

    @PrePersist
    protected void onCreate()
    {
        // When we import data, we set the fields via setters and don't want these to be
        // overwritten by this event handler.
        if (created == null) {
            created = new Date();
            updated = created;
        }
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }
}
