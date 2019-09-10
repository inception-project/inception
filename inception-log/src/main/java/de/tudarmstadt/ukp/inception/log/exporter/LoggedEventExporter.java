/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.SourceDocumentExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@Component
public class LoggedEventExporter implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(LoggedEventExporter.class);

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
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aFile)
        throws Exception
    {
        Project project = aRequest.getProject();
        
        AtomicInteger eventCount = new AtomicInteger(0);
        Set<Long> missingDocuments = new HashSet<>();
        AtomicInteger droppedEvents = new AtomicInteger(0);
        
        // Set up a map of document IDs to document names because we export by name and not
        // by ID.
        Map<Long, String> documentNameIndex = new HashMap<>();
        documentService.listSourceDocuments(project).forEach(doc -> {
            documentNameIndex.put(doc.getId(), doc.getName());
        });
        
        File eventLog = new File(aFile, EVENT_LOG);
        eventLog.createNewFile();
        try (JsonGenerator jGenerator = new ObjectMapper().getFactory()
                .createGenerator(new FileOutputStream(eventLog), JsonEncoding.UTF8)) {

            jGenerator.setPrettyPrinter(new MinimalPrettyPrinter("\n"));
            
            // Stream data
            eventRepository.forEachLoggedEvent(project, event -> {
                String documentName = null;
                // If the document ID is -1, then there is no document linked up to this event.
                // In this case, we do not need to try resolving the IDs to a name.
                if (event.getDocument() != -1) {
                    documentName = documentNameIndex.get(event.getDocument());
                    if (documentName == null) {
                        // The document has been deleted from the project so we cannot link up
                        // events back up to this document during import. So since this is not
                        // possible, we can even save ourselves the effort of exporting the logged
                        // events on a document that doesn't exist anymore.
                        missingDocuments.add(event.getDocument());
                        droppedEvents.incrementAndGet();
                        return;
                    }
                }

                // Transfer data over to DTO
                ExportedLoggedEvent exportedEvent = new ExportedLoggedEvent();
                exportedEvent.setId(event.getId());
                exportedEvent.setCreated(event.getCreated());
                exportedEvent.setDocumentName(documentName);
                exportedEvent.setEvent(event.getEvent());
                exportedEvent.setAnnotator(event.getAnnotator());
                exportedEvent.setUser(event.getUser());
                exportedEvent.setDetails(event.getDetails());
                
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
        
        ZipEntry entry = aZip.getEntry(EVENT_LOG);
        
        if (entry == null) {
            LOG.info("No event log available for import in project [{}]", aProject.getName());
            return;
        }

        LoadingCache<String, SourceDocument> documentCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .build(documentName -> documentService
                        .getSourceDocument(aProject, documentName));

        
        try (JsonParser jParser = new ObjectMapper().getFactory()
                .createParser(aZip.getInputStream(entry))) {

            // Persist events in batches to speed up import process
            List<LoggedEvent> batch = new ArrayList<>();

            Iterator<ExportedLoggedEvent> i = jParser.readValuesAs(ExportedLoggedEvent.class);
            while (i.hasNext()) {
                // Flush events
                if (batch.size() >= 25_000) {
                    eventRepository.create(batch.stream().toArray(LoggedEvent[]::new));
                    batch.clear();
                    LOG.trace("... {} events imported ...", eventCount);
                }
                
                ExportedLoggedEvent exportedEvent = i.next();
                
                LoggedEvent event = new LoggedEvent();
                event.setProject(aProject.getId());
                event.setUser(exportedEvent.getUser());
                event.setEvent(exportedEvent.getEvent());
                event.setCreated(exportedEvent.getCreated());
                event.setAnnotator(exportedEvent.getAnnotator());
                event.setDetails(exportedEvent.getDetails());

                // If an event is not associated with a document, then the default ID -1 is used
                if (exportedEvent.getDocumentName() != null) {
                    event.setDocument(documentCache.get(exportedEvent.getDocumentName()).getId());
                }
                else {
                    event.setDocument(-1);
                }
                
                batch.add(event);

                eventCount++;
            }

            // Flush remaining events
            eventRepository.create(batch.stream().toArray(LoggedEvent[]::new));

        }
        
        LOG.info("Imported [{}] logged events for project [{}]", eventCount, aProject.getName());
    }
}
