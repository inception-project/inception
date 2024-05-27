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
package de.tudarmstadt.ukp.clarin.webanno.api.casstorage;

public enum CasUpgradeMode
{
    /**
     * Do not upgrade the CAS to the current project type system. Avoiding the CAS upgrade has two
     * important effects:
     * <ul>
     * <li>The feature structure addresses in the CAS do not change. This is important because these
     * addresses are used e.g. in UI layer to uniquely identify and access annotations.</li>
     * <li>The access is faster because the upgrade is skipped.</li>
     * </ul>
     */
    NO_CAS_UPGRADE,

    /**
     * Try automatically detecting if the CAS type system is not up-to-date and perform and upgrade
     * if this is the case.
     */
    AUTO_CAS_UPGRADE,

    /**
     * Require a CAS upgrade.
     */
    FORCE_CAS_UPGRADE;
}
