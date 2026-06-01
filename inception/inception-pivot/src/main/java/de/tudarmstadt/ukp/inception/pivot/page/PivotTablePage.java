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
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
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
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.FeatureBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.GeneralBinding;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.LayerBinding;
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.report.AggregatorDecl;
import de.tudarmstadt.ukp.inception.pivot.report.ExtractorDecl;
import de.tudarmstadt.ukp.inception.pivot.report.ReportDecl;
import de.tudarmstadt.ukp.inception.pivot.report.ReportService;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTable;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTableDataProvider;
import de.tudarmstadt.ukp.inception.pivot.table.PivotTableFilterState;
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
    private @SpringBean ExtractorSupportRegistry extractorSupportRegistry;
    private @SpringBean AggregatorSupportRegistry aggregatorSupportRegistry;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean ReportService reportService;

    private List<ReportTab> tabs;
    private ReportTab activeTab;
    private WebMarkupContainer tabStrip;

    private final Form<ReportDecl> sidebar;
    private final DropDownChoice<AggregatorDecl> aggregatorSelector;
    private Component pivotTable;
    private final ListView<ExtractorDecl> rowExtractors;
    private final ListView<ExtractorDecl> colExtractors;

    private final WebMarkupContainer cellExtractorsContainer;
    private final ListView<ExtractorDecl> cellExtractors;

    public PivotTablePage(PageParameters aParameters)
    {
        super(aParameters);

        var sessionOwner = userService.getCurrentUser();

        requireProjectRole(sessionOwner, MANAGER, CURATOR);

        initTabs();

        pivotTable = buildExampleTable();
        queue(pivotTable);

        sidebar = new Form<>("sidebar",
                new CompoundPropertyModel<>(new PropertyModel<ReportDecl>(this, "activeDecl")));
        sidebar.setOutputMarkupId(true);
        queue(sidebar);

        tabStrip = new WebMarkupContainer("tabStrip");
        tabStrip.setOutputMarkupId(true);
        queue(tabStrip);

        queue(new ListView<ReportTab>("reportTabs", LoadableDetachableModel.of(() -> tabs))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ReportTab> aItem)
            {
                populateTabItem(aItem);
            }
        });
        queue(new LambdaAjaxLink("addTab", this::actionAddTab));

        aggregatorSelector = new DropDownChoice<AggregatorDecl>("aggregator");
        aggregatorSelector.setOutputMarkupId(true);
        aggregatorSelector.setChoices(LoadableDetachableModel.of(this::listAggregators));
        aggregatorSelector.setChoiceRenderer(new LambdaChoiceRenderer<>(AggregatorDecl::name));
        aggregatorSelector.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionSelectAggregator));
        queue(aggregatorSelector);

        var layerGroupsModel = LoadableDetachableModel.of(this::listLayerGroups);
        queue(new ExtractorPickerPanel("rowPicker", layerGroupsModel, //
                LoadableDetachableModel.of(() -> sidebar.getModelObject().getRowExtractors())) //
                        .onAddAction((t, e) -> actionAddTo(t, e,
                                sidebar.getModelObject().getRowExtractors())));
        queue(new ExtractorPickerPanel("colPicker", layerGroupsModel, //
                LoadableDetachableModel.of(() -> sidebar.getModelObject().getColExtractors())) //
                        .onAddAction((t, e) -> actionAddTo(t, e,
                                sidebar.getModelObject().getColExtractors())));
        queue(new ExtractorPickerPanel("cellPicker", layerGroupsModel, //
                LoadableDetachableModel.of(() -> sidebar.getModelObject().getCellExtractors())) //
                        .onAddAction((t, e) -> actionAddTo(t, e,
                                sidebar.getModelObject().getCellExtractors())));

        rowExtractors = new ListView<ExtractorDecl>("rowExtractors")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ExtractorDecl> aItem)
            {
                var decl = aItem.getModelObject();
                aItem.add(new Label("name", decl.name()));
                aItem.add(new Label("layer", extractorLayer(decl)));
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
                var decl = aItem.getModelObject();
                aItem.add(new Label("name", decl.name()));
                aItem.add(new Label("layer", extractorLayer(decl)));
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
                var decl = aItem.getModelObject();
                aItem.add(new Label("name", decl.name()));
                aItem.add(new Label("layer", extractorLayer(decl)));
                aItem.add(new LambdaAjaxLink("remove", t -> actionRemoveExtractor(t, aItem, this)));
            }
        };
        queue(cellExtractors);

        var noCellExtractors = new WebMarkupContainer("noCellExtractors");
        noCellExtractors.add(visibleWhen(cellExtractors.getModel().map(List::isEmpty)));
        queue(noCellExtractors);

        queue(new LambdaAjaxButton<ReportDecl>("switchRowsCols", this::actionSwitchRowsCols));
        queue(new LambdaAjaxButton<ReportDecl>("switchColsCells", this::actionSwitchColsCells));
        queue(new LambdaAjaxButton<ReportDecl>("save", (t, f) -> actionSaveTab(t, activeTab)));
        queue(new LambdaAjaxButton<ReportDecl>("run", this::actionRun));

        var annotatorList = new ListMultipleChoice<ProjectUserPermissions>("annotators");
        annotatorList.setChoiceRenderer(new ProjectUserPermissionChoiceRenderer());
        annotatorList.setChoices(reportService.listDataOwners(getProject()));
        annotatorList.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionMarkActiveTabDirty));
        queue(annotatorList);

        var documentList = new ListMultipleChoice<SourceDocument>("documents");
        documentList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        documentList.setChoices(listDocuments());
        documentList.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionMarkActiveTabDirty));
        queue(documentList);
    }

    private List<SourceDocument> listDocuments()
    {
        return documentService.listSourceDocuments(getProject());
    }

    private boolean isReportNameAvailable(String aName, PivotReport aExclude)
    {
        return reportService.listReports(getProject()).stream() //
                .filter(r -> r.getName().equals(aName)) //
                .allMatch(r -> aExclude != null && Objects.equals(r.getId(), aExclude.getId()));
    }

    public ReportDecl getActiveDecl()
    {
        if (activeTab == null) {
            return null;
        }

        ensureDecl(activeTab);
        return activeTab.decl;
    }

    public void setActiveDecl(ReportDecl aDecl)
    {
        if (activeTab != null) {
            activeTab.decl = aDecl;
        }
    }

    private void initTabs()
    {
        tabs = new ArrayList<>();
        for (var report : reportService.listReports(getProject())) {
            var tab = new ReportTab();
            tab.report = report;
            tab.name = report.getName();
            tab.description = report.getDescription();
            tabs.add(tab);
        }

        if (tabs.isEmpty()) {
            tabs.add(new ReportTab());
        }

        activeTab = tabs.get(0);
    }

    /**
     * Lazily loads (and resolves) the report definition backing the given tab on first access. New
     * and freshly loaded reports also receive a default aggregator.
     */
    private void ensureDecl(ReportTab aTab)
    {
        if (aTab.decl != null) {
            return;
        }

        ReportDecl decl;
        if (aTab.report != null) {
            try {
                var def = reportService.readDef(aTab.report);
                if (def != null) {
                    var resolved = reportService.resolve(def, getProject());
                    decl = resolved.decl();
                    resolved.problems().forEach(p -> p.toWicket(this));
                }
                else {
                    decl = new ReportDecl();
                }
            }
            catch (Exception e) {
                // Isolate the failure to this tab so that a single broken report definition does
                // not break the entire page (and the other reports).
                LOG.error("Unable to load report [{}]", aTab.report.getName(), e);
                error("Unable to load report [" + aTab.report.getName() + "]: " + e.getMessage());
                decl = new ReportDecl();
            }
        }
        else {
            decl = new ReportDecl();
        }

        // Assign before computing the default aggregator, as listAggregators() reads the active
        // declaration back via the sidebar model.
        aTab.decl = decl;

        if (decl.getAggregator() == null) {
            var aggregators = listAggregators();
            if (!aggregators.isEmpty()) {
                decl.setAggregator(aggregators.get(0));
            }
        }
    }

    private void populateTabItem(ListItem<ReportTab> aItem)
    {
        var tab = aItem.getModelObject();
        var active = tab == activeTab;

        // Inactive tabs are plain Ajax links that activate the tab when clicked.
        var select = new LambdaAjaxLink("select", t -> actionSelectTab(t, tab));
        select.add(dirtyMarker("selectDirty", tab));
        select.add(new Label("selectName", new PropertyModel<String>(aItem.getModel(), "name")));
        select.setVisible(!active);
        aItem.add(select);

        // The active tab shows an inline-editable title plus a management menu (duplicate/delete).
        var activeContainer = new WebMarkupContainer("active");
        activeContainer.setVisible(active);
        var dirty = dirtyMarker("dirty", tab);
        activeContainer.add(dirty);

        // Renaming commits to the in-memory model on leave/Enter (and marks the tab dirty); the
        // durable write happens only via the Save button in the sidebar footer. We only refresh the
        // dirty marker here - AjaxEditableLabel already re-renders itself, so adding its ancestor
        // (the tab strip) to the target would clash with that update.
        var title = new AjaxEditableLabel<String>("title",
                new PropertyModel<String>(aItem.getModel(), "name"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                super.onSubmit(aTarget);
                tab.dirty = true;
                aTarget.add(dirty);
            }
        };
        activeContainer.add(title);

        var menu = new WebMarkupContainer("menu");
        menu.add(new LambdaAjaxLink("duplicate", t -> actionDuplicateTab(t, tab)));
        menu.add(new LambdaAjaxLink("delete", t -> actionDeleteTab(t, tab)));
        activeContainer.add(menu);

        aItem.add(activeContainer);
    }

    /**
     * A small dot shown on a tab while it has changes that have not yet been persisted via Save.
     */
    private WebMarkupContainer dirtyMarker(String aId, ReportTab aTab)
    {
        var marker = new WebMarkupContainer(aId);
        marker.setOutputMarkupPlaceholderTag(true);
        marker.add(visibleWhen(() -> aTab.dirty));
        return marker;
    }

    private void actionMarkActiveTabDirty(AjaxRequestTarget aTarget)
    {
        if (activeTab != null) {
            activeTab.dirty = true;
        }
        aTarget.add(tabStrip);
    }

    private void actionSelectTab(AjaxRequestTarget aTarget, ReportTab aTab)
    {
        activeTab = aTab;
        ensureDecl(aTab);

        aTarget.add(tabStrip, sidebar);
        refreshPivotTable(aTarget);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionAddTab(AjaxRequestTarget aTarget)
    {
        var tab = new ReportTab();
        tabs.add(tab);
        activeTab = tab;
        ensureDecl(tab);

        aTarget.add(tabStrip, sidebar);
        refreshPivotTable(aTarget);
    }

    private void actionSaveTab(AjaxRequestTarget aTarget, ReportTab aTab)
    {
        if (aTab.name == null || aTab.name.isBlank()) {
            info("Please enter a report name.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (!isReportNameAvailable(aTab.name, aTab.report)) {
            error("Another report named '" + aTab.name + "' already exists in this project.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        ensureDecl(aTab);

        var report = aTab.report != null ? aTab.report : new PivotReport(getProject(), aTab.name);
        report.setName(aTab.name);
        report.setDescription(aTab.description);
        reportService.writeDef(report, reportService.toDef(aTab.decl));
        reportService.createOrUpdateReport(report);
        aTab.report = report;
        aTab.dirty = false;

        info("Report '" + report.getName() + "' saved.");
        aTarget.add(tabStrip, sidebar);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionDuplicateTab(AjaxRequestTarget aTarget, ReportTab aTab)
    {
        ensureDecl(aTab);

        var copy = new ReportTab();
        copy.name = aTab.name + " (copy)";
        copy.description = aTab.description;
        copy.decl = cloneDecl(aTab.decl);
        // A duplicate has no persisted report of its own yet, so it starts out with unsaved
        // changes.
        copy.dirty = true;
        tabs.add(copy);
        activeTab = copy;

        aTarget.add(tabStrip, sidebar);
        refreshPivotTable(aTarget);
    }

    private void actionDeleteTab(AjaxRequestTarget aTarget, ReportTab aTab)
    {
        if (aTab.report != null) {
            reportService.deleteReport(aTab.report);
        }

        tabs.remove(aTab);
        if (tabs.isEmpty()) {
            tabs.add(new ReportTab());
        }

        if (activeTab == aTab) {
            activeTab = tabs.get(0);
            ensureDecl(activeTab);
        }

        info("Report '" + aTab.name + "' deleted.");
        aTarget.add(tabStrip, sidebar);
        refreshPivotTable(aTarget);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private ReportDecl cloneDecl(ReportDecl aDecl)
    {
        var copy = new ReportDecl();
        copy.setAggregator(aDecl.getAggregator());
        copy.setRowExtractors(new ArrayList<>(aDecl.getRowExtractors()));
        copy.setColExtractors(new ArrayList<>(aDecl.getColExtractors()));
        copy.setCellExtractors(new ArrayList<>(aDecl.getCellExtractors()));
        copy.setAnnotators(new ArrayList<>(aDecl.getAnnotators()));
        copy.setDocuments(new ArrayList<>(aDecl.getDocuments()));
        copy.setStates(new ArrayList<>(aDecl.getStates()));
        return copy;
    }

    /**
     * The group an extractor belongs to (typically its layer), shown as muted secondary context
     * next to its label.
     */
    private static String extractorLayer(ExtractorDecl aDecl)
    {
        return aDecl.binding().groupLabel();
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

    private void actionSwitchRowsCols(AjaxRequestTarget aTarget, Form<ReportDecl> aForm)
    {
        var state = aForm.getModelObject();
        var cols = new ArrayList<>(state.getColExtractors());
        var rows = new ArrayList<>(state.getRowExtractors());

        state.getColExtractors().clear();
        state.getColExtractors().addAll(rows);

        state.getRowExtractors().clear();
        state.getRowExtractors().addAll(cols);

        activeTab.dirty = true;
        aTarget.add(sidebar, tabStrip);
    }

    private void actionSwitchColsCells(AjaxRequestTarget aTarget, Form<ReportDecl> aForm)
    {
        var state = aForm.getModelObject();
        var cols = new ArrayList<>(state.getColExtractors());
        var cells = new ArrayList<>(state.getCellExtractors());

        state.getColExtractors().clear();
        state.getColExtractors().addAll(cells);

        state.getCellExtractors().clear();
        state.getCellExtractors().addAll(cols);

        activeTab.dirty = true;
        aTarget.add(sidebar, tabStrip);
    }

    private void actionRemoveExtractor(AjaxRequestTarget aTarget, ListItem<ExtractorDecl> aItem,
            ListView<ExtractorDecl> aListView)
    {
        aListView.getModelObject().remove(aItem.getModelObject());

        activeTab.dirty = true;
        aTarget.add(sidebar, tabStrip);
    }

    @SuppressWarnings("rawtypes")
    private Extractor createExtractor(ExtractorDecl aDecl)
    {
        return extractorSupportRegistry.getExtension(aDecl.id()).get()
                .createExtractor(aDecl.binding());
    }

    @SuppressWarnings("unchecked")
    private void actionRun(AjaxRequestTarget aTarget, Form<ReportDecl> aForm)
    {
        var state = aForm.getModelObject();

        if (state.getRowExtractors().isEmpty() && state.getColExtractors().isEmpty()) {
            info("At least one row or column extractor is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (state.getAggregator() == null) {
            info("Please select an aggregator.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (state.getAggregator().supportsCells() && state.getCellExtractors().isEmpty()) {
            info("At least one cell extractor is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        List<Extractor> rows = state.getRowExtractors().stream().map(this::createExtractor)
                .toList();
        List<Extractor> cols = state.getColExtractors().stream().map(this::createExtractor)
                .toList();
        List<Extractor> cells = state.getAggregator().supportsCells()
                ? state.getCellExtractors().stream().map(this::createExtractor).toList()
                : emptyList();

        // The data collection iterates over annotations of the trigger types reported by the
        // extractors. If every selected extractor is layer-less (i.e. has no trigger type), there
        // is nothing to anchor the iteration on and the report would silently come back empty.
        var hasTrigger = Stream.of(rows, cols, cells) //
                .flatMap(List::stream) //
                .anyMatch(e -> e.getTriggerType().isPresent());
        if (!hasTrigger) {
            error("At least one row, column or cell extractor must be bound to a layer or "
                    + "feature - otherwise there is nothing to count.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var agg = getAggregatorSupport(state.getAggregator());

        var dataOwners = (state.getAnnotators().isEmpty()
                ? reportService.listDataOwners(getProject())
                : state.getAnnotators()).stream() //
                        .map(ProjectUserPermissions::getUser) //
                        .filter(Optional::isPresent) //
                        .map(Optional::get) //
                        .map(User::getUsername).toList();

        var sessionOwner = userService.getCurrentUser();
        var states = state.getStates().isEmpty() ? asList(IN_PROGRESS, FINISHED, IGNORE)
                : state.getStates();
        var allAnnDocs = documentService.listAnnotationDocumentsInState(getProject(), //
                states.toArray(AnnotationDocumentState[]::new)).stream() //
                .collect(groupingBy(AnnotationDocument::getDocument));

        if (isNotEmpty(state.getDocuments())) {
            allAnnDocs.keySet().retainAll(state.getDocuments());
            for (var doc : state.getDocuments()) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }
        else {
            for (var doc : documentService.listSourceDocuments(getProject())) {
                allAnnDocs.computeIfAbsent(doc, $ -> emptyList());
            }
        }

        if (allAnnDocs.isEmpty()) {
            info("No annotation documents found for the selected criteria.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
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

        var result = task.getResult();
        result.setFilterState(new PivotTableFilterState());
        activeTab.result = result;
        activeTab.resultAggregator = state.getAggregator();

        refreshPivotTable(aTarget);
    }

    /**
     * Renders the active tab's last-run result, rebuilding the cell renderer from the aggregator
     * that produced it, or shows an empty placeholder if the tab has not been run yet.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void refreshPivotTable(AjaxRequestTarget aTarget)
    {
        Component newTable = null;
        if (activeTab != null && activeTab.result != null) {
            // The aggregator that produced the cached result may no longer be available (e.g. its
            // extension was removed by a redeployment). Fall back to the empty placeholder rather
            // than throwing, as this path is also reached on plain tab activation and would
            // otherwise lock the user out of switching to any other tab.
            var support = aggregatorSupportRegistry.getExtension(activeTab.resultAggregator.id());
            if (support.isPresent()) {
                newTable = new PivotTable("pivotTable", activeTab.result,
                        support.get().createCellRenderer());
            }
            else {
                error("Cannot render the cached result: aggregator ["
                        + activeTab.resultAggregator.id() + "] is no longer available.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        if (newTable == null) {
            newTable = new EmptyPanel("pivotTable").setOutputMarkupPlaceholderTag(true);
        }

        pivotTable.replaceWith(newTable);
        pivotTable = newTable;
        aTarget.add(newTable);
    }

    private AggregatorSupport getAggregatorSupport(AggregatorDecl aAggregator)
    {
        return aggregatorSupportRegistry.getExtension(aAggregator.id()) //
                .orElseThrow(() -> new IllegalStateException(
                        "No such aggregator: [" + aAggregator.id() + "]"));
    }

    private void actionAddTo(AjaxRequestTarget aTarget, ExtractorDecl aDecl,
            List<ExtractorDecl> aTargetList)
    {
        if (aTargetList.contains(aDecl)) {
            info("'" + aDecl.name() + "' has already been added.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        aTargetList.add(aDecl);
        activeTab.dirty = true;
        aTarget.add(sidebar, tabStrip);
    }

    private List<ExtractorPickerPanel.LayerGroup> listLayerGroups()
    {
        var groups = new ArrayList<ExtractorPickerPanel.LayerGroup>();

        var generalBinding = new GeneralBinding();
        var generalSupports = extractorSupportRegistry.getExtensions(generalBinding);
        var generalIds = generalSupports.stream().map(s -> s.getId()).collect(toSet());

        var generalExtractors = generalSupports.stream() //
                .map(ext -> new ExtractorDecl(ext.getId(), ext.renderLabel(generalBinding),
                        generalBinding)) //
                .sorted(comparing(ExtractorDecl::name)) //
                .toList();
        if (!generalExtractors.isEmpty()) {
            groups.add(new ExtractorPickerPanel.LayerGroup(generalBinding.groupLabel(),
                    generalExtractors));
        }

        for (var layer : schemaService.listEnabledLayers(getProject())) {
            var extractors = new ArrayList<ExtractorDecl>();

            var layerBinding = new LayerBinding(layer);
            extractorSupportRegistry.getExtensions(layerBinding).stream() //
                    .filter(ext -> !generalIds.contains(ext.getId())) //
                    .map(ext -> new ExtractorDecl(ext.getId(), ext.renderLabel(layerBinding),
                            layerBinding)) //
                    .forEach(extractors::add);

            var adapter = schemaService.getAdapter(layer);
            for (var feature : adapter.listFeatures()) {
                var featureBinding = new FeatureBinding(feature);
                extractorSupportRegistry.getExtensions(featureBinding).stream() //
                        .map(ext -> new ExtractorDecl(ext.getId(), ext.renderLabel(featureBinding),
                                featureBinding)) //
                        .forEach(extractors::add);
            }

            extractors.sort(comparing(ExtractorDecl::name));
            if (!extractors.isEmpty()) {
                groups.add(
                        new ExtractorPickerPanel.LayerGroup(layerBinding.groupLabel(), extractors));
            }
        }
        return groups;
    }

    private List<AggregatorDecl> listAggregators()
    {
        var state = sidebar.getModelObject();
        var cells = state.getCellExtractors().stream() //
                .map(this::createExtractor) //
                .toList();

        return aggregatorSupportRegistry.getExtensions(cells).stream() //
                .map(ext -> new AggregatorDecl(ext.getId(), ext.getName(), ext.supportsCells())) //
                .toList();
    }

    private void actionSelectAggregator(AjaxRequestTarget aTarget)
    {
        if (activeTab != null) {
            activeTab.dirty = true;
        }
        aTarget.add(sidebar, tabStrip);
    }

    /**
     * An open report tab. Holds the persisted report (or {@code null} while still unsaved) along
     * with its editable name/description and the lazily-loaded editor declaration.
     */
    private static class ReportTab
        implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private PivotReport report;
        private String name = "Untitled";
        private String description;
        private ReportDecl decl;
        private PivotTableDataProvider<?, ?> result;
        private AggregatorDecl resultAggregator;
        private boolean dirty;

        @SuppressWarnings("unused") // Accessed via PropertyModel
        public String getName()
        {
            return name;
        }

        @SuppressWarnings("unused") // Accessed via PropertyModel
        public void setName(String aName)
        {
            name = aName;
        }

        @SuppressWarnings("unused") // Accessed via PropertyModel
        public String getDescription()
        {
            return description;
        }

        @SuppressWarnings("unused") // Accessed via PropertyModel
        public void setDescription(String aDescription)
        {
            description = aDescription;
        }
    }
}
