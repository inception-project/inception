/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response for the {@code whoami} command. Provides the user name of the user currently logged in.
 * Currently not used in the BRAT UI, saved future reference if there is a need to use it, likely for
 * Curation!
 */
public class WhoamiResponse
    extends AjaxResponse
{
    public static final String COMMAND = "whoami";

    private String user;

    public WhoamiResponse(String aUser)
    {
        super(COMMAND);
        user = aUser;
    }

    /**
     * Get the user name.
     */
    public String getUser()
    {
        return user;
    }

    /**
     * Set the user name.
     */
    public void setUser(String aUser)
    {
        user = aUser;
    }
}
