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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.activity;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageMenuItem;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainLinkCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainLinkDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainSpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainSpanDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel.ActivityOverview;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel.ActivityOverviewItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel.ActivitySummary;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.activity.panel.ActivitySummaryItem;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPageMenuItem;
import jakarta.persistence.NoResultException;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@RestController
@RequestMapping(ActivitiesDashletController.BASE_URL)
public class ActivitiesDashletControllerImpl
    implements ActivitiesDashletController
{
    private final EventRepository eventRepository;
    private final DocumentService documentService;
    private final CurationDocumentService curationDocumentService;
    private final ProjectService projectRepository;
    private final UserDao userRepository;
    private final AnnotationPageMenuItem annotationPageMenuItem;
    private final CurationPageMenuItem curationPageMenuItem;
    private final ServletContext servletContext;

    private final Set<String> annotationEvents = unmodifiableSet( //
            SpanCreatedEvent.class.getSimpleName(), //
            SpanDeletedEvent.class.getSimpleName(), //
            RelationCreatedEvent.class.getSimpleName(), //
            RelationDeletedEvent.class.getSimpleName(), //
            ChainLinkCreatedEvent.class.getSimpleName(), //
            ChainLinkDeletedEvent.class.getSimpleName(), //
            ChainSpanCreatedEvent.class.getSimpleName(), //
            ChainSpanDeletedEvent.class.getSimpleName(), //
            FeatureValueUpdatedEvent.class.getSimpleName(), //
            "DocumentMetadataCreatedEvent", //
            "DocumentMetadataDeletedEvent");

    @Autowired
    public ActivitiesDashletControllerImpl(EventRepository aEventRepository,
            DocumentService aDocumentService, CurationDocumentService aCurationDocumentService,
            ProjectService aProjectRepository, UserDao aUserRepository,
            AnnotationPageMenuItem aAnnotationPageMenuItem,
            CurationPageMenuItem aCurationPageMenuItem, ServletContext aServletContext)
    {
        eventRepository = aEventRepository;
        documentService = aDocumentService;
        curationDocumentService = aCurationDocumentService;
        projectRepository = aProjectRepository;
        userRepository = aUserRepository;
        annotationPageMenuItem = aAnnotationPageMenuItem;
        curationPageMenuItem = aCurationPageMenuItem;
        servletContext = aServletContext;
    }

    @Override
    public String getActivitySummaryUrl(long aProjectId)
    {
        return servletContext.getContextPath() + BASE_URL
                + ACTIVITY_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }

    @Override
    @GetMapping(ACTIVITY_PATH)
    public ActivitySummary activitySummary(@PathVariable("projectId") long aProjectId,
            @RequestParam("from") Optional<String> aFrom, @RequestParam("to") Optional<String> aTo)
    {
        var user = userRepository.getCurrentUser();
        var project = projectRepository.getProject(aProjectId);

        var today = LocalDate.now();
        var beginDate = aFrom.map(this::dateStringToInstant).orElse(today);
        var begin = startOfDay(beginDate);
        var end = endOfDay(aTo.map(this::dateStringToInstant).orElse(beginDate));
        if (!end.isAfter(begin)) {
            end = endOfDay(beginDate);
        }

        if (!projectRepository.hasAnyRole(user, project)) {
            return new ActivitySummary(begin, end, emptyList(), emptyMap());
        }

        var recentEvents = eventRepository.summarizeEvents(user.getUsername(), project, begin, end);
        var documentNameCache = new HashMap<Long, String>();
        var globalItems = new ArrayList<ActivitySummaryItem>();
        var itemsByDoc = new HashMap<String, List<ActivitySummaryItem>>();
        for (var event : recentEvents) {
            if (event.getDocument() > 0) {
                var documentName = documentNameCache.computeIfAbsent(event.getDocument(),
                        docId -> getDocumentName(aProjectId, docId));
                var items = itemsByDoc.computeIfAbsent(documentName, $ -> new ArrayList<>());
                items.add(new ActivitySummaryItem(event.getEvent(), event.getCount()));
            }
            else {
                globalItems.add(new ActivitySummaryItem(event.getEvent(), event.getCount()));
            }
        }

        return new ActivitySummary(begin, end, globalItems, itemsByDoc);
    }

    private String getDocumentName(long aProjectId, Long docId)
    {
        try {
            return documentService.getSourceDocument(aProjectId, docId).getName();
        }
        catch (NoResultException e) {
            return "Deleted document";
        }
    }

    private LocalDate dateStringToInstant(String aDateString)
    {
        return LocalDate.parse(aDateString, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private Instant startOfDay(LocalDate aDate)
    {
        return aDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC).toInstant();
    }

    private Instant endOfDay(LocalDate aDate)
    {
        return aDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();
    }

    private Instant startOfYear(LocalDate aDate)
    {
        return startOfDay(LocalDate.of(aDate.getYear(), Month.JANUARY, 1));
    }

    private Instant endOfYear(LocalDate aDate)
    {
        return endOfDay(LocalDate.of(aDate.getYear(), Month.DECEMBER, 31));
    }

    @Override
    public String getActivityOverviewUrl(long aProjectId)
    {
        return servletContext.getContextPath() + BASE_URL
                + ACTIVITY_OVERVIEW_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }

    @Override
    @GetMapping(ACTIVITY_OVERVIEW_PATH)
    public ActivityOverview activityOverview(@PathVariable("projectId") long aProjectId,
            @RequestParam("year") Optional<Integer> aYear)
    {
        var user = userRepository.getCurrentUser();
        var project = projectRepository.getProject(aProjectId);

        var today = LocalDate.now();
        var year = aYear.map(y -> String.format("%04d-01-01", y)).map(this::dateStringToInstant)
                .orElse(today);
        var begin = startOfYear(year);
        var end = endOfYear(year);

        if (!projectRepository.hasAnyRole(user, project)) {
            return new ActivityOverview(begin, end, emptyMap());
        }

        var recentEvents = eventRepository.summarizeEvents(user.getUsername(), project, begin, end);

        var aggregator = new LinkedHashMap<Instant, AtomicLong>();
        recentEvents.forEach(summarizedEvent -> {
            aggregator.computeIfAbsent(summarizedEvent.getDate(), $ -> new AtomicLong())
                    .addAndGet(summarizedEvent.getCount());
        });

        Map<Instant, ActivityOverviewItem> items = aggregator.entrySet().stream().collect(
                LinkedHashMap::new,
                (m, e) -> m.put(e.getKey(),
                        new ActivityOverviewItem(e.getKey(), e.getValue().get())),
                LinkedHashMap::putAll);

        return new ActivityOverview(begin, end, items);
    }

    @Override
    public String getListActivitiesUrl(long aProjectId)
    {
        return servletContext.getContextPath() + BASE_URL
                + RECENT_ACTIVITY_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }

    @Override
    @GetMapping(RECENT_ACTIVITY_PATH)
    public List<Activity> listActivities(@PathVariable("projectId") long aProjectId)
    {
        var user = userRepository.getCurrentUser();
        var project = projectRepository.getProject(aProjectId);

        if (!projectRepository.hasAnyRole(user, project)) {
            return emptyList();
        }

        var annotatableSourceDocuments = documentService.listAnnotatableDocuments(project, user)
                .keySet().stream() //
                .collect(toMap(SourceDocument::getId, identity()));

        var curatableSourceDocuments = curationDocumentService.listCuratableSourceDocuments(project)
                .stream() //
                .collect(toMap(SourceDocument::getId, identity()));

        var isCurator = projectRepository.hasRole(user, project, CURATOR);

        // get last annotation events
        // return filtered by user rights and document state
        var recentEvents = eventRepository.listRecentActivity(project, user.getUsername(),
                annotationEvents, 10);
        return recentEvents.stream() //
                .filter(Objects::nonNull) //
                .filter(event -> event.getDocument() != -1l) //
                .filter(event -> event.getAnnotator() != null) //
                // Filter out documents which are not annotatable or curatable
                .filter(event -> isEditable(annotatableSourceDocuments, curatableSourceDocuments,
                        isCurator, event))
                // Link events and documents before returning them
                .map(event -> toActivity(project, annotatableSourceDocuments,
                        curatableSourceDocuments, event))//
                .collect(toList());
    }

    private Activity toActivity(Project project,
            Map<Long, SourceDocument> annotatableSourceDocuments,
            Map<Long, SourceDocument> curatableSourceDocuments, LoggedEvent event)
    {
        if (CURATION_USER.equals(event.getAnnotator())) {
            return new Activity(event, curatableSourceDocuments.get(event.getDocument()),
                    curationPageMenuItem.getUrl(project, event.getDocument()));
        }
        else {
            return new Activity(event, annotatableSourceDocuments.get(event.getDocument()),
                    annotationPageMenuItem.getUrl(project, event.getDocument(),
                            event.getAnnotator()));
        }
    }

    private boolean isEditable(Map<Long, SourceDocument> annotatableSourceDocuments,
            Map<Long, SourceDocument> curatableSourceDocuments, boolean isCurator,
            LoggedEvent event)
    {
        if (CURATION_USER.equals(event.getAnnotator())) {
            if (!isCurator) {
                return false;
            }

            return curatableSourceDocuments.containsKey(event.getDocument());
        }
        else {
            return annotatableSourceDocuments.containsKey(event.getDocument());
        }
    }
}
