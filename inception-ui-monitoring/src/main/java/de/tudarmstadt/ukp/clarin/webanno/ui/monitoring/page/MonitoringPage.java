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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.IGNORE_TO_NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static java.awt.Color.BLUE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.jfree.chart.plot.PlotOrientation.HORIZONTAL;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.jfreechart.SvgChart;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.AnnotatorColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixDataProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixRow;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.SourceDocumentNameColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.SourceDocumentStateColumn;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/monitoring")
public class MonitoringPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean CurationDocumentService curationService;

    private SvgChart annotatorsProgressImage;
    private SvgChart annotatorsProgressPercentageImage;
    private DataTable<DocumentMatrixRow, Void> documentMatrix;

    public MonitoringPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        User user = userRepository.getCurrentUser();

        Project project = getProject();

        requireProjectRole(user, CURATOR, MANAGER);

        add(new Label("name", project.getName()));

        annotatorsProgressImage = new SvgChart("annotator",
                LoadableDetachableModel.of(this::renderAnnotatorAbsoluteProgress));
        annotatorsProgressImage.setOutputMarkupId(true);
        add(annotatorsProgressImage);

        annotatorsProgressPercentageImage = new SvgChart("annotatorPercentage",
                LoadableDetachableModel.of(this::renderAnnotatorPercentageProgress));
        annotatorsProgressPercentageImage.setOutputMarkupId(true);
        add(annotatorsProgressPercentageImage);

        add(documentMatrix = createDocumentMatrix("documentMatrix"));
    }

    private DataTable<DocumentMatrixRow, Void> createDocumentMatrix(String aComponentId)
    {
        DocumentMatrixDataProvider dataProvider = new DocumentMatrixDataProvider(getMatrixData());

        List<IColumn<DocumentMatrixRow, Void>> columns = new ArrayList<>();
        columns.add(new SourceDocumentStateColumn());
        columns.add(new SourceDocumentNameColumn());
        // columns.add(new AnnotatorColumn(CURATION_USER));
        for (User annotator : projectService.listProjectUsersWithPermissions(getProject(),
                ANNOTATOR)) {
            columns.add(new AnnotatorColumn(annotator.getUsername())
            {
                private static final long serialVersionUID = -5059896378893914756L;

                @Override
                public void actionStateChange(AjaxRequestTarget aTarget,
                        SourceDocument aSourceDocument, String aUsername)
                {
                    MonitoringPage.this.actionStateChange(aTarget, aSourceDocument, aUsername);
                }
            });
        }

        DataTable<DocumentMatrixRow, Void> table = new DefaultDataTable<>(aComponentId, columns,
                dataProvider, 50);
        table.setOutputMarkupId(true);
        return table;
    }

    private List<DocumentMatrixRow> getMatrixData()
    {
        Map<SourceDocument, DocumentMatrixRow> documentMatrixRows = new LinkedHashMap<>();
        for (SourceDocument srcDoc : documentService.listSourceDocuments(getProject())) {
            documentMatrixRows.put(srcDoc, new DocumentMatrixRow(srcDoc));
        }

        for (AnnotationDocument annDoc : documentService.listAnnotationDocuments(getProject())) {
            documentMatrixRows.get(annDoc.getDocument()).add(annDoc);
        }

        return new ArrayList<>(documentMatrixRows.values());
    }

    private void actionStateChange(AjaxRequestTarget aTarget, SourceDocument aSourceDocument,
            String aUsername)
    {
        User user = userRepository.get(aUsername);
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aSourceDocument, user);

        AnnotationDocumentStateTransition transition;
        switch (annotationDocument.getState()) {
        case NEW:
            transition = NEW_TO_IGNORE;
            break;
        case IGNORE:
            transition = IGNORE_TO_NEW;
            break;
        case IN_PROGRESS:
            transition = ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
            break;
        case FINISHED:
            transition = ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
            break;
        default:
            return;
        }

        documentService.transitionAnnotationDocumentState(annotationDocument, transition);

        ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .setMatrixData(getMatrixData());
        ;

        aTarget.add(documentMatrix);
    }

    private JFreeChart renderAnnotatorAbsoluteProgress()
    {
        int totalDocuments = documentService.numberOfExpectedAnnotationDocuments(getProject());
        Map<String, Integer> data = getFinishedDocumentsPerUser(getProject());
        annotatorsProgressImage.getOptions().withViewBox(300, 30 + (data.size() * 18));
        return createProgressChart(data, totalDocuments, false);
    }

    private JFreeChart renderAnnotatorPercentageProgress()
    {
        Map<String, Integer> data = getPercentageOfFinishedDocumentsPerUser(getProject());
        annotatorsProgressPercentageImage.getOptions().withViewBox(300, 30 + (data.size() * 18));
        return createProgressChart(data, 100, true);
    }

    private Map<String, Integer> getFinishedDocumentsPerUser(Project aProject)
    {
        if (aProject == null) {
            return emptyMap();
        }

        Map<String, List<AnnotationDocument>> docsPerUser = documentService
                .listFinishedAnnotationDocuments(aProject).stream()
                .collect(groupingBy(AnnotationDocument::getUser));

        // We explicitly use HashMap::new below since we *really* want a mutable map and
        // Collectors.toMap(...) doesn't make guarantees about the mutability of the map type it
        // internally creates.
        Map<String, Integer> finishedDocumentsPerUser = docsPerUser.entrySet().stream().collect(
                toMap(Entry::getKey, e -> e.getValue().size(), throwingMerger(), HashMap::new));

        // Make sure we also have all annotators in the map who have not actually annotated
        // anything
        projectService.listProjectUsersWithPermissions(aProject, ANNOTATOR).stream()
                .map(User::getUsername)
                .forEach(user -> finishedDocumentsPerUser.computeIfAbsent(user, _it -> 0));

        // Add the finished documents for the curation user
        List<SourceDocument> curatedDocuments = curationService.listCuratedDocuments(aProject);

        // Little hack: to ensure that the curation user comes first on screen, add a space
        finishedDocumentsPerUser.put(CURATION_USER, curatedDocuments.size());

        return finishedDocumentsPerUser;
    }

    private Map<String, Integer> getPercentageOfFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> finishedDocumentsPerUser = getFinishedDocumentsPerUser(aProject);

        Map<String, Integer> percentageFinishedPerUser = new HashMap<>();
        List<User> annotators = new ArrayList<>(
                projectService.listProjectUsersWithPermissions(aProject, ANNOTATOR));

        // Little hack: to ensure that the curation user comes first on screen, add a space
        annotators.add(new User(CURATION_USER));

        for (User annotator : annotators) {
            Map<SourceDocument, AnnotationDocument> docsForUser = documentService
                    .listAnnotatableDocuments(aProject, annotator);

            int finished = finishedDocumentsPerUser.get(annotator.getUsername());
            int annotatableDocs = docsForUser.size();
            percentageFinishedPerUser.put(annotator.getUsername(),
                    (int) Math.round((double) (finished * 100) / annotatableDocs));
        }

        return percentageFinishedPerUser;
    }

    private JFreeChart createProgressChart(Map<String, Integer> chartValues, int aMaxValue,
            boolean aIsPercentage)
    {
        // fill dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        if (aMaxValue > 0) {
            for (String chartValue : chartValues.keySet()) {
                dataset.setValue(chartValues.get(chartValue), "Completion", chartValue);
            }
        }

        // create chart
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset, HORIZONTAL, false,
                false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(null);
        plot.setNoDataMessage("No data");
        plot.setInsets(new RectangleInsets(0, 20, 0, 20));
        if (aMaxValue > 0) {
            plot.getRangeAxis().setRange(0.0, aMaxValue);
            ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0"));
            // For documents less than 10, avoid repeating the number of documents such
            // as 0 0 1 1 1 - NumberTickUnit automatically determines the range
            if (!aIsPercentage && aMaxValue <= 10) {
                TickUnits standardUnits = new TickUnits();
                NumberAxis tick = new NumberAxis();
                tick.setTickUnit(new NumberTickUnit(1));
                standardUnits.add(tick.getTickUnit());
                plot.getRangeAxis().setStandardTickUnits(standardUnits);
            }
        }

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, BLUE);
        plot.setRenderer(renderer);

        return chart;
    }

    private static <T> BinaryOperator<T> throwingMerger()
    {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }
}
