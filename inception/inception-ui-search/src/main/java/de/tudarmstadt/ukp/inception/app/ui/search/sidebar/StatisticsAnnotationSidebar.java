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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.search.Formats;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.StatisticsOptions;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.Granularities;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.Metrics;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.config.SearchProperties;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;

public class StatisticsAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 2796916194245461498L;

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsAnnotationSidebar.class);

    private static final String GRANULARITY = "granularity";
    private static final List<String> GRANULARITY_LEVELS = Granularities.uiList();

    private static final String STATISTIC = "statistic";
    private static final List<String> STATISTICS = Metrics.uiList();

    private static final String FORMAT = "format";
    private static final List<String> FORMATS = Formats.uiList();

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean SearchProperties searchProperties;
    private @SpringBean UserPreferencesService userPreferencesService;

    private User currentUser;

    private WebMarkupContainer mainContainer;

    private IModel<Project> projectModel;
    private List<IColumn> columns;
    private DefaultDataTable resultsTable;

    private DropDownChoice<String> granularityChoice;
    private DropDownChoice<String> statisticChoice;
    private DropDownChoice<String> formatChoice;
    private Label formatLabel;
    private AjaxDownloadLink exportButton;
    private CheckBox nonZeroCheckBox;

    private boolean hideNull;
    Map<String, LayerStatistics> withoutProblematicStats;

    private String selectedFormat;
    private String selectedStatistic;
    private String selectedGranularity;
    private String propertyExpressionStatistic;

    private List<LayerStatistics> layerStatsList;
    private List<AnnotationFeature> features;
    Set<Long> hiddenLayerIds;

    private CompoundPropertyModel<StatisticsOptions> statisticsOptions = CompoundPropertyModel
            .of(new StatisticsOptions());

    private StatisticsProvider statsProvider;

    public StatisticsAnnotationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        projectModel = new Model<Project>(aAnnotationPage.getProject());
        currentUser = userRepository.getCurrentUser();

        add(new DocLink("statisticsHelpLink", "sect_statistics"));

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        selectedFormat = Formats.internalToUi(Formats.TXT);
        selectedGranularity = Granularities.internalToUi(Granularities.PER_DOCUMENT);
        selectedStatistic = Metrics.internalToUi(Metrics.MEAN);
        propertyExpressionStatistic = "";

        hideNull = false;
        withoutProblematicStats = null;

        layerStatsList = null;

        /*
         * //userPreferencesService.loadPreferences(aModel.getObject(), currentUser.getUsername())
         * //layers = aModel.getObject().getAnnotationLayers(); //layers =
         * aModel.getObject().getSelectableLayers(); hiddenLayerIds =
         * aModel.getObject().getPreferences().getHiddenAnnotationLayerIds();
         * //userPreferencesService.loadPreferences(projectModel,currentUser.getUsername(),)
         * features = new ArrayList<AnnotationFeature>(); for (AnnotationFeature feature:
         * annotationService.listAnnotationFeature(projectModel.getObject())) { if
         * (!hiddenLayerIds.contains(feature.getLayer().getId())) { features.add(feature); } }
         */

        features = annotationService.listSupportedFeatures(projectModel.getObject());

        Form<StatisticsOptions> statisticsForm = new Form<>("settings", statisticsOptions);

        granularityChoice = new DropDownChoice<String>(GRANULARITY,
                new PropertyModel<String>(this, "selectedGranularity"), GRANULARITY_LEVELS);

        statisticsForm.add(granularityChoice);

        statisticChoice = new DropDownChoice<String>(STATISTIC,
                new PropertyModel<String>(this, "selectedStatistic"), STATISTICS);
        statisticsForm.add(statisticChoice);

        nonZeroCheckBox = new CheckBox("nonZero", new PropertyModel<Boolean>(this, "hideNull"));
        nonZeroCheckBox.setOutputMarkupId(true);
        statisticsForm.add(nonZeroCheckBox);

        LambdaAjaxButton<StatisticsOptions> calculateButton = new LambdaAjaxButton<StatisticsOptions>(
                "calculate", this::actionCalculate);
        statisticsForm.add(calculateButton);
        calculateButton.add(new LambdaAjaxFormComponentUpdatingBehavior("calculated"));

        statsProvider = new StatisticsProvider(new ArrayList<>());

        columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn<>(new Model<String>("Features"), "getLayerFeatureName",
                "getLayerFeatureName"));

        resultsTable = new DefaultDataTable("datatable", columns, statsProvider, 20);
        resultsTable.setOutputMarkupId(true);
        resultsTable.setOutputMarkupPlaceholderTag(true);
        resultsTable.add(visibleWhen(() -> columns.size() > 1));
        statisticsForm.add(resultsTable);

        mainContainer.add(statisticsForm);
        // add(statisticsForm);

        formatLabel = new Label("formatLabel", "Format");
        formatLabel.add(visibleWhen(() -> columns.size() > 1));
        formatLabel.setOutputMarkupPlaceholderTag(true);

        add(formatLabel);

        formatChoice = new DropDownChoice<String>(FORMAT,
                new PropertyModel<String>(this, "selectedFormat"), FORMATS);
        formatChoice.add(visibleWhen(() -> columns.size() > 1));
        formatChoice.setOutputMarkupPlaceholderTag(true);
        add(formatChoice);
        formatChoice.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        exportButton = new AjaxDownloadLink("export", () -> "searchResults" + selectedFormat,
                this::actionExport);
        exportButton.add(visibleWhen(() -> columns.size() > 1));
        add(exportButton);

        exportButton.setOutputMarkupPlaceholderTag(true);

    }

    private void actionCalculate(AjaxRequestTarget aTarget, Form<StatisticsOptions> aForm)
        throws ExecutionException
    {
        if (selectedGranularity == null) {
            LOG.error("Error: No granularity selected!");
            error("Error: No granularity selected!");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        if (selectedStatistic == null) {
            LOG.error("Error: No statistic selected!");
            error("Error: No statistic selected!");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            withoutProblematicStats = hideNull ? searchService
                    .getProjectStatistics(currentUser, projectModel.getObject(), Integer.MIN_VALUE,
                            Integer.MAX_VALUE, new HashSet<AnnotationFeature>(features))
                    .getNonZeroResults()
                    : searchService.getProjectStatistics(currentUser, projectModel.getObject(),
                            Integer.MIN_VALUE, Integer.MAX_VALUE,
                            new HashSet<AnnotationFeature>(features)).getResults();

        }
        catch (ExecutionException e) {
            LOG.error("Error: " + e.getMessage()
                    + ". This can be caused by an invalid feature name.");
            error("Error: " + e.getMessage() + ". This can be caused by an invalid feature name.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        catch (IOException e) {
            LOG.error("Error: Something went wrong accessing files.");
            error("Error: Something went wrong accessing files.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (Granularities.uiToInternal(selectedGranularity) == Granularities.PER_SENTENCE) {
            withoutProblematicStats.remove("Segmentation.sentence");
        }
        layerStatsList = new ArrayList<LayerStatistics>(withoutProblematicStats.values());

        try {
            propertyExpressionStatistic = StatisticsOptions.buildPropertyExpression(
                    Metrics.uiToInternal(selectedStatistic),
                    Granularities.uiToInternal(selectedGranularity));
        }
        catch (ExecutionException e) {
            LOG.error("Error: The selected statistic or the selected granularity is not valid.");
            error("Error: The selected statistic or the selected granularity is not valid.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (columns.size() > 1) {
            columns.remove(1);
        }
        columns.add(new PropertyColumn<>(new Model<String>(selectedStatistic),
                propertyExpressionStatistic, propertyExpressionStatistic));

        statsProvider.setData(layerStatsList);

        aTarget.add(resultsTable);
        aTarget.add(formatLabel);
        aTarget.add(formatChoice);
        aTarget.add(exportButton);
    }

    private IResourceStream actionExport()
    {
        if (selectedFormat == null) {
            LOG.error("Error: No format selected!");
            error("Error: No format selected!");
            return null;
        }
        return new AbstractResourceStream()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException
            {
                StatisticsExporter exporter = new StatisticsExporter();
                try {
                    return exporter.generateFile(statsProvider.getData(),
                            Formats.uiToInternal(selectedFormat));
                }
                catch (Exception e) {
                    LOG.error("Unable to generate statistics file", e);
                    error("Error: " + e.getMessage());
                    throw new ResourceStreamNotFoundException(e);
                }
            }

            @Override
            public void close() throws IOException
            {
                // Nothing to do
            }
        };
    }
}
