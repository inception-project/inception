/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.security;

import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.SettingsUtil;

public class OverridableUserDetailsManager
    extends JdbcUserDetailsManager
{
    private final Log log = LogFactory.getLog(getClass());

    @Override
    protected List<GrantedAuthority> loadUserAuthorities(String aUsername)
    {
        List<GrantedAuthority> authorities = super.loadUserAuthorities(aUsername);

        Properties settings = SettingsUtil.getSettings();

        String extraRoles = settings.getProperty("auth.user." + aUsername + ".roles");
        if (StringUtils.isNotBlank(extraRoles)) {
            for (String role : extraRoles.split(",")) {
                try {
                    authorities.add(new SimpleGrantedAuthority(role.trim()));
                    log.debug("Added extra role to user [" + aUsername + "]: [" + role.trim() + "]");
                }
                catch (IllegalArgumentException e) {
                    log.debug("Ignoring unknown extra role [" + role + "] for user [" + aUsername
                            + "]");
                }
            }
        }

        return authorities;
    }
}
