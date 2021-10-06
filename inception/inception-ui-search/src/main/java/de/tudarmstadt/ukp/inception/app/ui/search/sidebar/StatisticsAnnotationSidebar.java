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

import java.io.IOException;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
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
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
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


    // private DropDownChoice<AnnotationLayer> layerChoice;
    // private DropDownChoice<AnnotationFeature> featureChoice;
    private DropDownChoice<String> granularityChoice;
    private DropDownChoice<String> statisticChoice;
    private DropDownChoice<String> formatChoice;

    private String selectedFormat;
    private String selectedStatistic;
    private String selectedGranularity;
    private String propertyExpressionStatistic;

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

        /*
         * layerChoice = new DropDownChoice<>(MID_LAYER, this::listLayers);
         * layerChoice.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
         * layerChoice.setRequired(true); // The features and tools depend on the layer, so reload
         * them when the layer is changed layerChoice.add(new
         * LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
         * featureChoice.setModelObject(null); })); mainContainer.add(layerChoice);
         * 
         */
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

        formatChoice = new DropDownChoice<String>(FORMAT,
                new PropertyModel<String>(this, "selectedFormat"), FORMATS)
        {
            @Override
            protected String getNullKeyDisplayValue()
            {
                return "Format:";
            }
        };
        statisticsForm.add(formatChoice);

        LambdaAjaxButton<StatisticsOptions> calculateButton = new LambdaAjaxButton<StatisticsOptions>(
                "calculate", this::actionCalculate);
        statisticsForm.add(calculateButton);

        LambdaAjaxButton<StatisticsOptions> exportButton = new LambdaAjaxButton<StatisticsOptions>(
                "export", this::actionExport);
        statisticsForm.add(exportButton);

        statsProvider = new StatisticsProvider(new ArrayList<>());

        columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model<String>("Features"), "getLayerFeatureName",
                "getLayerFeatureName"));
        //try {
            /*
            columns.add(new PropertyColumn(new Model<String>(selectedStatistic),
                    StatisticsOptions.uiToInternal(selectedStatistic),
                    StatisticsOptions.uiToInternal(selectedStatistic)));

             */
        /*
            columns.add(new PropertyColumn(new Model<String>(selectedStatistic),
                //propertyExpressionStatistic
                "getMean"
            ));

         */
        //}
        /*
        catch (ExecutionException e) {
            LOG.error("Problem converting UI name of statistic to internal name!");
        }

         */

        resultsTable = new DefaultDataTable("datatable", columns, statsProvider, 30);
        resultsTable.setOutputMarkupId(true);

        statisticsForm.add(resultsTable);
        mainContainer.add(statisticsForm);
    }

    private void actionCalculate(AjaxRequestTarget aTarget, Form<StatisticsOptions> aForm)
        throws ExecutionException
    {
        List<LayerStatistics> layerStatsList = new ArrayList<LayerStatistics>();
        // List<Double> resultsList = new ArrayList<Double>();
        // List<String> featureNameList = new ArrayList<String>();

        List<AnnotationFeature> features = annotationService
                .listAnnotationFeature((projectModel.getObject()));

        if (selectedGranularity == null) {
            LOG.error("Error: No granularity selected!");
            error("Error: No granularity selected!");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        else if (selectedStatistic == null) {
            LOG.error("Error: No statistic selected!");
            error("Error: No statistic selected!");
            aTarget.addChildren(getPage(), IFeedback.class);

        }
        else {
            try {
                StatisticsResult result = searchService.getProjectStatistics(currentUser,
                        projectModel.getObject(), OptionalInt.empty(), OptionalInt.empty(),
                        new HashSet<AnnotationFeature>(features));

                layerStatsList.add(result.getTokenResult());
                layerStatsList.add(result.getSentenceResult());
                for (AnnotationFeature feature : features) {
                    LayerStatistics layerStats = result.getLayerResult(feature.getLayer(), feature);
                    // resultsList.add(
                    // layerStats.getMetric(StatisticsOptions.uiToInternal(selectedStatistic),
                    // selectedGranularity == GRANULARITY_LEVELS.get(1)));
                    // featureNameList.add(feature.getUiName());
                    layerStatsList.add(layerStats);
                }

                propertyExpressionStatistic = StatisticsOptions.buildPropertyExpression(Metrics.uiToInternal(selectedStatistic), Granularities.uiToInternal(selectedGranularity));
            }
            catch (ExecutionException e) {
                e.printStackTrace();
                LOG.error("Error: Something went wrong parsing the input.");
                error("Error: Something went wrong parsing the input.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            catch (IOException e) {
                LOG.error("Error: Something went wrong accessing files.");
                error("Error: Something went wrong accessing files.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
        if (columns.size() > 1) {
            columns.remove(1);
        }
        columns.add(new PropertyColumn(new Model<String>(selectedStatistic),
            propertyExpressionStatistic, propertyExpressionStatistic));

        statsProvider.setData(layerStatsList);

        // resultsTable = new DefaultDataTable("datatable", columns, statsProvider, 20);
        // resultsTable.setOutputMarkupId(true);
        // aTarget.add(resultsTable);

        // resultsTable.replaceWith(new DefaultDataTable("datatable", columns, statsProvider,
        // 20).setOutputMarkupId(true));
        aTarget.add(resultsTable);
    }

    private void actionExport(AjaxRequestTarget aTarget, Form<StatisticsOptions> aForm)
    {
        // selectedResult = null;
        // searchResultGroups.setItemsPerPage(searchOptions.getObject().getItemsPerPage());
        // executeSearchResultsGroupedQuery(aTarget);
        aTarget.add(mainContainer);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    /*
     * private List<AnnotationLayer> listLayers() { List<AnnotationLayer> layers = new
     * ArrayList<>();
     * 
     * for (AnnotationLayer layer : annotationService
     * .listAnnotationLayer(projectModel.getObject())) { if ((SPAN_TYPE.equals(layer.getType()) ||
     * RELATION_TYPE.equals(layer.getType())) && !Token.class.getName().equals(layer.getName())) {
     * layers.add(layer); } }
     * 
     * return layers; }
     * 
     */

    /*
     * private List<AnnotationFeature> listFeatures() { List<AnnotationFeature> features = new
     * ArrayList<AnnotationFeature>(); if (selectedLayer != null) { for (AnnotationFeature feature :
     * annotationService.listAnnotationFeature(selectedLayer)) { features.add(feature); } } else {
     * return Collections.emptyList(); } return features; }
     * 
     */

}
