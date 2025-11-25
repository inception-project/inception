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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

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

    private @Id String username;

    private String password;

    @Column(nullable = true)
    private String realm;

    @Column(nullable = true)
    private String uiName;

    @Column(name = "enabled", nullable = false)
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

    @Column(name = "opt_unique_key", nullable = true, length = 64)
    private String optUniqueKey;

    public User()
    {
        // No-args constructor required for ORM.
    }

    public User(String aName, String aUiName)
    {
        username = aName;
        uiName = aName;
        enabled = true;
    }

    /**
     * This constructor is mainly intended for testing.
     * 
     * @param aName
     *            used as the username <b>and</b> the UI name!
     * @param aRoles
     *            roles of the user
     */
    public User(String aName, Role... aRoles)
    {
        username = aName;
        uiName = aName;
        enabled = true;
        if (aRoles != null) {
            roles = new HashSet<>(asList(aRoles));
        }
    }

    private User(Builder builder)
    {
        username = builder.username;
        realm = builder.realm;
        uiName = builder.uiName;
        enabled = builder.enabled;
        lastLogin = builder.lastLogin;
        email = builder.email;
        roles = builder.roles;
        created = builder.created;
        updated = builder.updated;
        setPassword(builder.password);
    }

    private synchronized PasswordEncoder getPasswordEncoder()
    {
        if (passwordEncoder == null) {
            var context = ApplicationContextProvider.getApplicationContext();
            passwordEncoder = context.getBean("passwordEncoder", PasswordEncoder.class);
        }
        return passwordEncoder;
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
        password = aPassword != null ? getPasswordEncoder().encode(aPassword) : null;
    }

    public void setEncodedPassword(String aPassword)
    {
        password = aPassword;
    }

    public String getRealm()
    {
        return realm;
    }

    public void setRealm(String aRealm)
    {
        realm = aRealm;
    }

    public String getUiName()
    {
        if (isBlank(uiName)) {
            if (CURATION_USER.equals(username)) {
                return "Curator";
            }

            return username;
        }

        return uiName;
    }

    public void setUiName(String aUiName)
    {
        uiName = aUiName;
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

    public void setEmail(String aEMail)
    {
        email = aEMail;
    }

    public Set<Role> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<Role> aRoles)
    {
        if (aRoles != null) {
            roles = new HashSet<>(aRoles);
        }
        else {
            roles = new HashSet<>();
        }
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
        updateOptUniqueKey();
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
        updateOptUniqueKey();
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

    public void updateOptUniqueKey()
    {
        // For project-bound users, the UI name must be unique because in the guest annotator mode,
        // we use the UI name during "authentication".
        if (realm != null && realm.startsWith(Realm.REALM_PROJECT_PREFIX)) {
            var digest = DigestUtils.getSha256Digest();
            digest.update(realm.getBytes(UTF_8));
            digest.update((byte) 0x01);
            digest.update(getUiName().getBytes(UTF_8));
            optUniqueKey = Hex.encodeHexString(digest.digest());
        }
        else {
            optUniqueKey = null;
        }
    }

    public String toLongString()
    {
        var builder = new StringBuilder();

        if (uiName != null) {
            builder.append(uiName);
            if (!username.equals(uiName)) {
                builder.append(" (");
                builder.append(username);
                builder.append(")");
            }
        }
        else {
            builder.append(username);
        }

        if (!isEnabled()) {
            builder.append(" (deactivated)");
        }

        return builder.toString();
    }

    @Override
    public String toString()
    {
        return "[" + username + "]";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String username;
        private String password;
        private String realm;
        private String uiName;
        private boolean enabled;
        private Date lastLogin;
        private String email;
        private Set<Role> roles = new HashSet<>();
        private Date created;
        private Date updated;

        private Builder()
        {
        }

        public Builder withUsername(String aUsername)
        {
            this.username = aUsername;
            return this;
        }

        public Builder withPassword(String aPassword)
        {
            this.password = aPassword;
            return this;
        }

        public Builder withRealm(String aRealm)
        {
            this.realm = aRealm;
            return this;
        }

        public Builder withRealm(Realm aRealm)
        {
            this.realm = aRealm.getId();
            return this;
        }

        public Builder withUiName(String aUiName)
        {
            this.uiName = aUiName;
            return this;
        }

        public Builder withEnabled(boolean aEnabled)
        {
            this.enabled = aEnabled;
            return this;
        }

        public Builder withLastLogin(Date aLastLogin)
        {
            this.lastLogin = aLastLogin;
            return this;
        }

        public Builder withEmail(String aEmail)
        {
            this.email = aEmail;
            return this;
        }

        public Builder withRoles(Role... aRoles)
        {
            this.roles = Set.of(aRoles);
            return this;
        }

        public Builder withRoles(Set<Role> aRoles)
        {
            this.roles = aRoles;
            return this;
        }

        public Builder withCreated(Date aCreated)
        {
            this.created = aCreated;
            return this;
        }

        public Builder withUpdated(Date aUpdated)
        {
            this.updated = aUpdated;
            return this;
        }

        public User build()
        {
            return new User(this);
        }
    }
}
