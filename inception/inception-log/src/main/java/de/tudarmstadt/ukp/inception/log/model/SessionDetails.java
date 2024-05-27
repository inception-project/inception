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
package de.tudarmstadt.ukp.inception.log.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class SessionDetails
{
    private String sessionId;

    @JsonInclude(Include.NON_DEFAULT)
    private long expiredAfterInactivity;

    @JsonInclude(Include.NON_DEFAULT)
    private long duration;

    public SessionDetails()
    {
        // Nothing to do
    }

    public SessionDetails(String aId)
    {
        sessionId = aId;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String aSessionId)
    {
        sessionId = aSessionId;
    }

    /**
     * @return expired after milliseconds of inactivity
     */
    public long getExpiredAfterInactivity()
    {
        return expiredAfterInactivity;
    }

    public void setExpiredAfterInactivity(long aExpiredAfterInactivity)
    {
        expiredAfterInactivity = aExpiredAfterInactivity;
    }

    /**
     * @return duration of the session in milliseconds.
     */
    public long getDuration()
    {
        return duration;
    }

    public void setDuration(long aDuration)
    {
        duration = aDuration;
    }
}
