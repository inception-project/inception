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
package de.tudarmstadt.ukp.clarin.webanno.security.model;

/**
 * Global user roles.
 */
public enum Role
{
    /**
     * System-wide administrator privilege.
     */
    ROLE_ADMIN,
    
    /**
     * Project user such as manager, annotator or curator.
     */
    ROLE_USER, 
    
    /**
     * Privilege for web-service based user access
     */
    ROLE_REMOTE,

    /**
     * Project user with the right to create new projects.
     */
    ROLE_PROJECT_CREATOR;
}
