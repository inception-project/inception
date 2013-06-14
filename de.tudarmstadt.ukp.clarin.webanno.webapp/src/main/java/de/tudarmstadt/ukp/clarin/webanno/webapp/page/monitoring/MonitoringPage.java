/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;
import org.jfree.util.UnitType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.statistics.TwoPairedKappa;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;

/**
 * Monitoring To display different monitoring and statistics measurements tabularly and graphically.
 *
 * @author Seid Muhie Yimam
 *
 */
public class MonitoringPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(MonitoringPage.class);

    private static final int CHART_WIDTH = 300;

    /**
     * The user column in the user-document status table
     */
    public static final String USER = "user:";

    /**
     * The document column in the user-document status table
     */
    public static final String DOCUMENT = "document:";
    public static final String SOURCE_DOCUMENT = "source document";

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<Project>();

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            List<Project> allProjects = projectRepository.listProjects();
                            List<Authority> authorities = projectRepository.listAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)
                                        || ApplicationUtils.isCurator(project,
                                                projectRepository, user)) {
                                    allowedProject.add(project);
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    if (aNewSelection != null) {
                        monitoringDetailForm.setModelObject(aNewSelection);
                        monitoringDetailForm.setVisible(true);
                        annotationTypeSelectionForm.setVisible(true);
                        monitoringDetailForm.setVisible(true);
                        agreementForm.setVisible(true);
                        ProjectSelectionForm.this.setVisible(true);

                        final Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
                        final Project project = aNewSelection;
                        List<SourceDocument> documents = projectRepository
                                .listSourceDocuments(project);

                        final int totalDocuments = documents.size();

                        // Annotator's Progress
                        if (project != null) {
                            annotatorsProgress.putAll(getAnnotatorsProgres(project));
                        }
                        projectName.setDefaultModelObject(project.getName());

                        annotatorsProgressImage.setImageResource(createProgressChart(
                                annotatorsProgress, totalDocuments));
                        annotatorsProgressImage.setVisible(true);

                        List<String> documentListAsColumnHeader = new ArrayList<String>();
                        documentListAsColumnHeader.add("Documents");

                        // A column for source document states
                        documentListAsColumnHeader.add(SOURCE_DOCUMENT);

                        // A column for curation user annotation document status
                        documentListAsColumnHeader.add("curation");

                        // List of users with USER permission level
                        List<User> usersWithPermissions = projectRepository
                                .listProjectUsersWithPermissions(project, PermissionLevel.USER);

                        for (User user : usersWithPermissions) {
                            documentListAsColumnHeader.add(user.getUsername());
                        }

                        List<List<String>> userAnnotationDocumentStatusList = new ArrayList<List<String>>();

                        for (SourceDocument document : documents) {
                            List<String> userAnnotationDocuments = new ArrayList<String>();
                            userAnnotationDocuments.add(DOCUMENT + document.getName());

                // source Document status
                            userAnnotationDocuments.add(SOURCE_DOCUMENT + "-" + DOCUMENT
                                    + document.getName());

                            // Curation Document status
                            userAnnotationDocuments.add(CurationPanel.CURATION_USER + "-"
                                    + DOCUMENT + document.getName());

                            for (User user : usersWithPermissions) {
                                // annotation document status for this annotator
                                userAnnotationDocuments.add(user.getUsername() + "-" + DOCUMENT
                                        + document.getName());
                            }

                            userAnnotationDocumentStatusList.add(userAnnotationDocuments);
                        }

                        TableDataProvider provider = new TableDataProvider(
                                documentListAsColumnHeader, userAnnotationDocumentStatusList);

                        List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

                        for (int i = 0; i < provider.getColumnCount(); i++) {
                            columns.add(new DocumentColumnMetaData(provider, i, project,
                                    projectRepository));
                        }
                        annotationDocumentStatusTable.remove();
                        annotationDocumentStatusTable = new DefaultDataTable("rsTable", columns,
                                provider, 20);
                        annotationDocumentStatusTable.add(new AjaxEventBehavior("onclick")
                        {
                            @Override
                            protected void onEvent(AjaxRequestTarget aTarget)
                            {
                                annotatorsProgress.clear();
                                annotatorsProgress.putAll(getAnnotatorsProgres(project));
                                annotatorsProgressImage.setImageResource(createProgressChart(
                                        annotatorsProgress, totalDocuments));
                                annotatorsProgressImage.setImageResource(createProgressChart(
                                        annotatorsProgress, totalDocuments));
                                aTarget.add(annotatorsProgressImage.setOutputMarkupId(true));

                                final Map<Project, Integer> projectsProgressPerClosedDocument = getClosedDocumentsInProject();
                                monitoringDetailForm.remove(projectRepeator);
                                projectRepeator = new RepeatingView("projectRepeator");
                                monitoringDetailForm.add(projectRepeator);
                                modifyProjectRepeater(projectRepeator,
                                        projectsProgressPerClosedDocument);

                                aTarget.add(monitoringDetailForm.setOutputMarkupId(true));

                            }
                        });
                        monitoringDetailForm.add(annotationDocumentStatusTable);

                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
        }
    }

    private class AnnotationTypeSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private ListChoice<AnnotationType> annotationTypes;

        public AnnotationTypeSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(annotationTypes = new ListChoice<AnnotationType>("annotationTypes")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationType>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationType> load()
                        {
                            return annotationService.listAnnotationType();
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationType>("name", "id"));
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            annotationTypes.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    Project project = projectSelectionForm.getModelObject().project;
                    if (project != null) {// application is starting
                        // TODO the type conversion will not be needed when the type is stored in
                        // the
                        // database

                        String type = getType(annotationTypes.getModelObject().getName());
                        String featureName = getFeatureName(annotationTypes.getModelObject()
                                .getName());
                        List<User> users = projectRepository
                                .listProjectUsersWithPermissions(project, PermissionLevel.USER);
                        double[][] results = new double[users.size()][users.size()];

                        List<SourceDocument> sourceDocuments = projectRepository
                                .listSourceDocuments(project);

                        // Filter source documents that have annotation document already.
                        List<SourceDocument> sourceDocumentsWithAnnotations = new ArrayList<SourceDocument>();
                        for (SourceDocument sourceDocument : sourceDocuments) {
                            boolean exist = true;
                            for (User user : users) {
                                AnnotationDocument annotationDocument = projectRepository
                                        .getAnnotationDocument(sourceDocument, user);
                                if (annotationDocument.getState().equals(
                                        AnnotationDocumentState.NEW)
                                        || annotationDocument.getState().equals(
                                                AnnotationDocumentState.IGNORE)) {
                                    exist = false;
                                    break;
                                }
                            }
                            if (exist) {
                                sourceDocumentsWithAnnotations.add(sourceDocument);
                            }
                        }

                        for (SourceDocument sourceDocument : sourceDocumentsWithAnnotations) {
                            TwoPairedKappa twoPairedKappa = new TwoPairedKappa(project,
                                    projectRepository);
                            Set<String> allANnotations = twoPairedKappa.getAllAnnotations(users,
                                    sourceDocument, type);
                            Map<String, Map<String, String>> userAnnotations = twoPairedKappa
                                    .initializeAnnotations(users, allANnotations);
                            userAnnotations = twoPairedKappa.updateUserAnnotations(users,
                                    sourceDocument, type, featureName, userAnnotations);
                            double[][] thisSourceDocumentResult = twoPairedKappa
                                    .getAgreement(userAnnotations);
                            // Update result per document
                            for (int i = 0; i < users.size(); i++) {
                                for (int j = 0; j < users.size(); j++) {
                                    results[i][j] = results[i][j] + thisSourceDocumentResult[i][j];
                                }
                            }
                        }

                        // get average agreement value across documents
                        for (int i = 0; i < users.size(); i++) {
                            for (int j = 0; j < users.size(); j++) {
                                results[i][j] = results[i][j]
                                        / sourceDocumentsWithAnnotations.size();
                            }
                        }

                        List<String> usersListAsColumnHeader = new ArrayList<String>();
                        usersListAsColumnHeader.add("users");

                        for (User user : users) {
                            usersListAsColumnHeader.add(user.getUsername());
                        }
                        List<List<String>> agreementResults = new ArrayList<List<String>>();
                        int i = 0;
                        for (User user1 : users) {
                            List<String> agreementResult = new ArrayList<String>();
                            agreementResult.add(user1.getUsername());

                            for (int j = 0; j < users.size(); j++) {
                                agreementResult.add((double)Math.round(results[i][j]*100)/100 + "");
                            }
                            i++;
                            agreementResults.add(agreementResult);
                        }

                        TableDataProvider provider = new TableDataProvider(usersListAsColumnHeader,
                                agreementResults);

                        List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

                        for (int m = 0; m < provider.getColumnCount(); m++) {
                            columns.add(new AgreementColumnMetaData(provider, m));
                        }
                        agreementTable.remove();
                        agreementTable = new DefaultDataTable("agreementTable", columns, provider,
                                10);
                        agreementForm.add(agreementTable);
                        aTarget.add(agreementForm);
                    }
                }
            }).setOutputMarkupId(true);
        }
    }

    Model<Project> projectModel = new Model<Project>()
    {
        private static final long serialVersionUID = -6394439155356911110L;

        @Override
        public Project getObject()
        {
            return projectSelectionForm.getModelObject().project;
        }
    };

    private Map<String, Integer> getAnnotatorsProgres(Project project)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
        if (project != null) {
            for (User user : projectRepository.listProjectUsersWithPermissions(project)) {
                for (SourceDocument document : projectRepository.listSourceDocuments(project)) {
                    if (projectRepository.isAnnotationFinished(document, project, user)) {
                        if (annotatorsProgress.get(user.getUsername()) == null) {
                            annotatorsProgress.put(user.getUsername(), 1);
                        }
                        else {
                            int previousExpectedValue = annotatorsProgress.get(user.getUsername());
                            annotatorsProgress.put(user.getUsername(), previousExpectedValue + 1);
                        }
                    }
                }
                if (annotatorsProgress.get(user.getUsername()) == null) {
                    annotatorsProgress.put(user.getUsername(), 0);
                }
            }
        }
        return annotatorsProgress;
    }

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private AnnotationType annotationTypes;
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            final Map<Project, Integer> projectsProgressPerClosedDocument = getClosedDocumentsInProject();

            // Overall progress by Projects
            projectRepeator = new RepeatingView("projectRepeator");
            add(projectRepeator);
            modifyProjectRepeater(projectRepeator, projectsProgressPerClosedDocument);

        }
    }

    private class AgreementForm
        extends Form
    {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public AgreementForm(String id, Model<AnnotationType> aType, Model<Project> aProject)

        {
            super(id);
            // Intialize the agreementTable with NOTHING.
            List<String> usersListAsColumnHeader = new ArrayList<String>();
            usersListAsColumnHeader.add("");
            List<String> agreementResult = new ArrayList<String>();
            agreementResult.add("");
            List<List<String>> agreementResults = new ArrayList<List<String>>();
            agreementResults.add(agreementResult);

            TableDataProvider provider = new TableDataProvider(usersListAsColumnHeader,
                    agreementResults);
            List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

            for (int m = 0; m < provider.getColumnCount(); m++) {
                columns.add(new AgreementColumnMetaData(provider, m));
            }
            add(agreementTable = new DefaultDataTable("agreementTable", columns, provider, 10));
        }
    }

    private class UsersModel
        implements Serializable
    {
        public List<User> users;
    }

    private String getType(String aType)
    {
        if (aType.equals("pos")) {
            aType = TwoPairedKappa.POSTYPE;
        }
        else if (aType.equals("dependency")) {
            aType = TwoPairedKappa.DEPENDENCYTYPE;
        }
        else if (aType.equals("named entity")) {
            aType = TwoPairedKappa.NAMEDENITYTYPE;
        }
        else if (aType.equals("coreference type")) {
            aType = TwoPairedKappa.COREFERENCELINKTYPE;
        }
        else if (aType.equals("coreference")) {
            aType = TwoPairedKappa.COREFERENCECHAINTYPE;
        }
        return aType;
    }

    private String getFeatureName(String aType)
    {
        String featureName = "";
        if (aType.equals("pos")) {
            featureName = AnnotationTypeConstant.POS_FEATURENAME;
        }
        else if (aType.equals("dependency")) {
            featureName = AnnotationTypeConstant.DEPENDENCY_FEATURENAME;
        }
        else if (aType.equals("named entity")) {
            featureName = AnnotationTypeConstant.NAMEDENTITY_FEATURENAME;
        }

        else if (aType.equals("coreference type")) {
            featureName = AnnotationTypeConstant.COREFERENCELINK_FEATURENAME;
        }
        else if (aType.equals("coreference")) {
            featureName = AnnotationTypeConstant.COREFERENCECHAIN_FEATURENAME;
        }

        return featureName;
    }

    private void modifyProjectRepeater(RepeatingView aProjectRepeater,
            final Map<Project, Integer> aProjectsProgressPerClosedDocument)
    {
        for (final Project project : aProjectsProgressPerClosedDocument.keySet()) {

            final int totaldocuments = projectRepository.listSourceDocuments(project).size();

            AbstractItem item = new AbstractItem(aProjectRepeater.newChildId());
            aProjectRepeater.add(item);

            item.add(new Label("project", project.getName()));
            item.add(new ProgressBar("projectProgress", new ProgressionModel()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected Progression getProgression()
                {
                    double value = (double) aProjectsProgressPerClosedDocument.get(project)
                            / totaldocuments;
                    return new Progression((int) (value * 100));
                }
            }));
        }
    }

    private Map<Project, Integer> getClosedDocumentsInProject()
    {
        Map<Project, Integer> projectsProgressPerClosedDocument = new HashMap<Project, Integer>();
        for (Project project : projectRepository.listProjects()) {
            for (SourceDocument document : projectRepository.listSourceDocuments(project)) {

                // Projects Progress
                if (document.getState().equals(SourceDocumentState.ANNOTATION_FINISHED)) {
                    if (projectsProgressPerClosedDocument.get(project) == null) {
                        projectsProgressPerClosedDocument.put(project, 1);

                    }
                    else {
                        int previousExpectedValue = projectsProgressPerClosedDocument.get(project);
                        projectsProgressPerClosedDocument.put(project, previousExpectedValue + 1);
                    }
                }
            }
            if (projectsProgressPerClosedDocument.get(project) == null) {
                projectsProgressPerClosedDocument.put(project, 0);
            }
        }
        return projectsProgressPerClosedDocument;
    }

    private ProjectSelectionForm projectSelectionForm;
    private MonitoringDetailForm monitoringDetailForm;
    private Image annotatorsProgressImage;
    private DefaultDataTable annotationDocumentStatusTable;
    private DefaultDataTable agreementTable;
    private Label projectName;
    private RepeatingView projectRepeator;
    private AgreementForm agreementForm;
    private AnnotationTypeSelectionForm annotationTypeSelectionForm;

    public MonitoringPage()
        throws UIMAException, IOException, ClassNotFoundException
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");
        // monitoringDetailForm.setVisible(false);
        agreementForm = new AgreementForm("agreementForm", new Model<AnnotationType>(),
                new Model<Project>());
        agreementForm.setVisible(false);
        add(agreementForm);

        annotationTypeSelectionForm = new AnnotationTypeSelectionForm("annotationTypeSelectionForm");
        annotationTypeSelectionForm.setVisible(false);
        add(annotationTypeSelectionForm);

        annotatorsProgressImage = new NonCachingImage("annotator");
        annotatorsProgressImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressImage.setVisible(false);
        add(projectSelectionForm);
        projectName = new Label("projectName", "");

        Project project = projectRepository.listProjects().get(0);

        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
        List<SourceDocument> dc = projectRepository.listSourceDocuments(project);
        for (User user : projectRepository.listProjectUsersWithPermissions(project)) {
            List<String> userAnnotationDocument = new ArrayList<String>();
            userAnnotationDocument.add("");
            for (int i = 0; i < dc.size(); i++) {
                userAnnotationDocument.add("");
            }
            userAnnotationDocumentLists.add(userAnnotationDocument);
        }
        List<String> documentListAsColumnHeader = new ArrayList<String>();
        documentListAsColumnHeader.add("Users");
        for (SourceDocument d : dc) {
            documentListAsColumnHeader.add(d.getName());
        }
        TableDataProvider prov = new TableDataProvider(documentListAsColumnHeader,
                userAnnotationDocumentLists);

        List<IColumn<?>> cols = new ArrayList<IColumn<?>>();

        for (int i = 0; i < prov.getColumnCount(); i++) {
            cols.add(new DocumentColumnMetaData(prov, i, new Project(), projectRepository));
        }
        annotationDocumentStatusTable = new DefaultDataTable("rsTable", cols, prov, 2);
        monitoringDetailForm.setVisible(false);
        add(monitoringDetailForm.add(annotatorsProgressImage).add(projectName)
                .add(annotationDocumentStatusTable));
        annotationDocumentStatusTable.setVisible(false);
    }

    private JFreeChart createPieChart(Map<String, Integer> chartValues)
    {

        // fill dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (String chartValue : chartValues.keySet()) {
            dataset.setValue(chartValue, chartValues.get(chartValue));
        }
        // create chart
        JFreeChart chart = ChartFactory.createPieChart3D(null, dataset, false, true, false);
        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setStartAngle(290);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setIgnoreZeroValues(true);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);
        plot.setInteriorGap(0.0);
        plot.setMaximumLabelWidth(0.22);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} {1} ({2})"));
        plot.setDepthFactor(0.25);
        plot.setCircular(true);
        plot.setDarkerSides(true);
        return chart;
    }

    private ChartImageResource createProgressChart(Map<String, Integer> chartValues, int aMaxValue)
    {
        // fill dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String chartValue : chartValues.keySet()) {
            dataset.setValue(chartValues.get(chartValue), "Completion", chartValue);
        }
        // create chart
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setInsets(new RectangleInsets(UnitType.ABSOLUTE, 0, 20, 0, 20));
        plot.getRangeAxis().setRange(0.0, aMaxValue);
        ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0"));
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        // renderer.setGradientPaintTransformer(new StandardGradientPaintTransformer(
        // GradientPaintTransformType.HORIZONTAL));
        renderer.setSeriesPaint(0, Color.BLUE);
        chart.getCategoryPlot().setRenderer(renderer);

        return new ChartImageResource(chart, CHART_WIDTH, 30 + (chartValues.size() * 18));
    }
}
