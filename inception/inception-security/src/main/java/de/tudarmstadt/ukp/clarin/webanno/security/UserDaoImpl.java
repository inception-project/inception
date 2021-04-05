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
package de.tudarmstadt.ukp.clarin.webanno.security;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Implementation of methods defined in the {@link UserDao} interface
 */
@Component("userRepository")
public class UserDaoImpl
    implements UserDao
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired(required = false) SecurityProperties securityProperties;
    private @Autowired(required = false) PlatformTransactionManager transactionManager;

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        if (securityProperties.getDefaultAdminPassword() == null) {
            return;
        }

        if (transactionManager == null) {
            return;
        }

        new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
            if (list().isEmpty()) {
                User admin = new User();
                admin.setUsername(ADMIN_DEFAULT_USERNAME);
                admin.setEncodedPassword(securityProperties.getDefaultAdminPassword());
                admin.setEnabled(true);
                if (securityProperties.isDefaultAdminRemoteAccess()) {
                    admin.setRoles(EnumSet.of(ROLE_ADMIN, ROLE_USER, ROLE_REMOTE));
                }
                else {
                    admin.setRoles(EnumSet.of(ROLE_ADMIN, ROLE_USER));
                }
                create(admin);
            }
        });
    }

    @Override
    @Transactional
    public boolean exists(final String aUsername)
    {
        return entityManager
                .createQuery("FROM " + User.class.getName() + " o WHERE o.username = :username")
                .setParameter("username", aUsername).getResultList().size() > 0;
    }

    @Override
    @Transactional
    public void create(User aUser)
    {
        entityManager.persist(aUser);
        entityManager.flush();
    }

    @Override
    @Transactional
    public User update(User aUser)
    {
        return entityManager.merge(aUser);
    }

    @Override
    @Transactional
    public int delete(String aUsername)
    {
        User toDelete = get(aUsername);
        if (toDelete == null) {
            return 0;
        }
        else {
            delete(toDelete);
            return 1;
        }
    }

    @Override
    @Transactional
    public void delete(User aUser)
    {
        entityManager.remove(entityManager.merge(aUser));
    }

    @Override
    @Transactional
    public User get(String aUsername)
    {
        Validate.notBlank(aUsername, "User must be specified");

        return entityManager.find(User.class, aUsername);
    }

    @Override
    @Transactional
    public List<User> list()
    {
        return entityManager.createQuery("FROM " + User.class.getName(), User.class)
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listEnabledUsers()
    {
        String query = "FROM " + User.class.getName() + " WHERE enabled = :enabled";

        return entityManager.createQuery(query, User.class).setParameter("enabled", true)
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listDisabledUsers()
    {
        String query = "FROM " + User.class.getName() + " WHERE enabled = :enabled";

        return entityManager.createQuery(query, User.class).setParameter("enabled", false)
                .getResultList();
    }

    @Override
    @Transactional
    public User getCurrentUser()
    {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return get(username);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> listAuthorities(User aUser)
    {
        String query = "FROM Authority " + "WHERE username = :username";
        return entityManager.createQuery(query, Authority.class) //
                .setParameter("username", aUser) //
                .getResultList();
    }

    /**
     * Check if the user has global administrator permissions.
     */
    @Override
    @Transactional
    public boolean isAdministrator(User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aUser)) {
            if (Role.ROLE_ADMIN.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    @Override
    public boolean isCurrentUserAdmin()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> ROLE_ADMIN.toString().equals(auth));
    }

    /**
     * Check if the user has the permission to create projects.
     */
    @Override
    @Transactional
    public boolean isProjectCreator(User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aUser)) {
            if (Role.ROLE_PROJECT_CREATOR.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    @Override
    @Transactional
    public Set<String> getRoles(User aUser)
    {
        // When looking up roles for the user who is currently logged in, then we look in the
        // security context - otherwise we ask the database.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Set<String> roles = new HashSet<>();
        if (authentication != null && aUser.getUsername().equals(authentication.getName())) {
            for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities()) {
                roles.add(ga.getAuthority());
            }
        }
        else {
            for (Authority a : listAuthorities(aUser)) {
                roles.add(a.getAuthority());
            }
        }
        return roles;
    }

    @Override
    public long countEnabledUsers()
    {
        String query = String.join("\n", //
                "SELECT COUNT(*)", //
                "FROM " + User.class.getName(), //
                "WHERE enabled = :enabled");

        return entityManager.createQuery(query, Long.class).setParameter("enabled", true)
                .getSingleResult();
    }

    @Override
    public String getCurrentUsername()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null ? authentication.getName() : null;
    }
}
