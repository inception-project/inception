/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectImportEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;

@Component
public class ConstraintsServiceEventAdapter
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired ConstraintsService service;
    
    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent)
        throws Exception
    {
        //Remove Constraints
        for (ConstraintSet set : service.listConstraintSets(aEvent.getProject())) {
            service.removeConstraintSet(set);
        }
    }
    
    @EventListener
    public void onProjectImport(ProjectImportEvent aEvent)
        throws Exception
    {
        Project project = aEvent.getProject();
        ZipFile zipFile = aEvent.getZip();
        
        for (Enumeration zipEnumerate = zipFile.entries(); zipEnumerate.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(ConstraintsService.CONSTRAINTS + "/")) {
                String fileName = FilenameUtils.getName(entry.getName());
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.setProject(project);
                constraintSet.setName(fileName);
                service.createConstraintSet(constraintSet);
                service.writeConstraintSet(constraintSet, zipFile.getInputStream(entry));
                log.info("Imported constraint [" + fileName + "] for project [" + project.getName()
                        + "] with id [" + project.getId() + "]");
            }
        }
    }
}
