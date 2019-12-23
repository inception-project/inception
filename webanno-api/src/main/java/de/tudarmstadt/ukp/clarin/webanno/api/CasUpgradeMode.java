/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api;

public enum CasUpgradeMode
{
    /**
     * Do not upgrade the CAS to the current project type system.
     */
    NO_CAS_UPGRADE,
    
    /**
     * Try automatically detecting if the CAS type system is not up-to-date and perform
     * and upgrade if this is the case.
     */
    AUTO_CAS_UPGRADE,
    
    /**
     * Require a CAS upgrade.
     */
    FORCE_CAS_UPGRADE;
}
