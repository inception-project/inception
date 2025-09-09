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
package de.tudarmstadt.ukp.inception.pivot.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.users.ProjectUserPermissionChoiceRenderer;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.Aggregator;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupport;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTable;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/explorer")
public class PivotTablePage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -1260889764390320404L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean LayerExtractorSupportRegistry layerExtractorSupportRegistry;
    private @SpringBean FeatureExtractorSupportRegistry featureExtractorSupportRegistry;
    private @SpringBean AggregatorSupportRegistry aggregatorSupportRegistry;
    private @SpringBean SchedulingService schedulingService;

    private Form<State> sidebar = new Form<>("sidebar");
    private DropDownChoice<AnnotationLayer> layerSelector;
    private DropDownChoice<ExtractorDecl> extractorSelector;
    private DropDownChoice<AggregatorDecl> aggregatorSelector;
    private Component pivotTable;
    private ListView<ExtractorDecl> rowExtractors;
    private ListView<ExtractorDecl> colExtractors;
    private LambdaAjaxButton<State> addCell;

    private WebMarkupContainer cellExtractorsContainer;
    private ListView<ExtractorDecl> cellExtractors;

    public PivotTablePage(PageParameters aParameters)
    {
        super(aParameters);

        var sessionOwner = userService.getCurrentUser();

        requireProjectRole(sessionOwner, MANAGER, CURATOR);

        pivotTable = buildExampleTable();
        queue(pivotTable);

        sidebar = new Form<>("sidebar", CompoundPropertyModel.of(new State()));
        sidebar.setOutputMarkupId(true);
        queue(sidebar);

        layerSelector = new DropDownChoice<AnnotationLayer>("layer");
        layerSelector.setOutputMarkupId(true);
        layerSelector.setNullValid(true);
        layerSelector.setChoices(new ListModel<>(schemaService.listEnabledLayers(getProject())));
        layerSelector.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationLayer::getUiName));
        layerSelector.add(
                new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, this::actionSelectLayer));
        queue(layerSelector);
        

        extractorSelector = new DropDownChoice<ExtractorDecl>("extractor");
        extractorSelector.setOutputMarkupId(true);
        extractorSelector.setChoices(LoadableDetachableModel.of(this::listExtractors));
        extractorSelector.setChoiceRenderer(new LambdaChoiceRenderer<>(ExtractorDecl::name));
        extractorSelector.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionSelectExtractor));
        queue(extractorSelector);
        
        aggregatorSelector = new DropDownChoice<AggregatorDecl>("aggregator");
        aggregatorSelector.setOutputMarkupId(true);
        aggregatorSelector.setChoices(LoadableDetachableModel.of(this::listAggregators));
        aggregatorSelector.setChoiceRenderer(new LambdaChoiceRenderer<>(AggregatorDecl::name));
        aggregatorSelector.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionSelectAggregator));
        queue(aggregatorSelector);

        if (!layerSelector.getChoices().isEmpty()) {
            layerSelector.setModelObject(layerSelector.getChoices().get(0));
        }

        if (!extractorSelector.getChoices().isEmpty()) {
            extractorSelector.setModelObject(extractorSelector.getChoices().get(0));
        }

        if (!aggregatorSelector.getChoices().isEmpty()) {
            aggregatorSelector.setModelObject(aggregatorSelector.getChoices().get(0));
        }

        rowExtractors = new ListView<ExtractorDecl>("rowExtractors")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ExtractorDecl> aItem)
            {
                aItem.add(new Label("name", aItem.getModel().map(ExtractorDecl::name)));
                aItem.add(new LambdaAjaxLink("remove", t -> actionRemoveExtractor(t, aItem, this)));
            }
        };
        queue(rowExtractors);

        var noRowExtractors = new WebMarkupContainer("noRowExtractors");
        noRowExtractors.add(visibleWhen(rowExtractors.getModel().map(List::isEmpty)));
        queue(noRowExtractors);

        colExtractors = new ListView<ExtractorDecl>("colExtractors")
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ExtractorDecl> aItem)
            {
                aItem.add(new Label("name", aItem.getModel().map(ExtractorDecl::name)));
                aItem.add(new LambdaAjaxLink("remove", t -> actionRemoveExtractor(t, aItem, this)));
            }
        };
        queue(colExtractors);

        var noColExtractors = new WebMarkupContainer("noColExtractors");
        noColExtractors.add(visibleWhen(colExtractors.getModel().map(List::isEmpty)));
        queue(noColExtractors);

        cellExtractorsContainer = new WebMarkupContainer("cellExtractorsContainer");
        cellExtractorsContainer.setOutputMarkupPlaceholderTag(true);
        cellExtractorsContainer
                .add(visibleWhen(aggregatorSelector.getModel().map(AggregatorDecl::supportsCells)));
        queue(cellExtractorsContainer);

        cellExtractors = new ListView<ExtractorDecl>("cellExtractors")
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ExtractorDecl> aItem)
            {
                aItem.add(new Label("name", aItem.getModel().map(ExtractorDecl::name)));
                aItem.add(new LambdaAjaxLink("remove", t -> actionRemoveExtractor(t, aItem, this)));
            }
        };
        queue(cellExtractors);

        var noCellExtractors = new WebMarkupContainer("noCellExtractors");
        noCellExtractors.add(visibleWhen(cellExtractors.getModel().map(List::isEmpty)));
        queue(noCellExtractors);

        queue(new LambdaAjaxButton<State>("switchRowsCols", this::actionSwitchRowsCols));
        queue(new LambdaAjaxButton<State>("switchColsCells", this::actionSwitchColsCells));
        queue(new LambdaAjaxButton<State>("addRow",
                (t, f) -> actionAddExtractor(t, f.getModelObject(), rowExtractors)));
        queue(new LambdaAjaxButton<State>("addCol",
                (t, f) -> actionAddExtractor(t, f.getModelObject(), colExtractors)));
        addCell = new LambdaAjaxButton<State>("addCell",
                (t, f) -> actionAddExtractor(t, f.getModelObject(), cellExtractors));
        addCell.setOutputMarkupPlaceholderTag(true);
        addCell.add(visibleWhen(aggregatorSelector.getModel().map(AggregatorDecl::supportsCells)));
        queue(addCell);
        queue(new LambdaAjaxButton<State>("run", this::actionRun));

        var annotatorList = new ListMultipleChoice<ProjectUserPermissions>("annotators");
        annotatorList.setChoiceRenderer(new ProjectUserPermissionChoiceRenderer());
        annotatorList.setChoices(listDataOwners());
        queue(annotatorList);

        var documentList = new ListMultipleChoice<SourceDocument>("documents");
        documentList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        documentList.setChoices(listDocuments());
        queue(documentList);
    }

    private List<ProjectUserPermissions> listDataOwners()
    {
        var dataOwners = new ArrayList<ProjectUserPermissions>();

        projectService.listProjectUserPermissions(getProject()).stream() //
                .filter(p -> p.getRoles().contains(ANNOTATOR)) //
                .sorted(comparing(p -> p.getUser().map(User::getUiName).orElse(p.getUsername()))) //
                .forEach(dataOwners::add);

        var curationUser = userService.getCurationUser();
        dataOwners.add(new ProjectUserPermissions(getProject(), curationUser.getUsername(),
                curationUser, emptySet()));

        return dataOwners;
    }

    private List<SourceDocument> listDocuments()
    {
        return documentService.listSourceDocuments(getProject());
    }

    private Component buildExampleTable()
    {
        // Example: create a minimal PivotTable with dummy data
        // You should replace this with real extractors and data for your use case
        // var rowExtractor1 = new LambdaExtractor<>("row1", String.class, o -> "A");
        // var rowExtractor2 = new LambdaExtractor<>("row2", String.class, o -> "B");
        // var colExtractor1 = new LambdaExtractor<>("col1", String.class, o -> "A");
        // var colExtractor2 = new LambdaExtractor<>("col2", String.class, o -> "B");
        // var aggregator = new CountAggregator();
        // var dataProvider = PivotTableDataProvider.builder(List.of(rowExtractor1, rowExtractor2),
        // List.of(colExtractor1, colExtractor2), aggregator);
        //
        // dataProvider.add("one");
        // dataProvider.add("two");
        // dataProvider.add("three");

        // return new PivotTable<>("pivotTable", PivotTableDataProvider.EMPTY);
        return new EmptyPanel("pivotTable").setOutputMarkupPlaceholderTag(true);
    }

    private void actionSwitchRowsCols(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        var state = aForm.getModelObject();
        var cols = new ArrayList<>(state.colExtractors);
        var rows = new ArrayList<>(state.rowExtractors);

        state.colExtractors.clear();
        state.colExtractors.addAll(rows);

        state.rowExtractors.clear();
        state.rowExtractors.addAll(cols);

        aTarget.add(sidebar);
    }

    private void actionSwitchColsCells(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        var state = aForm.getModelObject();
        var cols = new ArrayList<>(state.colExtractors);
        var cells = new ArrayList<>(state.cellExtractors);

        state.colExtractors.clear();
        state.colExtractors.addAll(cells);

        state.cellExtractors.clear();
        state.cellExtractors.addAll(cols);

        aTarget.add(sidebar);
    }

    private void actionRemoveExtractor(AjaxRequestTarget aTarget, ListItem<ExtractorDecl> aItem,
            ListView<ExtractorDecl> aListView)
    {
        aListView.getModelObject().remove(aItem.getModelObject());

        aTarget.add(sidebar);
    }

    @SuppressWarnings("rawtypes")
    private Extractor createExtractor(ExtractorDecl aDecl)
    {
        if (aDecl.delegate instanceof AnnotationFeature feature) {
            var ext = featureExtractorSupportRegistry.getExtension(aDecl.id).get();
            return ext.createExtractor(feature);
        }

        var ext = layerExtractorSupportRegistry.getExtension(aDecl.id).get();
        return ext.createExtractor((AnnotationLayer) aDecl.delegate);
    }

    @SuppressWarnings("unchecked")
    private void actionRun(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        var state = aForm.getModelObject();

        if (state.rowExtractors.isEmpty() && state.colExtractors.isEmpty()) {
            info("At least one row or column extractor is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (state.aggregator.supportsCells && state.cellExtractors.isEmpty()) {
            info("At least one cell extractor is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var rows = state.rowExtractors.stream().map(this::createExtractor).toList();
        var cols = state.colExtractors.stream().map(this::createExtractor).toList();
        var cells = state.aggregator.supportsCells
                ? state.cellExtractors.stream().map(this::createExtractor).toList()
                : emptyList();
        var agg = getAggregatorSupport(state.aggregator);

        var dataOwners = (state.annotators.isEmpty() ? listDataOwners() : state.annotators).stream() //
                .map(ProjectUserPermissions::getUser) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .map(User::getUsername).toList();

        var sessionOwner = userService.getCurrentUser();
        var states = state.states.isEmpty() ? asList(IN_PROGRESS, FINISHED, IGNORE) : state.states;
        var allAnnDocs = documentService.listAnnotationDocumentsInState(getProject(), //
                states.toArray(AnnotationDocumentState[]::new)).stream() //
                .collect(groupingBy(AnnotationDocument::getDocument));

        if (isNotEmpty(state.documents)) {
            allAnnDocs.keySet().retainAll(state.documents);
            for (var doc : state.documents) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }
        else {
            for (var doc : documentService.listSourceDocuments(getProject())) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }

        @SuppressWarnings("rawtypes")
        var task = CollectTableDataTask
                .builder((List) rows, (List) cols, (List) cells,
                        (Aggregator) agg.createAggregator()) //
                .withProject(getProject()) //
                .withTrigger("Explicit user action") //
                .withSessionOwner(sessionOwner) //
                .withDocuments(allAnnDocs) //
                .withDataOwners(dataOwners) //
                .build();

        schedulingService.executeSync(task);

        pivotTable = (PivotTable) pivotTable.replaceWith(
                new PivotTable<>("pivotTable", task.getResult(), agg.createCellRenderer()));
        aTarget.add(pivotTable);
    }

    private AggregatorSupport getAggregatorSupport(AggregatorDecl aAggregator)
    {
        return aggregatorSupportRegistry.getExtension(aAggregator.id) //
                .orElseThrow(() -> new IllegalStateException(
                        "No such aggregator: [" + aAggregator.id + "]"));
    }

    private void actionAddExtractor(AjaxRequestTarget aTarget, State state,
            ListView<ExtractorDecl> aExtractorList)
    {
        if (state.extractor == null) {
            info("Please select an extractor first");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var list = aExtractorList.getModelObject();
        if (list.contains(state.extractor)) {
            info("Extractor has already been added");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        list.add(state.extractor);

        aTarget.add(sidebar);
    }

    private List<ExtractorDecl> listExtractors()
    {
        var selectedLayer = layerSelector.getModelObject();

        var extractors = new ArrayList<ExtractorDecl>();

        layerExtractorSupportRegistry.getExtensions(selectedLayer).stream() //
                .map(ext -> new ExtractorDecl(ext.getId(), ext.renderName(selectedLayer),
                        selectedLayer)) //
                .forEach(extractors::add);

        if (selectedLayer != null) {
            var adapter = schemaService.getAdapter(selectedLayer);

            for (var feature : adapter.listFeatures()) {
                featureExtractorSupportRegistry.getExtensions(feature).stream() //
                        .map(ext -> new ExtractorDecl(ext.getId(), ext.renderName(feature),
                                feature)) //
                        .forEach(extractors::add);
            }
        }

        extractors.sort(comparing(ExtractorDecl::name));
        
        return extractors;
    }

    private List<AggregatorDecl> listAggregators()
    {
        var state = sidebar.getModelObject();
        var cells = state.cellExtractors.stream() //
                .map(this::createExtractor) //
                .toList();

        return aggregatorSupportRegistry.getExtensions(cells).stream() //
                .map(ext -> new AggregatorDecl(ext.getId(), ext.getName(), ext.supportsCells())) //
                .toList();
    }

    private void actionSelectLayer(AjaxRequestTarget aTarget)
    {
        aTarget.add(extractorSelector);
    }

    private void actionSelectExtractor(AjaxRequestTarget aTarget)
    {
    }

    private void actionSelectAggregator(AjaxRequestTarget aTarget)
    {
        aTarget.add(cellExtractorsContainer, addCell);
    }

    private static record ExtractorDecl(String id, String name, Serializable delegate)
        implements Serializable
    {}

    private static record AggregatorDecl(String id, String name, boolean supportsCells)
        implements Serializable
    {}

    @SuppressWarnings("unused")
    private static class State
        implements Serializable
    {
        private static final long serialVersionUID = 3617963902948560054L;

        AnnotationLayer layer;
        ExtractorDecl extractor;
        AggregatorDecl aggregator;

        List<ProjectUserPermissions> annotators = new ArrayList<>();
        List<SourceDocument> documents = new ArrayList<>();
        List<AnnotationDocumentState> states = new ArrayList<>();
        List<ExtractorDecl> rowExtractors = new ArrayList<>();
        List<ExtractorDecl> colExtractors = new ArrayList<>();
        List<ExtractorDecl> cellExtractors = new ArrayList<>();
    }
}
