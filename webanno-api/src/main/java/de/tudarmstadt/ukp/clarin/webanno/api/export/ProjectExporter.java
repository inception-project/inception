/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface ProjectExporter
{
    default List<Class<? extends ProjectExporter>> getImportDependencies() {
        return Collections.emptyList();
    }

    default List<Class<? extends ProjectExporter>> getExportDependencies() {
        return Collections.emptyList();
    }
    
    void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception;

    void importData(ProjectImportRequest aRequest, Project aProject, ExportedProject aExProject,
            ZipFile aZip)
        throws Exception;
    
    static String normalizeEntryName(ZipEntry aEntry)
    {
        // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
        String entryName = aEntry.toString();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
       
        return entryName;
    }

}
