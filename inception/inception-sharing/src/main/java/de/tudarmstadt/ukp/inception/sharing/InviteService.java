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
package de.tudarmstadt.ukp.inception.sharing;

import java.util.Date;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

public interface InviteService
{
    /**
     * @return random expiring invite id for the project and save to database
     * 
     * @param aProject
     *            the given project
     */
    String generateInviteID(Project aProject);

    /**
     * Delete invite id for the project if it exists
     * 
     * @param aProject
     *            the given project
     */
    void removeInviteID(Project aProject);

    /**
     * @return invite id for given project if it exists and has expired yet.
     * 
     * @param aProject
     *            the given project
     */
    String getValidInviteID(Project aProject);

    /**
     * @return if given invite ID is valid for the given project
     * 
     * @param aProject
     *            the relevant project
     * @param aInviteId
     *            invite Id to check
     */
    boolean isValidInviteLink(Project aProject, String aInviteId);

    /**
     * @return the expiration date of the invite link belonging to the given project
     * 
     * @param aProject
     *            the corresponding project
     */
    Date getExpirationDate(Project aProject);

    /**
     * Extend validity of the invite link associated with the given project for another year.
     * 
     * @param aProject
     *            a project
     */
    void extendInviteLinkDate(Project aProject);

    /**
     * Set expiration date of the invite link of the given project or generate new invite link with
     * the given date
     * 
     * @param aProject
     *            the project
     * @param aExpirationDate
     *            the new expiration date
     * @return if invite was generated or date was updated
     */
    boolean generateInviteWithExpirationDate(Project aProject, Date aExpirationDate);

    ProjectInvite readProjectInvite(Project aProject);

    void writeProjectInvite(ProjectInvite aInvite);

    boolean isProjectAnnotationComplete(ProjectInvite aInvite);

    boolean isDateExpired(ProjectInvite aInvite);

    boolean isMaxAnnotatorCountReached(ProjectInvite aInvite);

    String getFullInviteLinkUrl(ProjectInvite aInvite);
}
