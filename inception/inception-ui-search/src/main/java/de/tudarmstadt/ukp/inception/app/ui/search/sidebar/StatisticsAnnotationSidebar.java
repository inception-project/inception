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
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.StatisticsOptions;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.config.SearchProperties;

public class StatisticsAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsAnnotationSidebar.class);

    // private static final String MID_LAYER = "layer";
    private static final String GRANULARITY = "Granularity: ";
    private static final List<String> GRANULARITY_LEVELS = StatisticsOptions.GRANULARITY_LEVELS;

    private static final String STATISTIC = "Choose statistic: ";
    private static final List<String> STATISTICS = StatisticsOptions.STATISTICS;

    private static final String FORMAT = "Format: ";
    private static final List<String> FORMATS = StatisticsOptions.FORMATS;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean SearchProperties searchProperties;

    private User currentUser;

    private final WebMarkupContainer mainContainer;

    private IModel<Project> projectModel;

    private IModel<String> targetQuery = Model.of("");

    // private DropDownChoice<AnnotationLayer> layerChoice;
    private DropDownChoice<String> granularityChoice;
    // private DropDownChoice<AnnotationFeature> featureChoice;
    private DropDownChoice<String> statisticChoice;
    private DropDownChoice<String> formatChoice;

    private String selectedFormat;
    private String selectedStatistic;
    private String selectedGranularity;

    private CompoundPropertyModel<StatisticsOptions> statisticsOptions = CompoundPropertyModel
            .of(new StatisticsOptions());

    private DropDownChoice<AnnotationFeature> groupingFeature;
    private CheckBox lowLevelPagingCheckBox;

    private DataTable<Number, String> resultsTable;

    private SearchResult selectedResult;

    // UI elements for annotation changes
    /*
     * private final Form<CreateAnnotationsOptions> annotationOptionsForm; private final
     * LambdaAjaxLink createOptionsLink; private final LambdaAjaxButton<Void> deleteButton;
     * 
     * private final Form<DeleteAnnotationsOptions> deleteOptionsForm; private final
     * LambdaAjaxButton<Void> annotateButton; private final LambdaAjaxLink deleteOptionsLink;
     * private final Form<Void> annotationForm;
     * 
     */

    public StatisticsAnnotationSidebar(String aId, IModel<AnnotatorState> aModel, IModel<Project> aProject,
                                       AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
                                       AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        projectModel = aProject;
        currentUser = userRepository.getCurrentUser();

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        selectedFormat = null;
        selectedGranularity = null;
        selectedStatistic = null;

        resultsTable = new DefaultDataTable<>();

/*
        layerChoice = new DropDownChoice<>(MID_LAYER, this::listLayers);
        layerChoice.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layerChoice.setRequired(true);
        // The features and tools depend on the layer, so reload them when the layer is changed
        layerChoice.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            featureChoice.setModelObject(null);
        }));
        mainContainer.add(layerChoice);

 */
        Form<StatisticsOptions> statisticsForm = new Form<>("statistics settings", statisticsOptions);

        granularityChoice = new DropDownChoice<String>(GRANULARITY, GRANULARITY_LEVELS);
        granularityChoice.setChoiceRenderer()
        granularityChoice.setRequired(true);
        statisticsForm.add(granularityChoice);

        statisticChoice = new DropDownChoice<String>(STATISTIC, STATISTICS);
        statisticChoice.setRequired(true);
        statisticsForm.add(statisticChoice);

        formatChoice = new DropDownChoice<String>(FORMAT, FORMATS);
        formatChoice.setRequired(true);
        statisticsForm.add(formatChoice);


        //statisticsForm.add(new TextArea<>("queryInput", targetQuery));
        LambdaAjaxButton<StatisticsOptions> calculateButton = new LambdaAjaxButton<StatisticsOptions>("calculate",
            this::actionCalculate);
        statisticsForm.add(calculateButton);

        LambdaAjaxButton<StatisticsOptions> exportButton = new LambdaAjaxButton<StatisticsOptions>("export",
            this::actionExport);
        statisticsForm.add(exportButton);


        //searchForm.setDefaultButton(searchButton);

        mainContainer.add(statisticsForm);

    }

    private void actionCalculate(AjaxRequestTarget aTarget, Form<StatisticsOptions> aForm)
    {
        List<AnnotationFeature> features = annotationService
                .listAnnotationFeature((projectModel.getObject()));

        String currentGranularity = granularityChoice.getChoicesModel().getObject().get(0);
        String currentStatistic = statisticChoice.getChoicesModel().getObject().get(0);
        if (currentGranularity == null) {
            LOG.error("No granularity selected!");
        }
        else if (currentStatistic == null) {
            LOG.error("No statistic selected!");
        }
        else {
            try {
                StatisticsResult result = searchService.getProjectStatistics(currentUser,
                        projectModel.getObject(), OptionalInt.empty(), OptionalInt.empty(),
                        new HashSet<AnnotationFeature>(features));

                for (AnnotationFeature feature : features) {
                    LayerStatistics layerStats = result.getLayerResult(feature.getLayer(), feature);
                    switch (currentStatistic) {
                    case "sum":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getTotal();
                        } else {
                            Number value = layerStats.getTotal();
                        }
                        break;
                    case "max":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getMaximum();
                        } else {
                            Number value = layerStats.getMaximumPerSentence();
                        }
                        break;
                    case "min":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getMinimum();
                        } else {
                            Number value = layerStats.getMinimumPerSentence();
                        }
                        break;
                    case "mean":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getMean();
                        } else {
                            Number value = layerStats.getMeanPerSentence();
                        }
                        break;
                    case "median":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getMedian();
                        } else {
                            Number value = layerStats.getMedianPerSentence();
                        }
                        break;
                    case "standarddeviation":
                        if (currentGranularity == GRANULARITY_LEVELS.get(0)) {
                            Number value = layerStats.getStandardDeviation();
                        } else {
                            Number value = layerStats.getStandardDeviationPerSentence();
                        }
                        break;
                    default:
                        Number value = null;
                        break;
                    }

                    List<IColumn<Number, String>> columns = new ArrayList<>();
                    columns.add(new LambdaColumn(new Model<String>(feature.getUiName()), Contact::getFirstName));


                }

            }
            catch (ExecutionException e) {
                LOG.error("Error: Something went wrong parsing the input.");
            }
            catch (IOException e) {
                LOG.error("Error: Something went wrong accessing files.");
            }
        }

    }

    private void actionExport(AjaxRequestTarget aTarget, Form<StatisticsOptions> aForm)
    {
        selectedResult = null;
        searchResultGroups.setItemsPerPage(searchOptions.getObject().getItemsPerPage());
        executeSearchResultsGroupedQuery(aTarget);
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
