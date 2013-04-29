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
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
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
                            List<Authority> authorities = projectRepository.getAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)) {
                                    allowedProject.add(project);
                                }
                                else {
                                    error("You don't have permission!");
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

                        annotatorsProgressImage.setImageResource(createProgressChart(annotatorsProgress,
                                totalDocuments));
                        annotatorsProgressImage.setVisible(true);

                        List<String> documentListAsColumnHeader = new ArrayList<String>();
                        documentListAsColumnHeader.add("Documents");

                        for (User user : projectRepository.listProjectUsersWithPermissions(project)) {
                            documentListAsColumnHeader.add(user.getUsername());
                        }
                        List<List<String>> userAnnotationDocumentStatusList = new ArrayList<List<String>>();
                        for (SourceDocument document : documents) {
                            List<String> userAnnotationDocuments = new ArrayList<String>();
                            userAnnotationDocuments.add(DOCUMENT+ document.getName());
                            for (User user : projectRepository.listProjectUsersWithPermissions(project)) {
                                userAnnotationDocuments.add(user.getUsername() + "-" + DOCUMENT
                                        + document.getName());
                            }
                            userAnnotationDocumentStatusList.add(userAnnotationDocuments);
                        }

                        UserAnnotatedDocumentProvider provider = new UserAnnotatedDocumentProvider(
                                documentListAsColumnHeader, userAnnotationDocumentStatusList);

                        List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

                        for (int i = 0; i < provider.getColumnCount(); i++) {
                            columns.add(new DocumentColumnMetaData(provider, i, project,
                                    projectRepository));
                        }
                        table.remove();
                        table = new DefaultDataTable("rsTable", columns, provider, 10);
                        table.add(new AjaxEventBehavior("onclick")
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
                        monitoringDetailForm.add(table);

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
    Image annotatorsProgressImage;
    DefaultDataTable table;
    private Label projectName;
    RepeatingView projectRepeator;

    public MonitoringPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");
        // monitoringDetailForm.setVisible(false);

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
        UserAnnotatedDocumentProvider prov = new UserAnnotatedDocumentProvider(
                documentListAsColumnHeader, userAnnotationDocumentLists);

        List<IColumn<?>> cols = new ArrayList<IColumn<?>>();

        for (int i = 0; i < prov.getColumnCount(); i++) {
            cols.add(new DocumentColumnMetaData(prov, i, new Project(), projectRepository));
        }
        table = new DefaultDataTable("rsTable", cols, prov, 2);
        add(monitoringDetailForm.add(annotatorsProgressImage).add(projectName).add(table));
        table.setVisible(false);
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
