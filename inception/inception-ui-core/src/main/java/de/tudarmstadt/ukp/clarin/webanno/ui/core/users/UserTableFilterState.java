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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserTableFilterState
    implements Serializable
{
    private static final long serialVersionUID = -6340607235253080789L;

    private String username;
    private final List<UserState> states = new ArrayList<>();

    public List<UserState> getStates()
    {
        return states;
    }

    public void setState(List<UserState> states)
    {
        states.clear();
        if (states != null) {
            states.addAll(states);
        }
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String aUsername)
    {
        username = aUsername;
    }
}
