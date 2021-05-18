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
package de.tudarmstadt.ukp.inception.sharing.project.exporters;

import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.NOT_ALLOWED;

import java.io.Serializable;
import java.util.Date;

import de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness;

public class ExportedProjectInvite
    implements Serializable
{
    private static final long serialVersionUID = 9070188939479197080L;

    private String inviteId;
    private String userIdPlaceholder;
    private Date expirationDate;
    private String invitationText;
    private Date created;
    private Date updated;
    private boolean guestAccessible;
    private Mandatoriness askForEMail;
    private boolean disableOnAnnotationComplete;
    private int maxAnnotatorCount;

    public String getInviteId()
    {
        return inviteId;
    }

    public void setInviteId(String aInviteId)
    {
        inviteId = aInviteId;
    }

    public String getUserIdPlaceholder()
    {
        return userIdPlaceholder;
    }

    public void setUserIdPlaceholder(String aUserIdPlaceholder)
    {
        userIdPlaceholder = aUserIdPlaceholder;
    }

    public Date getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(Date aExpirationDate)
    {
        expirationDate = aExpirationDate;
    }

    public String getInvitationText()
    {
        return invitationText;
    }

    public void setInvitationText(String aInvitationText)
    {
        invitationText = aInvitationText;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }

    public boolean isGuestAccessible()
    {
        return guestAccessible;
    }

    public void setGuestAccessible(boolean aGuestAccessible)
    {
        guestAccessible = aGuestAccessible;
    }

    public Mandatoriness getAskForEMail()
    {
        return askForEMail != null ? askForEMail : NOT_ALLOWED;
    }

    public void setAskForEMail(Mandatoriness aAskForEMail)
    {
        askForEMail = aAskForEMail != null ? aAskForEMail : NOT_ALLOWED;
    }

    public boolean isDisableOnAnnotationComplete()
    {
        return disableOnAnnotationComplete;
    }

    public void setDisableOnAnnotationComplete(boolean aDisableOnAnnotationComplete)
    {
        disableOnAnnotationComplete = aDisableOnAnnotationComplete;
    }

    public int getMaxAnnotatorCount()
    {
        return maxAnnotatorCount;
    }

    public void setMaxAnnotatorCount(int aMaxAnnotatorCount)
    {
        maxAnnotatorCount = aMaxAnnotatorCount;
    }
}
