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

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;

public class OverridableUserDetailsManager
    extends JdbcUserDetailsManager
    implements InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private AuthenticationConfiguration authenticationConfiguration;

    public OverridableUserDetailsManager(DataSource aDataSource,
            AuthenticationConfiguration aAuthenticationConfiguration)
    {
        super(aDataSource);
        authenticationConfiguration = aAuthenticationConfiguration;
    }

    @Override
    protected void initDao()
    {
        try {
            setAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
        }
        catch (Exception e) {
            throw new ApplicationContextException(e.getMessage(), e);
        }

        super.initDao();
    }

    @Override
    public List<GrantedAuthority> loadUserAuthorities(String aUsername)
    {
        List<GrantedAuthority> authorities = super.loadUserAuthorities(aUsername);

        Properties settings = SettingsUtil.getSettings();

        String extraRoles = settings.getProperty("auth.user." + aUsername + ".roles");
        if (StringUtils.isNotBlank(extraRoles)) {
            for (String role : extraRoles.split(",")) {
                try {
                    authorities.add(new SimpleGrantedAuthority(role.trim()));
                    log.debug(
                            "Added extra role to user [" + aUsername + "]: [" + role.trim() + "]");
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
