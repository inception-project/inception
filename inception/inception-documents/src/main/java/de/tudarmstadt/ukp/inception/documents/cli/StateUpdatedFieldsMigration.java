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
package de.tudarmstadt.ukp.inception.documents.cli;

import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

public class StateUpdatedFieldsMigration
{
    public StateUpdatedFieldsMigration(ProjectService aProjectService,
            EventRepository aEventRepository, DocumentService aDocumentService)
    {
        // If we have projects and none of them has a state_updated set, then we should rebuild
        // this
        var projects = aProjectService.listProjects();
        if (aEventRepository != null && !projects.isEmpty()
                && projects.stream().allMatch(p -> p.getStateUpdated() == null)) {
            BaseLoggers.BOOT_LOG.info(
                    "Detected that [state_updated] information in projects is not present. Rebuilding from event log. This may take a moment...");
            var rebuilder = new RebuildStateUpdatedFieldsCliCommand(aProjectService,
                    aDocumentService, aEventRepository);
            rebuilder.rebuildAll(BaseLoggers.BOOT_LOG);
            BaseLoggers.BOOT_LOG.info("Rebuilding complete.");
        }
    }
}
