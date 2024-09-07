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

import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InviteServiceAutoConfiguration#projectInviteExporter}.
 * </p>
 */
public class ProjectInviteExporter
    implements ProjectExporter
{
    private static final String KEY = "project_invites";
    private static final Logger LOG = LoggerFactory.getLogger(ProjectInviteExporter.class);

    private final InviteService inviteService;

    @Autowired
    public ProjectInviteExporter(InviteService aInviteService)
    {
        inviteService = aInviteService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var project = aRequest.getProject();

        // add project permissions to the project
        var projectInvites = new ArrayList<ExportedProjectInvite>();

        var invite = inviteService.readProjectInvite(project);
        if (invite != null) {
            var exportedInvite = new ExportedProjectInvite();
            exportedInvite.setCreated(invite.getCreated());
            exportedInvite.setExpirationDate(invite.getExpirationDate());
            exportedInvite.setGuestAccessible(invite.isGuestAccessible());
            exportedInvite.setInvitationText(invite.getInvitationText());
            exportedInvite.setInviteId(invite.getInviteId());
            exportedInvite.setUpdated(invite.getUpdated());
            exportedInvite.setUserIdPlaceholder(invite.getUserIdPlaceholder());
            exportedInvite.setAskForEMail(invite.getAskForEMail());
            exportedInvite.setDisableOnAnnotationComplete(invite.isDisableOnAnnotationComplete());
            exportedInvite.setMaxAnnotatorCount(invite.getMaxAnnotatorCount());
            projectInvites.add(exportedInvite);
        }

        aExProject.setProperty(KEY, projectInvites);

        LOG.info("Exported [{}] invites for project [{}]", projectInvites.size(),
                aRequest.getProject().getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var exportedProjectInvites = aExProject.getArrayProperty(KEY, ExportedProjectInvite.class);

        if (exportedProjectInvites.length > 0) {
            var invite = new ProjectInvite();
            var exportedInvite = exportedProjectInvites[0];
            invite.setProject(aProject);
            invite.setCreated(exportedInvite.getCreated());
            invite.setExpirationDate(exportedInvite.getExpirationDate());
            invite.setGuestAccessible(exportedInvite.isGuestAccessible());
            invite.setInvitationText(exportedInvite.getInvitationText());
            invite.setInviteId(exportedInvite.getInviteId());
            invite.setUpdated(exportedInvite.getUpdated());
            invite.setUserIdPlaceholder(exportedInvite.getUserIdPlaceholder());
            invite.setAskForEMail(exportedInvite.getAskForEMail());
            invite.setDisableOnAnnotationComplete(exportedInvite.isDisableOnAnnotationComplete());
            invite.setMaxAnnotatorCount(exportedInvite.getMaxAnnotatorCount());
            inviteService.writeProjectInvite(invite);

            LOG.info("Exported [{}] invites for project [{}]", 1, aProject.getName());
        }
    }
}
