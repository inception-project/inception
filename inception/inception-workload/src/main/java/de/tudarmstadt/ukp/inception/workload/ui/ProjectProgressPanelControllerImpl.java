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
package de.tudarmstadt.ukp.inception.workload.ui;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@RestController
@RequestMapping(ProjectProgressPanelController.BASE_URL)
public class ProjectProgressPanelControllerImpl
    implements ProjectProgressPanelController
{
    private final ServletContext servletContext;
    private final EventRepository eventRepository;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;

    public ProjectProgressPanelControllerImpl(ServletContext aServletContext,
            EventRepository aEventRepository, DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        servletContext = aServletContext;
        eventRepository = aEventRepository;
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getProgressUrl(long aProjectId)
    {
        return servletContext.getContextPath() + BASE_URL
                + PROGRESS_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }

    @Override
    @GetMapping(PROGRESS_PATH)
    public List<DocumentStateSnapshot> progress( //
            @PathVariable("projectId") long aProjectId, //
            @RequestParam("from") Optional<Instant> aFrom, //
            @RequestParam("to") Optional<Instant> aTo, //
            @RequestParam("now") Optional<Instant> aNow)
    {
        var sessionOwner = userService.getCurrentUser();
        var project = projectService.getProject(aProjectId);

        if (!projectService.hasRole(sessionOwner, project, MANAGER, CURATOR)) {
            return emptyList();
        }

        var stats = documentService.getSourceDocumentStats(project);
        var history = eventRepository.calculateHistoricalDocumentStates(project, stats.toMap(),
                aFrom.orElse(null));

        // Filter history to only include snapshots at or before the specified "now" point
        var effectiveNow = aNow.orElse(now());
        if (aNow.isPresent()) {
            history = history.stream() //
                    .filter(snapshot -> !snapshot.day().isAfter(effectiveNow)) //
                    .toList();
        }

        var projector = new TotalConservingDocumentStateProjector();

        // Determine projection horizon. If `to` is provided and is in the future,
        // use the difference in days between effectiveNow and `to`. Otherwise fall back to 30 days.
        int projectionDays = 30;
        if (aTo.isPresent()) {
            var toInstant = aTo.get();
            projectionDays = Math.max(0, (int) DAYS.between(effectiveNow, toInstant));
        }

        // Backproject only the lookback window to daily snapshots for accurate trend calculation
        var dailyHistory = backprojectToDaily(history, 30);

        var future = projectionDays > 0 ? projector.generate(dailyHistory, projectionDays, 30)
                : new ArrayList<DocumentStateSnapshot>();

        // Return sparse historical data + future projections
        var result = new ArrayList<DocumentStateSnapshot>();
        result.addAll(history);
        result.addAll(future);
        return result;
    }

    /**
     * Backprojects sparse historical snapshots into daily snapshots using backward-fill logic. This
     * ensures that regression-based projections correctly account for time between sparse events
     * instead of being biased by clustered timestamps.
     * <p>
     * Uses backward-fill because snapshots represent state BEFORE events at their timestamps. When
     * processing events in reverse (newest-first), each snapshot captures the state that existed
     * before that event fired. To fill gaps, we carry later values backward in time.
     *
     * @param aHistory
     *            Sparse historical snapshots (ordered oldest-first).
     * @param aLookbackDays
     *            Number of days to backproject from the most recent snapshot.
     * @return Daily snapshots with counts carried backward from the most recent following snapshot.
     */
    static List<DocumentStateSnapshot> backprojectToDaily(List<DocumentStateSnapshot> aHistory,
            int aLookbackDays)
    {
        if (aHistory == null || aHistory.isEmpty()) {
            return emptyList();
        }

        if (aHistory.size() == 1) {
            return aHistory;
        }

        var last = aHistory.get(aHistory.size() - 1);
        var startDay = last.day().minus(aLookbackDays, DAYS);

        // Filter history to only snapshots within or after the lookback window
        var relevantHistory = aHistory.stream() //
                .filter(s -> !s.day().isBefore(startDay)) //
                .toList();

        if (relevantHistory.isEmpty()) {
            return emptyList();
        }

        var first = relevantHistory.get(0);
        var totalDays = DAYS.between(startDay, last.day());

        // If all snapshots are already on consecutive days, no backprojection needed
        if (totalDays < relevantHistory.size()) {
            return relevantHistory;
        }

        var result = new ArrayList<DocumentStateSnapshot>();
        var snapshotIndex = 0;

        // Iterate forward from start of lookback window through all days to today
        for (long dayOffset = 0; dayOffset <= totalDays; dayOffset++) {
            var currentDay = startDay.plus(dayOffset, DAYS);

            // Advance pointer to next snapshot at or after currentDay
            while (snapshotIndex < relevantHistory.size() - 1
                    && relevantHistory.get(snapshotIndex).day().isBefore(currentDay)) {
                snapshotIndex++;
            }

            var activeSnapshot = relevantHistory.get(snapshotIndex);

            // Use counts from the next (later) snapshot
            result.add(
                    new DocumentStateSnapshot(currentDay, new HashMap<>(activeSnapshot.counts())));
        }

        return result;
    }
}
