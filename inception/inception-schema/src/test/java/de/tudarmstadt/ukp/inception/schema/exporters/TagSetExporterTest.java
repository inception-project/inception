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
package de.tudarmstadt.ukp.inception.schema.exporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
class TagSetExporterTest
{
    private @Mock AnnotationSchemaService schemaService;

    private @TempDir File tempDir;

    private TagSetExporter sut;

    private Project sourceProject;
    private Project targetProject;

    @BeforeEach
    void setup()
    {
        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        sut = new TagSetExporter(schemaService);
    }

    @Test
    void thatExportingAndImportingAgainWorks() throws Exception
    {
        var exportFile = new File(tempDir, "export.zip");

        when(schemaService.listTagSets(any())).thenReturn(tagSets(sourceProject));
        when(schemaService.listTags(any())).then(call -> tags(call.getArgument(0)));

        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        exportedProject.setVersion(32);

        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, exportedProject, zos);
        }

        reset(schemaService);
        var tagSetCaptor = ArgumentCaptor.forClass(TagSet.class);
        doNothing().when(schemaService).createTagSet(tagSetCaptor.capture());
        var tagsCaptor = ArgumentCaptor.forClass(Tag[].class);
        doNothing().when(schemaService).createTags(tagsCaptor.capture());

        // Import the project again
        var importRequest = ProjectImportRequest.builder().build();
        try (var zipFile = new ZipFile(exportFile)) {
            sut.importData(importRequest, targetProject, exportedProject, zipFile);
        }

        assertThat(tagSetCaptor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "project") //
                .containsExactlyElementsOf(tagSets(targetProject));

        assertThat(tagsCaptor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "tagSet.id",
                        "tagSet.project") //
                .containsExactlyElementsOf(tagSets(targetProject).stream()
                        .map(tagSet -> tags(tagSet).toArray(Tag[]::new)).toList());
    }

    private List<Tag> tags(TagSet aTagSet)
    {
        var result = new ArrayList<Tag>();

        for (var i = 1l; i <= 3; i++) {
            var tag = new Tag();
            tag.setId(i);
            tag.setTagSet(aTagSet);
            tag.setName(aTagSet.getName() + " " + i);
            tag.setRank((int) i - 1);
            result.add(tag);
        }

        return result;
    }

    private List<TagSet> tagSets(Project aProject)
    {
        var result = new ArrayList<TagSet>();

        for (var i = 1l; i <= 3; i++) {
            var tagSet = new TagSet();
            tagSet.setId(i);
            tagSet.setProject(aProject);
            tagSet.setName("TagSet " + i);
            result.add(tagSet);
        }

        return result;
    }
}
