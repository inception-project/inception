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

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.exporters.SourceDocumentExporter;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EventLoggingAutoConfiguration#loggedEventExporter}.
 * </p>
 */
public class LoggedEventExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String EVENT_LOG = "event.log";

    private final EventRepository eventRepository;
    private final DocumentService documentService;

    @Autowired
    public LoggedEventExporter(EventRepository aEventRepository, DocumentService aDocumentService)
    {
        eventRepository = aEventRepository;
        documentService = aDocumentService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(SourceDocumentExporter.class);
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException
    {
        var project = aRequest.getProject();

        var eventCount = new AtomicInteger(0);
        var missingDocuments = new HashSet<Long>();
        var droppedEvents = new AtomicInteger(0);

        // Set up a map of document IDs to document names because we export by name and not
        // by ID.
        var documentNameIndex = new HashMap<Long, String>();
        documentService.listSourceDocuments(project)
                .forEach(doc -> documentNameIndex.put(doc.getId(), doc.getName()));

        ProjectExporter.writeEntry(aStage, EVENT_LOG, os -> {
            try (var jGenerator = new ObjectMapper().getFactory().createGenerator(os, UTF8)) {
                jGenerator.setPrettyPrinter(new MinimalPrettyPrinter("\n"));

                // Stream data
                eventRepository.forEachLoggedEvent(project, event -> {
                    // check if the export has been cancelled
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    String documentName = null;
                    // If the document ID is -1, then there is no document linked up to this event.
                    // In this case, we do not need to try resolving the IDs to a name.
                    if (event.getDocument() != -1) {
                        documentName = documentNameIndex.get(event.getDocument());
                        if (documentName == null) {
                            // The document has been deleted from the project so we cannot link up
                            // events back up to this document during import. So since this is not
                            // possible, we can even save ourselves the effort of exporting the
                            // logged
                            // events on a document that doesn't exist anymore.
                            missingDocuments.add(event.getDocument());
                            droppedEvents.incrementAndGet();
                            return;
                        }
                    }

                    // Transfer data over to DTO
                    var exportedEvent = ExportedLoggedEvent.fromLoggedEvent(documentName, event);

                    // Write DTO
                    try {
                        jGenerator.writeObject(exportedEvent);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    eventCount.incrementAndGet();
                });
            }
        });

        LOG.info("Exported [{}] logged events for project [{}]", eventCount.get(),
                project.getName());
        if (!missingDocuments.isEmpty()) {
            LOG.info("Skipped [{}] logged events for [{}] documents no longer existing",
                    droppedEvents.get(), missingDocuments.size());
        }
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        int eventCount = 0;

        var entry = ProjectExporter.getEntry(aZip, EVENT_LOG);

        if (entry == null) {
            LOG.info("No event log available for import in project [{}]", aProject.getName());
            return;
        }

        // Query once for all the documents to avoid hitting the DB in the loop below
        var docs = documentService.listSourceDocuments(aProject).stream()
                .collect(Collectors.toMap(SourceDocument::getName, identity()));

        try (var jParser = new ObjectMapper().getFactory()
                .createParser(aZip.getInputStream(entry))) {

            // Persist events in batches to speed up import process
            var batch = new ArrayList<LoggedEvent>();

            var i = jParser.readValuesAs(ExportedLoggedEvent.class);
            while (i.hasNext()) {
                // Flush events
                if (batch.size() >= 50_000) {
                    eventRepository.create(batch.stream().toArray(LoggedEvent[]::new));
                    batch.clear();
                    LOG.trace("... {} events imported ...", eventCount);
                }

                var exportedEvent = i.next();

                var event = new LoggedEvent();
                event.setProject(aProject.getId());
                event.setUser(exportedEvent.getUser());
                event.setEvent(exportedEvent.getEvent());
                event.setCreated(exportedEvent.getCreated());
                event.setAnnotator(exportedEvent.getAnnotator());
                event.setDetails(exportedEvent.getDetails());

                // If an event is not associated with a document, then the default ID -1 is used
                if (exportedEvent.getDocumentName() != null) {
                    event.setDocument(docs.get(exportedEvent.getDocumentName()).getId());
                }
                else {
                    event.setDocument(-1);
                }

                batch.add(event);

                eventCount++;
            }

            // Flush remaining events
            if (!batch.isEmpty()) {
                eventRepository.create(batch.stream().toArray(LoggedEvent[]::new));
            }
        }

        LOG.info("Imported [{}] logged events for project [{}]", eventCount, aProject.getName());
    }
}
