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
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;

public class ProjectProgressGraphPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 56663501568696461L;

    private @SpringBean EventRepository eventRepository;
    private @SpringBean DocumentService documentService;
    private @SpringBean(required = false) ProgressWeighter weightSource;
    private @SpringBean UserDao userService;

    public ProjectProgressGraphPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);
    }

    List<ProgressDataPoint> calculateData(Project aProject, Instant aTo) throws IOException
    {
        return calculateData(aProject, aTo, ProgressMetric.DOCUMENTS);
    }

    /**
     * Calculates data points backwards from the current state back to the time indicated by the
     * {@code aTo} parameter.
     *
     * @param aProject
     *            the project
     * @param aTo
     *            time to stop calculating
     * @param aMetric
     *            unit of the Y axis — {@code DOCUMENTS} counts one per doc, {@code TOKENS} weights
     *            each doc by its token count in the search index
     * @return list of historical state snapshots
     */
    List<ProgressDataPoint> calculateData(Project aProject, Instant aTo, ProgressMetric aMetric)
        throws IOException
    {
        Map<Long, Long> weights = null;
        Map<SourceDocumentState, Long> currentCounts;

        if (aMetric == ProgressMetric.TOKENS) {
            weights = resolveTokenWeights(aProject);
            currentCounts = weightedCurrentCounts(aProject, weights);
        }
        else {
            var stats = documentService.getSourceDocumentStats(aProject);
            currentCounts = new LinkedHashMap<>();
            currentCounts.put(NEW, stats.getNewAnnotations());
            currentCounts.put(ANNOTATION_IN_PROGRESS, stats.getAnnotationsInProgress());
            currentCounts.put(ANNOTATION_FINISHED, stats.getFinishedAnnotations());
            currentCounts.put(CURATION_IN_PROGRESS, stats.getCurationsInProgress());
            currentCounts.put(CURATION_FINISHED, stats.getCurationsFinished());
        }

        var snapshots = eventRepository.calculateHistoricalDocumentStates(aProject, currentCounts,
                weights, aTo);

        return snapshots.stream() //
                .map(s -> new ProgressDataPoint(s.day(), s.counts())) //
                .toList();
    }

    private Map<Long, Long> resolveTokenWeights(Project aProject) throws IOException
    {
        if (weightSource == null) {
            return emptyMap();
        }
        var weights = weightSource.getWeights(userService.getCurrentUser(), aProject,
                ProgressMetric.TOKENS);
        return weights != null ? weights : emptyMap();
    }

    private Map<SourceDocumentState, Long> weightedCurrentCounts(Project aProject,
            Map<Long, Long> aWeights)
    {
        var stateByDocId = new HashMap<Long, SourceDocumentState>();
        for (var doc : documentService.listSourceDocuments(aProject)) {
            stateByDocId.put(doc.getId(), doc.getState());
        }
        var counts = new LinkedHashMap<SourceDocumentState, Long>();
        for (var state : new SourceDocumentState[] { NEW, ANNOTATION_IN_PROGRESS,
                ANNOTATION_FINISHED, CURATION_IN_PROGRESS, CURATION_FINISHED }) {
            counts.put(state, 0L);
        }
        for (var entry : aWeights.entrySet()) {
            var state = stateByDocId.get(entry.getKey());
            if (state == null) {
                continue;
            }
            counts.merge(state, entry.getValue(), Long::sum);
        }
        return counts;
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
