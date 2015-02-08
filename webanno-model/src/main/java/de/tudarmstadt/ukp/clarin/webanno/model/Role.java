/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Global User roles (ROLE_ADMIN- system-wide administrator privilege, ROLE_USER - project level
 * privilege such as project admin, annotator or curator ROLE_REMORE - Privilege for web-service
 * based user access
 *
 * @author Seid Muhie Yimam
 */
public enum Role
{
    ROLE_ADMIN, ROLE_USER, ROLE_REMOTE, ROLE_PROJECT_CREATOR;

    public static Set<Role> getRoles()
    {
        return new HashSet<Role>(Arrays.asList(values()));
    }
}
