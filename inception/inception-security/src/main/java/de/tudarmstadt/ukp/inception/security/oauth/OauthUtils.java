package de.tudarmstadt.ukp.inception.security.oauth;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class OauthUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Properties settings = SettingsUtil.getSettings();

    public static Set<Role> getOAuth2NewUSerRoles(User aUser, ArrayList<String> oauth2groups)
    {
        Set<Role> roles = new HashSet<>();

        if (!settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_MAPPING_ENABLED).equalsIgnoreCase("true")) {
            roles.add(Role.ROLE_USER);
            return roles;
        }

        if (oauth2groups == null || oauth2groups.isEmpty()) {
            LOG.debug("OAuth2 groups mapping is enabled, but user ["
                + aUser.getUsername() + "] doesn't have any groups, or the corresponding  claim is empty");
        }

        oauth2groups.forEach(group -> matchOauth2groupToRole(group, roles));

        return roles;
    }

    private static void matchOauth2groupToRole(String oauth2group, Set<Role> userRoles) {

        String adminGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_ADMIN);
        String userGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_USER);
        String projectCreatorGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_PROJECT_CREATOR);
        String remoteGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_REMOTE);

        if (oauth2group.equals(adminGroup)) {
            userRoles.add(Role.ROLE_ADMIN);
        }

        if (oauth2group.equals(userGroup)) {
            userRoles.add(Role.ROLE_USER);
        }

        if (oauth2group.equals(projectCreatorGroup)) {
            userRoles.add(Role.ROLE_PROJECT_CREATOR);
        }

        if (oauth2group.equals(remoteGroup)) {
            userRoles.add(Role.ROLE_REMOTE);
        }
    }
}
