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
package de.tudarmstadt.ukp.inception.log.exporter;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@ExtendWith(MockitoExtension.class)
public class LoggedEventExporterTest
{
    public @TempDir File tempFolder;

    private @Mock DocumentService documentService;
    private @Mock EventRepository eventRepository;

    private Project project;
    private File workFolder;

    private LoggedEventExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        workFolder = tempFolder;

        when(documentService.listSourceDocuments(any())).thenReturn(documents());

        sut = new LoggedEventExporter(eventRepository, documentService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        doAnswer((Answer<Void>) invocation -> {
            FailableConsumer<LoggedEvent, Exception> consumer = invocation.getArgument(1);
            Streams.stream(events()).forEach(consumer);
            return null;
        }).when(eventRepository).forEachLoggedEvent(any(), any());

        ZipFile zipFile = mock(ZipFile.class);
        when(zipFile.getEntry(any())).thenReturn(new ZipEntry("event.log"));
        when(zipFile.getInputStream(any()))
                .thenAnswer(_invocation -> new FileInputStream(new File(workFolder, "event.log")));

        // Export the project and import it again
        ArgumentCaptor<LoggedEvent> captor = runExportImportAndFetchEvents(zipFile);

        // Check that after re-importing the exported projects, they are identical to the original
        List<LoggedEvent> expectedEvents = events().stream()
                // The document with the ID 2 does supposedly not exist, so it is skipped
                // during export
                .filter(e -> e.getDocument() != 2l).collect(toList());
        assertThat(captor.getAllValues()).usingElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(expectedEvents);
    }

    @Test
    public void thatImportingArchiveWithoutEventsWorks() throws Exception
    {
        ZipFile zipFile = mock(ZipFile.class);

        // Export the project and import it again
        ArgumentCaptor<LoggedEvent> captor = runExportImportAndFetchEvents(zipFile);

        // Check that import was successful but not events have been imported
        assertThat(captor.getAllValues()).isEmpty();
    }

    private List<SourceDocument> documents()
    {
        SourceDocument doc1 = new SourceDocument();
        doc1.setId(1l);
        doc1.setName("doc1");
        doc1.setProject(project);

        return asList(doc1);
    }

    private List<LoggedEvent> events()
    {
        LoggedEvent event1 = new LoggedEvent(1l);
        event1.setUser("user");
        event1.setCreated(new Date(782341234123l));
        event1.setDocument(1l);
        event1.setEvent("SomeEvent1");
        event1.setProject(project.getId());
        event1.setAnnotator("annotator");
        event1.setDetails("{\"value\":1}");

        LoggedEvent event2 = new LoggedEvent(2l);
        event2.setUser("user");
        event2.setCreated(new Date(782341234124l));
        event2.setDocument(2l);
        event2.setEvent("SomeEvent2");
        event2.setProject(project.getId());
        event2.setAnnotator("annotator");
        event2.setDetails("{\"value\":1}");

        LoggedEvent event3 = new LoggedEvent(3l);
        event3.setUser("user");
        event3.setCreated(new Date(782341234125l));
        event3.setDocument(1l);
        event3.setEvent("SomeEvent3");
        event3.setProject(project.getId());
        event3.setAnnotator("annotator");
        event3.setDetails("{\"value\":2}");

        LoggedEvent event4 = new LoggedEvent(3l);
        event4.setUser("user");
        event4.setCreated(new Date(782341234126l));
        // This event is not associated with a document thus the document ID is -1
        event4.setDocument(-1l);
        event4.setEvent("SomeEvent3");
        event4.setProject(project.getId());
        event4.setAnnotator("annotator");
        event4.setDetails("{\"value\":2}");

        return asList(event1, event2, event3, event4);
    }

    private ArgumentCaptor<LoggedEvent> runExportImportAndFetchEvents(ZipFile aZipFile)
        throws Exception
    {
        // Export the project
        FullProjectExportRequest exportRequest = new FullProjectExportRequest(project, null, false);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(project, null, "test");
        ExportedProject exportedProject = new ExportedProject();

        sut.exportData(exportRequest, monitor, exportedProject, workFolder);

        // Import the project again
        ArgumentCaptor<LoggedEvent> captor = ArgumentCaptor.forClass(LoggedEvent.class);
        lenient().doNothing().when(eventRepository).create(captor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        sut.importData(importRequest, project, exportedProject, aZipFile);

        return captor;
    }
}
