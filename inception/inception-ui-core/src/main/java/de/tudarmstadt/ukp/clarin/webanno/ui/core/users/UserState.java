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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.wicket.HasSymbol;

public enum UserState
    implements HasSymbol
{
    ENABLED("<i class=\"fas fa-user\"></i>"), //
    DEACTIVATED("<i class=\"fas fa-user-lock\"></i>");

    private final String symbol;

    private UserState(String aSymbol)
    {
        symbol = aSymbol;
    }

    public static UserState of(User aUser)
    {
        if (aUser.isEnabled()) {
            return ENABLED;
        }
        else {
            return DEACTIVATED;
        }
    }

    @Override
    public String symbol()
    {
        return symbol;
    }
}
