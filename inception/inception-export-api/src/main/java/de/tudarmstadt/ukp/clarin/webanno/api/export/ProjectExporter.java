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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.removeStart;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.function.FailableConsumer;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface ProjectExporter
{
    default List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return Collections.emptyList();
    }

    default List<Class<? extends ProjectExporter>> getExportDependencies()
    {
        return Collections.emptyList();
    }

    void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aZOs)
        throws ProjectExportException, IOException, InterruptedException;

    void importData(ProjectImportRequest aRequest, Project aProject, ExportedProject aExProject,
            ZipFile aZip)
        throws Exception;

    static String normalizeEntryName(ZipEntry aEntry)
    {
        // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
        var entryName = aEntry.toString();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }

        return entryName;
    }

    static ZipEntry getEntry(ZipFile aFile, String aEntryName)
    {
        var entryName = new ZipEntry(removeStart(normalize(aEntryName, true), "/"));
        var entry = aFile.getEntry(aEntryName);
        if (entry != null) {
            return entry;
        }
        return aFile.getEntry("/" + entryName);
    }

    static void writeEntry(ZipOutputStream aStage, String aEntryName,
            FailableConsumer<OutputStream, IOException> aWriter)
        throws IOException
    {
        var entryName = new ZipEntry(removeStart(normalize(aEntryName, true), "/"));
        aStage.putNextEntry(entryName);
        try {
            aWriter.accept(CloseShieldOutputStream.wrap(aStage));
        }
        finally {
            aStage.closeEntry();
        }
    }
}
