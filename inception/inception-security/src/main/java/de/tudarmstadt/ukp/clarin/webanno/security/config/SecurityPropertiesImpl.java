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
package de.tudarmstadt.ukp.clarin.webanno.security.config;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class is exposed as a Spring Component via {@link SecurityAutoConfiguration}.
 */
@ConfigurationProperties("security")
public class SecurityPropertiesImpl
    implements SecurityProperties
{
    private String defaultAdminUsername;
    private String defaultAdminPassword;
    private boolean defaultAdminRemoteAccess = false;
    private boolean spaceAllowedInUsername = false;

    public static final int HARD_MINIMUM_PASSWORD_LENGTH = 0;
    public static final int DEFAULT_MINIMUM_PASSWORD_LENGTH = 8;
    private int minimumPasswordLength = DEFAULT_MINIMUM_PASSWORD_LENGTH;

    public static final int HARD_MAXIMUM_PASSWORD_LENGTH = 128;
    public static final int DEFAULT_MAXIMUM_PASSWORD_LENGTH = 32;
    private int maximumPasswordLength = DEFAULT_MAXIMUM_PASSWORD_LENGTH;

    public static final int HARD_MINIMUM_USERNAME_LENGTH = 1;
    public static final int DEFAULT_MINIMUM_USERNAME_LENGTH = 4;
    private int minimumUsernameLength = DEFAULT_MINIMUM_USERNAME_LENGTH;

    public static final int HARD_MAXIMUM_USERNAME_LENGTH = 128;
    public static final int DEFAULT_MAXIMUM_USERNAME_LENGTH = 64;
    private int maximumUsernameLength = DEFAULT_MAXIMUM_USERNAME_LENGTH;

    public static final String DEFAULT_USERNAME_PATTERN = ".*";
    private Pattern usernamePattern = Pattern.compile(DEFAULT_USERNAME_PATTERN);

    public static final String DEFAULT_PASSWORD_PATTERN = ".*";
    private Pattern passwordPattern = Pattern.compile(DEFAULT_PASSWORD_PATTERN);

    @Override
    public String getDefaultAdminUsername()
    {
        return defaultAdminUsername;
    }

    public void setDefaultAdminUsername(String aDefaultAdminUsername)
    {
        defaultAdminUsername = aDefaultAdminUsername;
    }

    @Override
    public String getDefaultAdminPassword()
    {
        return defaultAdminPassword;
    }

    public void setDefaultAdminPassword(String aDefaultAdminPassword)
    {
        defaultAdminPassword = aDefaultAdminPassword;
    }

    @Override
    public boolean isDefaultAdminRemoteAccess()
    {
        return defaultAdminRemoteAccess;
    }

    public void setDefaultAdminRemoteAccess(boolean aDefaultAdminRemoteAccess)
    {
        defaultAdminRemoteAccess = aDefaultAdminRemoteAccess;
    }

    @Override
    public int getMinimumPasswordLength()
    {
        return Math.max(minimumPasswordLength, HARD_MINIMUM_PASSWORD_LENGTH);
    }

    public void setMinimumPasswordLength(int aMinimumPasswordLength)
    {
        minimumPasswordLength = Math.max(aMinimumPasswordLength, HARD_MINIMUM_PASSWORD_LENGTH);
    }

    @Override
    public int getMaximumPasswordLength()
    {
        return Math.min(maximumPasswordLength, HARD_MAXIMUM_PASSWORD_LENGTH);
    }

    public void setMaximumPasswordLength(int aMaximumPasswordLength)
    {
        maximumPasswordLength = Math.min(aMaximumPasswordLength, HARD_MAXIMUM_PASSWORD_LENGTH);
    }

    @Override
    public int getMinimumUsernameLength()
    {
        return Math.max(minimumUsernameLength, HARD_MINIMUM_USERNAME_LENGTH);
    }

    public void setMinimumUsernameLength(int aMinimumUsernameLength)
    {
        minimumUsernameLength = Math.max(aMinimumUsernameLength, HARD_MINIMUM_USERNAME_LENGTH);
    }

    @Override
    public int getMaximumUsernameLength()
    {
        return Math.min(maximumUsernameLength, HARD_MAXIMUM_USERNAME_LENGTH);
    }

    public void setMaximumUsernameLength(int aMaximumUsernameLength)
    {
        maximumUsernameLength = Math.min(aMaximumUsernameLength, HARD_MAXIMUM_USERNAME_LENGTH);
    }

    @Override
    public Pattern getUsernamePattern()
    {
        return usernamePattern;
    }

    public void setUsernamePattern(Pattern aUsernamePattern)
    {
        usernamePattern = aUsernamePattern;
    }

    @Override
    public Pattern getPasswordPattern()
    {
        return passwordPattern;
    }

    public void setPasswordPattern(Pattern aPasswordPattern)
    {
        passwordPattern = aPasswordPattern;
    }

    @Override
    public boolean isSpaceAllowedInUsername()
    {
        return spaceAllowedInUsername;
    }

    public void setSpaceAllowedInUsername(boolean aSpaceInUsernameAllowed)
    {
        spaceAllowedInUsername = aSpaceInUsernameAllowed;
    }
}
