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
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.containsAnyCharacterMatching;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link SecurityAutoConfiguration#userService}.
 * </p>
 */
public class UserDaoImpl
    implements UserDao
{
    private static final String MVAR_CHARS = "chars";
    private static final String MVAR_PATTERN = "pattern";
    private static final String MVAR_LIMIT = "limit";
    private static final String MSG_PASSWORD_ERROR_PATTERN_MISMATCH = "password.error.pattern-mismatch";
    private static final String MSG_PASSWORD_ERROR_TOO_LONG = "password.error.too-long";
    private static final String MSG_PASSWORD_ERROR_TOO_SHORT = "password.error.too-short";
    private static final String MSG_PASSWORD_ERROR_BLANK = "password.error.blank";
    private static final String MSG_PASSWORD_ERROR_CONTROL_CHARACTERS = "password.error.control-characters";
    private static final String MSG_USERNAME_ERROR_PATTERN_MISMATCH = "username.error.pattern-mismatch";
    private static final String MSG_USERNAME_ERROR_TOO_LONG = "username.error.too-long";
    private static final String MSG_USERNAME_ERROR_TOO_SHORT = "username.error.too-short";
    private static final String MSG_USERNAME_ERROR_RESERVED = "username.error.reserved";
    private static final String MSG_USERNAME_ERROR_ILLEGAL_CHARACTERS = "username.error.illegal-characters";
    private static final String MSG_USERNAME_ERROR_CONTROL_CHARACTERS = "username.error.control-characters";
    private static final String MSG_USERNAME_ERROR_ILLEGAL_SPACE = "username.error.illegal-space";
    private static final String MSG_USERNAME_ERROR_BLANK = "username.error.blank";

    private static final String USERNAME_ILLEGAL_CHARACTERS = "^/\\&*?+$![] ";

    private static final Set<String> RESERVED_USERNAMES = Set.of(INITIAL_CAS_PSEUDO_USER,
            CURATION_USER);

    private static final String BAD_FOR_FILENAMES = "#%&{}\\<>*?/ $!'\":@+`|=";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EntityManager entityManager;
    private final SecurityProperties securityProperties;
    private final PlatformTransactionManager transactionManager;
    private final SessionRegistry sessionRegistry;

    public UserDaoImpl(EntityManager aEntityManager, SecurityProperties aSecurityProperties,
            PlatformTransactionManager aTransactionManager, SessionRegistry aSessionRegistry)
    {
        entityManager = aEntityManager;
        securityProperties = aSecurityProperties;
        transactionManager = aTransactionManager;
        sessionRegistry = aSessionRegistry;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        installDefaultAdminUser();
    }

    void installDefaultAdminUser()
    {
        if (securityProperties == null || securityProperties.getDefaultAdminPassword() == null) {
            return;
        }

        if (transactionManager == null) {
            return;
        }

        var defaultAdminUsername = securityProperties.getDefaultAdminUsername();
        if (defaultAdminUsername != null && !isValidUsername(defaultAdminUsername)) {
            throw new IllegalStateException(
                    "Illegal default admin username configured in the settings file: ["
                            + securityProperties.getDefaultAdminUsername() + "]");
        }

        new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
            if (list().isEmpty()) {
                User admin = new User();
                admin.setUsername(defaultString(defaultAdminUsername, ADMIN_DEFAULT_PASSWORD));
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
    public User create(User aUser)
    {
        if (RESERVED_USERNAMES.contains(aUser.getUsername())) {
            throw new IllegalArgumentException("Username [" + aUser.getUsername()
                    + "] is reserved. No user with this name can be created.");
        }

        entityManager.persist(aUser);
        entityManager.flush();
        log.debug("Created new user [" + aUser.getUsername() + "] with roles " + aUser.getRoles());
        return aUser;
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
        if (sessionRegistry != null) {
            sessionRegistry.getAllSessions(aUsername, false)
                    .forEach(_session -> _session.expireNow());
        }

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
        if (sessionRegistry != null) {
            sessionRegistry.getAllSessions(aUser.getUsername(), false)
                    .forEach(_session -> _session.expireNow());
        }

        entityManager.remove(entityManager.merge(aUser));
    }

    @Override
    @Transactional
    public List<User> listAllUsersFromRealm(String aRealm)
    {
        String query = String.join("\n", //
                "FROM " + User.class.getName(), //
                "WHERE realm = :realm");

        return entityManager.createQuery(query, User.class) //
                .setParameter("realm", aRealm) //
                .getResultList();
    }

    @Override
    @Transactional
    public int deleteAllUsersFromRealm(String aRealm)
    {
        if (sessionRegistry != null) {
            List<User> usersInRealm = listAllUsersFromRealm(aRealm);

            for (User user : usersInRealm) {
                sessionRegistry.getAllSessions(user.getUsername(), false)
                        .forEach(_session -> _session.expireNow());
                entityManager.remove(user);
            }

            return usersInRealm.size();
        }
        else {
            String query = String.join("\n", //
                    "DELETE FROM " + User.class.getName(), //
                    "WHERE realm = :realm");

            return entityManager.createQuery(query) //
                    .setParameter("realm", aRealm) //
                    .executeUpdate();
        }
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
    public User getUserByRealmAndUiName(String aRealm, String aUiName)
    {
        Validate.notBlank(aUiName, "User must be specified");

        String query = String.join("\n", //
                "FROM " + User.class.getName(), //
                "WHERE ((:realm is null and realm is null) or realm = :realm)", //
                "AND   uiName = :uiName");

        List<User> users = entityManager.createQuery(query, User.class) //
                .setParameter("realm", aRealm) //
                .setParameter("uiName", aUiName) //
                .getResultList();

        switch (users.size()) {
        case 0:
            return null;
        case 1:
            return users.get(0);
        default:
            throw new IllegalStateException(
                    "UI name [" + aUiName + "] is not unique within realm [" + aRealm + "]");
        }
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

        return entityManager.createQuery(query, User.class) //
                .setParameter("enabled", true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listDisabledUsers()
    {
        String query = "FROM " + User.class.getName() + " WHERE enabled = :enabled";

        return entityManager.createQuery(query, User.class) //
                .setParameter("enabled", false) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<String> listRealms()
    {
        String query = "SELECT DISTINCT realm FROM " + User.class.getName();

        return entityManager.createQuery(query, String.class) //
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
        return hasRole(aUser, Role.ROLE_ADMIN);
    }

    @Override
    @Transactional
    public boolean hasRole(User aUser, Role aRole)
    {
        if (aUser == null) {
            return false;
        }

        for (String role : getRoles(aUser)) {
            if (aRole.name().equals(role)) {
                return true;
            }
        }

        return false;
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
    public List<ValidationError> validateUsername(String aName)
    {
        var errors = new ArrayList<ValidationError>();

        // Do not allow empty or blank usernames
        if (isBlank(aName)) {
            errors.add(new ValidationError("Username cannot be empty or blank") //
                    .addKey(MSG_USERNAME_ERROR_BLANK));
            return errors;
        }

        // Do not allow space
        if (containsAnyCharacterMatching(aName, Character::isWhitespace)) {
            errors.add(new ValidationError("Username cannot contain a space character") //
                    .addKey(MSG_USERNAME_ERROR_ILLEGAL_SPACE));
            return errors;
        }

        // Do not allow problematic characters (note that we use the username in some file names!
        if (containsAny(aName, USERNAME_ILLEGAL_CHARACTERS)) {
            errors.add(new ValidationError("Username cannot contain any of these characters: "
                    + USERNAME_ILLEGAL_CHARACTERS) //
                            .addKey(MSG_USERNAME_ERROR_ILLEGAL_CHARACTERS)
                            .setVariable(MVAR_CHARS, USERNAME_ILLEGAL_CHARACTERS));
            return errors;
        }

        if (containsAnyCharacterMatching(aName, TextUtils::isControlCharacter)) {
            errors.add(new ValidationError("Username cannot contain any control characters") //
                    .addKey(MSG_USERNAME_ERROR_CONTROL_CHARACTERS));
            return errors;
        }

        if (RESERVED_USERNAMES.contains(aName)) {
            errors.add(new ValidationError("Username is reserved") //
                    .addKey(MSG_USERNAME_ERROR_RESERVED));
            return errors;
        }

        // Username is too short or too long
        var len = aName.length();
        int minimumUsernameLength = securityProperties.getMinimumUsernameLength();
        if (len < minimumUsernameLength) {
            errors.add(new ValidationError("Username too short. It must at least consist of "
                    + minimumUsernameLength + " characters.") //
                            .addKey(MSG_USERNAME_ERROR_TOO_SHORT)
                            .setVariable(MVAR_LIMIT, minimumUsernameLength));
        }

        int maximumUsernameLength = securityProperties.getMaximumUsernameLength();
        if (len > maximumUsernameLength) {
            errors.add(new ValidationError("Username too long. It can at most consist of "
                    + maximumUsernameLength + " characters.") //
                            .addKey(MSG_USERNAME_ERROR_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumUsernameLength));
        }

        Pattern usernamePattern = securityProperties.getUsernamePattern();
        if (!usernamePattern.matcher(aName).matches()) {
            errors.add(new ValidationError("Username invalid. It must match the pattern ["
                    + usernamePattern.pattern() + "].") //
                            .addKey(MSG_USERNAME_ERROR_PATTERN_MISMATCH)
                            .setVariable(MVAR_PATTERN, usernamePattern.pattern()));
        }

        return errors;
    }

    @Override
    public boolean isValidUsername(String aName)
    {
        return validateUsername(aName).isEmpty();
    }

    @Override
    public List<ValidationError> validatePassword(String aPassword)
    {
        var errors = new ArrayList<ValidationError>();

        // Do not allow empty or blank passwords
        if (isBlank(aPassword)) {
            errors.add(new ValidationError("Password cannot be empty or blank") //
                    .addKey(MSG_PASSWORD_ERROR_BLANK));
            return errors;
        }

        if (containsAnyCharacterMatching(aPassword, TextUtils::isControlCharacter)) {
            errors.add(new ValidationError("Password cannot contain any control characters") //
                    .addKey(MSG_PASSWORD_ERROR_CONTROL_CHARACTERS));
            return errors;
        }

        // Password is too short or too long
        var len = aPassword.length();
        int minimumPasswordLength = securityProperties.getMinimumPasswordLength();
        if (len < minimumPasswordLength) {
            errors.add(new ValidationError("Password too short. It must at least consist of "
                    + minimumPasswordLength + " characters.") //
                            .addKey(MSG_PASSWORD_ERROR_TOO_SHORT)
                            .setVariable(MVAR_LIMIT, minimumPasswordLength));
        }

        int maximumPasswordLength = securityProperties.getMaximumPasswordLength();
        if (len > maximumPasswordLength) {
            errors.add(new ValidationError("Password too long. It can at most consist of "
                    + maximumPasswordLength + " characters.") //
                            .addKey(MSG_PASSWORD_ERROR_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumPasswordLength));
        }

        Pattern passwordPattern = securityProperties.getPasswordPattern();
        if (!passwordPattern.matcher(aPassword).matches()) {
            errors.add(new ValidationError("Password invalid. It must match the pattern ["
                    + passwordPattern.pattern() + "].") //
                            .addKey(MSG_PASSWORD_ERROR_PATTERN_MISMATCH)
                            .setVariable(MVAR_PATTERN, passwordPattern.pattern()));
        }

        return errors;
    }

    @Override
    public boolean isValidPassword(String aPassword)
    {
        return validatePassword(aPassword).isEmpty();
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

        return entityManager.createQuery(query, Long.class) //
                .setParameter("enabled", true) //
                .getSingleResult();
    }

    @Override
    public String getCurrentUsername()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null ? authentication.getName() : null;
    }
}
