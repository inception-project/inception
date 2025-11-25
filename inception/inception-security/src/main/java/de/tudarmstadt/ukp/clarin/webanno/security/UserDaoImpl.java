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

import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.FILESYSTEM_RESERVED_CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.containsAnyCharacterMatching;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.sortAndRemoveDuplicateCharacters;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.startsWithMatching;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.UserProfileProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User_;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

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
    private static final String MSG_USERNAME_ILLEGAL_PREFIX = "username.name.error.illegal-prefix";
    private static final String MSG_USERNAME_ERROR_ILLEGAL_SPACE = "username.error.illegal-space";
    private static final String MSG_USERNAME_ERROR_BLANK = "username.error.blank";
    private static final String MSG_UINAME_ERROR_TOO_LONG = "ui-name.error.too-long";
    private static final String MSG_EMAIL_INVALID = "email.error.invalid";
    private static final String MSG_EMAIL_ERROR_TOO_LONG = "email.error.too-long";

    private static final String USERNAME_ILLEGAL_PREFIX_CHARACTERS = FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS;
    private static final String USERNAME_ILLEGAL_CHARACTERS = sortAndRemoveDuplicateCharacters(
            "^/\\&*?+$![]", FILESYSTEM_RESERVED_CHARACTERS);

    public static final Set<String> RESERVED_USERNAMES = Set.of(INITIAL_CAS_PSEUDO_USER,
            CURATION_USER, "anonymousUser");

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private final SecurityProperties securityProperties;
    private final UserProfileProperties userProfileProperties;
    private final PlatformTransactionManager transactionManager;
    private final SessionRegistry sessionRegistry;

    public UserDaoImpl(EntityManager aEntityManager, SecurityProperties aSecurityProperties,
            UserProfileProperties aUserProfileProperties,
            PlatformTransactionManager aTransactionManager, SessionRegistry aSessionRegistry)
    {
        entityManager = aEntityManager;
        securityProperties = aSecurityProperties;
        transactionManager = aTransactionManager;
        sessionRegistry = aSessionRegistry;
        userProfileProperties = aUserProfileProperties;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        installDefaultAdminUser();

        ensureUniqueProjectBoundUserKeys();
    }

    void installDefaultAdminUser()
    {
        if (securityProperties == null || securityProperties.getDefaultAdminPassword() == null) {
            return;
        }

        if (transactionManager == null) {
            LOG.warn("No transaction manager - cannot set up default admin user");
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
                var admin = new User();
                admin.setUsername(
                        Objects.toString(defaultAdminUsername, UserDao.ADMIN_DEFAULT_USERNAME));
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

    private void ensureUniqueProjectBoundUserKeys()
    {
        if (transactionManager == null) {
            LOG.warn("No transaction manager - cannot set up unique keys for project-bound users");
            return;
        }

        new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
            var cb = entityManager.getCriteriaBuilder();
            var query = cb.createQuery(User.class);
            var root = query.from(User.class);

            // WHERE u.realm LIKE :prefix AND u.optUniqueKey IS NULL
            var realmPredicate = cb.like(root.get("realm"), Realm.REALM_PROJECT_PREFIX + "%");
            var keyNullPredicate = cb.isNull(root.get("optUniqueKey"));
            query.select(root).where(cb.and(realmPredicate, keyNullPredicate));

            var usersToUpdate = entityManager.createQuery(query).getResultList();

            // Persist again to trigger @PreUpdate
            for (var user : usersToUpdate) {
                user.updateOptUniqueKey();
                entityManager.merge(user);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
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
        LOG.debug("Created new user {} with roles {}", aUser, aUser.getRoles());
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

        var toDelete = get(aUsername);
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
    public List<User> listAllUsersFromRealm(Realm aRealm)
    {
        return listAllUsersFromRealm(aRealm.getId());
    }

    @Override
    @Transactional
    public List<User> listAllUsersFromRealm(String aRealm)
    {
        var query = join("\n", //
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

        String query = join("\n", //
                "DELETE FROM " + User.class.getName(), //
                "WHERE realm = :realm");

        return entityManager.createQuery(query) //
                .setParameter("realm", aRealm) //
                .executeUpdate();
    }

    @Override
    public User getCurationUser()
    {
        var user = new User(CURATION_USER);
        user.setUiName("Curator");
        return user;
    }

    @Override
    public User getInitialCasUser()
    {
        var user = new User(INITIAL_CAS_PSEUDO_USER);
        user.setUiName("Original");
        return user;
    }

    @Override
    @Transactional
    public User getUserOrCurationUser(String aUsername)
    {
        Validate.notBlank(aUsername, "User must be specified");

        if (CURATION_USER.equals(aUsername)) {
            return getCurationUser();
        }

        return get(aUsername);
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
    public User getUserByRealmAndUiName(Realm aRealm, String aUiName)
    {
        return getUserByRealmAndUiName(aRealm.getId(), aUiName);
    }

    @Override
    @Transactional
    public User getUserByRealmAndUiName(String aRealm, String aUiName)
    {
        Validate.notBlank(aUiName, "User must be specified");

        var query = join("\n", //
                "FROM " + User.class.getName(), //
                "WHERE ((:realm is null and realm is null) or realm = :realm)", //
                "AND   uiName = :uiName");

        var users = entityManager.createQuery(query, User.class) //
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
    @Transactional(readOnly = true)
    public boolean isEmpty()
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(User.class);
        query.select(cb.count(root));

        var count = entityManager.createQuery(query).getSingleResult();

        return count == 0;
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
        var query = "FROM " + User.class.getName() + " WHERE enabled = :enabled";

        return entityManager.createQuery(query, User.class) //
                .setParameter("enabled", true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<User> listDisabledUsers()
    {
        var query = "FROM " + User.class.getName() + " WHERE enabled = :enabled";

        return entityManager.createQuery(query, User.class) //
                .setParameter("enabled", false) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<String> listRealms()
    {
        var cr = entityManager.getCriteriaBuilder().createQuery(String.class);
        cr.select(cr.from(User.class) //
                .get(User_.realm)) //
                .distinct(true);
        return entityManager.createQuery(cr).getResultList();
    }

    @Override
    @Transactional
    public User getCurrentUser()
    {
        var username = getCurrentUsername();

        if (username == null) {
            return null;
        }

        return get(username);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<Authority> listAuthorities(User aUser)
    {
        var query = "FROM Authority " + "WHERE username = :username";
        return entityManager.createQuery(query, Authority.class) //
                .setParameter("username", aUser) //
                .getResultList();
    }

    /**
     * Check if the user has global administrator permissions.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAdministrator(User aUser)
    {
        return hasRole(aUser, ROLE_ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProjectCreator(User aUser)
    {
        return hasRole(aUser, ROLE_PROJECT_CREATOR);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(User aUser, Role aRole)
    {
        if (aUser == null) {
            return false;
        }

        for (var role : getRoles(aUser)) {
            if (aRole.name().equals(role)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCurrentUserAdmin()
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream() //
                .map(GrantedAuthority::getAuthority) //
                .anyMatch(auth -> ROLE_ADMIN.name().equals(auth));
    }

    @Override
    public List<ValidationError> validateEmail(String eMail)
    {
        var errors = new ArrayList<ValidationError>();

        var len = eMail.length();
        int maximumUiNameLength = 200;
        if (len > maximumUiNameLength) {
            errors.add(new ValidationError("Email address too long. It can at most consist of "
                    + maximumUiNameLength + " characters.") //
                            .addKey(MSG_EMAIL_ERROR_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumUiNameLength));
        }

        if (!EmailValidator.getInstance().isValid(eMail)) {
            errors.add(new ValidationError("Not a valid email address.") //
                    .addKey(MSG_EMAIL_INVALID));
        }

        return errors;
    }

    @Override
    public List<ValidationError> validateUiName(String aName)
    {
        var errors = new ArrayList<ValidationError>();

        var len = aName.length();
        int maximumUiNameLength = 200;
        if (len > maximumUiNameLength) {
            errors.add(new ValidationError("Display name too long. It can at most consist of "
                    + maximumUiNameLength + " characters.") //
                            .addKey(MSG_UINAME_ERROR_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumUiNameLength));
        }

        return errors;
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
        if (containsAnyCharacterMatching(aName, this::isWhitespaceIllegalInUsername)) {
            errors.add(new ValidationError("Username cannot contain whitespace") //
                    .addKey(MSG_USERNAME_ERROR_ILLEGAL_SPACE));
            return errors;
        }

        if (startsWithMatching(aName, c -> contains(USERNAME_ILLEGAL_PREFIX_CHARACTERS, c))) {
            errors.add(new ValidationError("Username cannot start with any of these characters: "
                    + USERNAME_ILLEGAL_PREFIX_CHARACTERS) //
                            .addKey(MSG_USERNAME_ILLEGAL_PREFIX)
                            .setVariable(MVAR_CHARS, USERNAME_ILLEGAL_PREFIX_CHARACTERS));
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

    private boolean isWhitespaceIllegalInUsername(char ch)
    {
        if (securityProperties.isSpaceAllowedInUsername() && ch == ' ') {
            return false;
        }

        return Character.isWhitespace((int) ch);
    }

    @Override
    public boolean isValidUsername(String aName)
    {
        return validateUsername(aName).isEmpty();
    }

    @Override
    public boolean isValidEmail(String aEMail)
    {
        return validateEmail(aEMail).isEmpty();
    }

    @Override
    public boolean isValidUiName(String aName)
    {
        return validateUiName(aName).isEmpty();
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
        var minimumPasswordLength = securityProperties.getMinimumPasswordLength();
        if (len < minimumPasswordLength) {
            errors.add(new ValidationError("Password too short. It must at least consist of "
                    + minimumPasswordLength + " characters.") //
                            .addKey(MSG_PASSWORD_ERROR_TOO_SHORT)
                            .setVariable(MVAR_LIMIT, minimumPasswordLength));
        }

        var maximumPasswordLength = securityProperties.getMaximumPasswordLength();
        if (len > maximumPasswordLength) {
            errors.add(new ValidationError("Password too long. It can at most consist of "
                    + maximumPasswordLength + " characters.") //
                            .addKey(MSG_PASSWORD_ERROR_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumPasswordLength));
        }

        var passwordPattern = securityProperties.getPasswordPattern();
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
    @Transactional(readOnly = true)
    public Set<String> getRoles(User aUser)
    {
        // When looking up roles for the user who is currently logged in, then we look in the
        // security context - otherwise we ask the database.
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        var roles = new HashSet<String>();
        if (authentication != null && aUser.getUsername().equals(authentication.getName())) {
            for (var ga : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
                roles.add(ga.getAuthority());
            }
        }
        else {
            // Not using listAuthorities() here because that is not read-only
            var query = "FROM Authority " + "WHERE username = :username";
            entityManager.createQuery(query, Authority.class) //
                    .setParameter("username", aUser) //
                    .getResultStream() //
                    .map(Authority::getAuthority) //
                    .forEach(roles::add);
        }

        return roles;
    }

    @Override
    @Transactional(readOnly = true)
    public long countEnabledUsers()
    {
        String query = join("\n", //
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null ? authentication.getName() : null;
    }

    // FIXME: Use DI to get password encoder
    @Override
    public boolean userHasNoPassword(User aUser)
    {
        var applicationContext = ApplicationContextProvider.getApplicationContext();
        var passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
        return aUser.getPassword() == null
                || passwordEncoder.matches(EMPTY_PASSWORD, aUser.getPassword());
    }

    // FIXME: Use DI to get password encoder and environment
    @Override
    public boolean canChangePassword(User aUser)
    {
        var applicationContext = ApplicationContextProvider.getApplicationContext();

        // Just in case the administrator has not run the user account migration of external
        // accounts after the upgrade... because if an external user could change their password,
        // they would be able to log in via form-based login...
        if (ArrayUtils.contains(applicationContext.getEnvironment().getActiveProfiles(),
                PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)) {
            var passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
            if (aUser.getPassword() == null
                    || passwordEncoder.matches(EMPTY_PASSWORD, aUser.getPassword())) {
                return false;
            }
        }

        if (aUser.getRealm() == null) {
            return true; // Local users can change their password
        }

        return false; // External users and project-bound users cannot
    }

    @Override
    public boolean isProfileSelfServiceAllowed(User aUser)
    {
        if (!userProfileProperties.isAccessible()) {
            return false;
        }

        if (startsWith(aUser.getRealm(), Realm.REALM_PROJECT_PREFIX)) {
            // Project-bound users get no access to their profile. They could at most change their
            // display name and email, but since those are basically their logins, we don't want
            // them to be able to do that.
            return false;
        }

        return true;
    }

    @Override
    public boolean isAdminAccountRecoveryMode()
    {
        return System.getProperty(PROP_RESTORE_DEFAULT_ADMIN_ACCOUNT) != null;
    }
}
