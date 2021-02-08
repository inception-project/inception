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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;

public enum LinkCompareBehavior
{
    /**
     * The link target is considered to be the label. As a consequence, the
     * {@link Position#compareTo} method includes the role label into comparison but not the link
     * target.
     */
    LINK_TARGET_AS_LABEL,

    /**
     * The link role is considered to be the label and the {@link Position#compareTo} method takes
     * the link target into account
     */
    LINK_ROLE_AS_LABEL;

    public String getName()
    {
        return toString();
    }
}
