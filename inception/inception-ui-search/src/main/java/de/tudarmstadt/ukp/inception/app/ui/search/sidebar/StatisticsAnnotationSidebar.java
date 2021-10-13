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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.search.Formats;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.StatisticsOptions;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.Granularities;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.Metrics;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.config.SearchProperties;

public class StatisticsAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
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

    private User currentUser;

    private WebMarkupContainer mainContainer;

    private IModel<Project> projectModel;
    private List<IColumn> columns;
    private DefaultDataTable resultsTable;

    private DropDownChoice<String> granularityChoice;
    private DropDownChoice<String> statisticChoice;
    private DropDownChoice<String> formatChoice;

    private AjaxDownloadLink exportButton;

    private String selectedFormat;
    private String selectedStatistic;
    private String selectedGranularity;
    private String propertyExpressionStatistic;

    private List<LayerStatistics> layerStatsList;
    private List<AnnotationFeature> features;

    private CompoundPropertyModel<StatisticsOptions> statisticsOptions = CompoundPropertyModel
            .of(new StatisticsOptions());

    private StatisticsProvider statsProvider;

    public StatisticsAnnotationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        projectModel = new Model<Project>(aAnnotationPage.getProject());
        currentUser = userRepository.getCurrentUser();

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        selectedFormat = null;
        selectedGranularity = null;
        selectedStatistic = null;
        propertyExpressionStatistic = "";

        layerStatsList = null;
        features = annotationService.listAnnotationFeature((projectModel.getObject()));

        Form<StatisticsOptions> statisticsForm = new Form<>("settings", statisticsOptions);

        granularityChoice = new DropDownChoice<String>(GRANULARITY,
                new PropertyModel<String>(this, "selectedGranularity"), GRANULARITY_LEVELS)
        {
            @Override
            protected String getNullKeyDisplayValue()
            {
                return "Granularity:";
            }
        };

        statisticsForm.add(granularityChoice);

        statisticChoice = new DropDownChoice<String>(STATISTIC,
                new PropertyModel<String>(this, "selectedStatistic"), STATISTICS)
        {
            @Override
            protected String getNullKeyDisplayValue()
            {
                return "Statistic:";
            }
        };
        statisticsForm.add(statisticChoice);

        LambdaAjaxButton<StatisticsOptions> calculateButton = new LambdaAjaxButton<StatisticsOptions>(
                "calculate", this::actionCalculate);
        statisticsForm.add(calculateButton);
        calculateButton.add(new LambdaAjaxFormComponentUpdatingBehavior("calculated"));

        statsProvider = new StatisticsProvider(new ArrayList<>());

        columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model<String>("Features"), "getLayerFeatureName",
                "getLayerFeatureName"));

        resultsTable = new DefaultDataTable("datatable", columns, statsProvider, 30);
        resultsTable.setOutputMarkupId(true);

        statisticsForm.add(resultsTable);

        formatChoice = new DropDownChoice<String>(FORMAT,
                new PropertyModel<String>(this, "selectedFormat"), FORMATS)
        {
            @Override
            protected String getNullKeyDisplayValue()
            {
                return "Format:";
            }
        };
        formatChoice.add(visibleWhen(() -> columns.size() > 1));
        formatChoice.setOutputMarkupPlaceholderTag(true);
        statisticsForm.add(formatChoice);
        formatChoice.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        exportButton = new AjaxDownloadLink("export", () -> "searchResults" + selectedFormat,
                this::actionExport);
        exportButton.add(visibleWhen(() -> columns.size() > 1));
        statisticsForm.add(exportButton);

        exportButton.setOutputMarkupPlaceholderTag(true);
        // exportButton.setOutputMarkupId(true);

        // exportButton.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));

        mainContainer.add(statisticsForm);
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
            if (layerStatsList == null) {
                StatisticsResult result = searchService.getProjectStatistics(currentUser,
                        projectModel.getObject(), OptionalInt.empty(), OptionalInt.empty(),
                        new HashSet<AnnotationFeature>(features));
                layerStatsList = new ArrayList<>(result.getResults().values());
            }
        }
        catch (ExecutionException e) {
            // e.printStackTrace();
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
        columns.add(new PropertyColumn(new Model<String>(selectedStatistic),
                propertyExpressionStatistic, propertyExpressionStatistic));

        statsProvider.setData(layerStatsList);

        aTarget.add(resultsTable);
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