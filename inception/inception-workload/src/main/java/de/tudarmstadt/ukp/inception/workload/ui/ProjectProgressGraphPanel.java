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

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;

public class ProjectProgressGraphPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 56663501568696461L;

    private @SpringBean EventRepository eventRepository;
    private @SpringBean DocumentService documentService;

    public ProjectProgressGraphPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);
    }

    /**
     * Calculates data points backwards from the current state back to the time indicated by the
     * {@code aTo} parameter.
     * 
     * @param aProject
     *            the project
     * @param aTo
     *            time to stop calculating
     * @return list of historical state snapshots
     */
    List<ProgressDataPoint> calculateData(Project aProject, Instant aTo)
    {
        // Get current state from DocumentService
        var stats = documentService.getSourceDocumentStats(aProject);

        // Convert to map format expected by EventRepository
        var currentCounts = new LinkedHashMap<SourceDocumentState, Long>();
        currentCounts.put(NEW, stats.getNewAnnotations());
        currentCounts.put(ANNOTATION_IN_PROGRESS, stats.getAnnotationsInProgress());
        currentCounts.put(ANNOTATION_FINISHED, stats.getFinishedAnnotations());
        currentCounts.put(CURATION_IN_PROGRESS, stats.getCurationsInProgress());
        currentCounts.put(CURATION_FINISHED, stats.getCurationsFinished());

        // Calculate historical states using backwards replay
        var snapshots = eventRepository.calculateHistoricalDocumentStates(aProject, currentCounts,
                aTo);

        // Convert to ProgressDataPoint format
        return snapshots.stream() //
                .map(s -> new ProgressDataPoint(s.day(), s.counts())) //
                .toList();
    }

    static class ProgressDataPoint
    {
        Instant day;
        Map<SourceDocumentState, Integer> counts;

        ProgressDataPoint(Instant aDay, Map<SourceDocumentState, Integer> aCounts)
        {
            day = aDay;
            counts = aCounts;
        }
    }
}
