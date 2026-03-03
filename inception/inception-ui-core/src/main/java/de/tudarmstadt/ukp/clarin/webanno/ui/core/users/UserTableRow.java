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

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class UserTableRow
    implements Serializable
{
    private static final long serialVersionUID = 3784071856814087251L;

    private final User user;

    private boolean selected;

    public UserTableRow(User aUser)
    {
        user = aUser;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean aSelected)
    {
        selected = aSelected;
    }

    public User getUser()
    {
        return user;
    }
}
