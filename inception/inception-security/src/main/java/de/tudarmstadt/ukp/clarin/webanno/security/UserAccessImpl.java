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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link SecurityAutoConfiguration#userAccess}.
 * </p>
 */
public class UserAccessImpl
    implements UserAccess
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserDao userService;

    public UserAccessImpl(UserDao aUserService)
    {
        userService = aUserService;
    }

    @Override
    public boolean isUser(String aUsername)
    {
        log.trace("Permission check: isUser [user: {}]", aUsername);

        if (StringUtils.isBlank(aUsername)) {
            return false;
        }

        return userService.getCurrentUsername().equals(aUsername);
    }
}
