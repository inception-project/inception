/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class CurationVID
    extends VID
{
    private static final long serialVersionUID = -4052847275637346338L;
    
    private final String username;
    
    public CurationVID(String aExtId, String aExtPayload, String aUsername, VID aVID)
    {
        super(aExtId, aVID.getLayerId(), aVID.getId(), aVID.getSubId(), aVID.getAttribute(),
                aVID.getSlot(), aExtPayload);
        username = aUsername;
    }
    
    public String getUsername()
    {
        return username;
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode() * 31 + username.hashCode();
    }

    @Override
    public boolean equals(Object aObj)
    {
        return super.equals(aObj) && ((CurationVID) aObj).getUsername().equals(username);
    }
  
}
