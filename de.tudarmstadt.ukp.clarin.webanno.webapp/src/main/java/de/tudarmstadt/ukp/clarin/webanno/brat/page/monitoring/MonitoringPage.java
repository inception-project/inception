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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.monitoring;

import static org.uimafit.util.JCasUtil.select;

import java.awt.Color;
import java.awt.GradientPaint;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.html.panel.Panel;
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
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.StandardGradientPaintTransformer;
import org.jfree.util.Rotation;
import org.jfree.util.UnitType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
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

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Button creatProject;

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

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private List<String> documents;
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        AbstractTab overallProgress;
        AbstractTab projectProgress;
        AbstractTab keppaStatistics;
        AbstractTab documentProgress;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            List<ITab> tabs = new ArrayList<ITab>();
            tabs.add(overallProgress = new AbstractTab(new Model<String>("Overall Progress"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new OverallProgressPanel(panelId);
                }
            });

            tabs.add(projectProgress = new AbstractTab(new Model<String>("Project Progress"))
            {
                private static final long serialVersionUID = 7160734867954315366L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectProgressPanel(panelId);
                }
            });

            tabs.add(documentProgress = new AbstractTab(new Model<String>("Document Progress"))
            {
                private static final long serialVersionUID = 1170760600317199418L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new DocumentProgressPanel(panelId);
                }
            });

            tabs.add(keppaStatistics = new AbstractTab(new Model<String>("Keppa Statistics"))
            {
                private static final long serialVersionUID = -3205723896786674220L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new KeppaStaticsPanel(panelId);
                }
            });

            add(new AjaxTabbedPanel("tabs", tabs));
            MonitoringDetailForm.this.setMultiPart(true);
        }
    }

    private ProjectSelectionForm projectSelectionForm;
    private MonitoringDetailForm monitoringDetailForm;

    public MonitoringPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");
        // monitoringDetailForm.setVisible(false);

        add(projectSelectionForm);
        add(monitoringDetailForm);
    }

    private class OverallProgressPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        public OverallProgressPanel(String id)
        {
            super(id);
            int actual = 0;
            int expected = 0;
            int pos = 0;
            int namedEntity = 0;
            int dependency = 0;
            int coreference = 0;
            int coreferenceType = 0;
            final Map<String, Integer> annotatorProgressExpected = new HashMap<String, Integer>();
            final Map<String, Integer> annotatorProgressActual = new HashMap<String, Integer>();
            final Map<String, Integer> projectsProgressExpected = new HashMap<String, Integer>();
            final Map<String, Integer> projectsProgressActual = new HashMap<String, Integer>();

            for (AnnotationDocument annotationDocument : projectRepository.listAnnotationDocument()) {
                try {

                    JCas jCas = projectRepository.getAnnotationDocumentContent(annotationDocument);
                    expected = expected + MonitoringUtils.expectedAnnotations(jCas);
                    actual = actual + MonitoringUtils.actualAnnotations(jCas);
                    pos = pos + select(jCas, POS.class).size();
                    namedEntity = namedEntity + select(jCas, NamedEntity.class).size();
                    dependency = dependency + select(jCas, Dependency.class).size();
                    coreference = coreference + select(jCas, CoreferenceChain.class).size();
                    coreferenceType = coreferenceType + select(jCas, CoreferenceLink.class).size();

                    // Annotator's Progress
                    String username = annotationDocument.getUser().getUsername();
                    if (annotatorProgressExpected.get(username) == null) {
                        annotatorProgressExpected.put(username,
                                MonitoringUtils.expectedAnnotations(jCas));
                        annotatorProgressActual.put(username,
                                MonitoringUtils.actualAnnotations(jCas));

                    }
                    else {
                        int previousExpectedValue = annotatorProgressExpected.get(username);
                        int previousActualValue = annotatorProgressActual.get(username);
                        annotatorProgressExpected.put(username, previousExpectedValue
                                + MonitoringUtils.expectedAnnotations(jCas));
                        annotatorProgressActual.put(username,
                                previousActualValue + MonitoringUtils.actualAnnotations(jCas));
                    }

                    // Projects Progress
                    String project = annotationDocument.getProject().getName();
                    if (projectsProgressExpected.get(project) == null) {
                        projectsProgressExpected.put(project,
                                MonitoringUtils.expectedAnnotations(jCas));
                        projectsProgressActual.put(project,
                                MonitoringUtils.actualAnnotations(jCas));

                    }
                    else {
                        int previousExpectedValue = projectsProgressExpected.get(project);
                        int previousActualValue = projectsProgressActual.get(project);
                        projectsProgressExpected.put(project, previousExpectedValue
                                + MonitoringUtils.expectedAnnotations(jCas));
                        projectsProgressActual.put(project,
                                previousActualValue + MonitoringUtils.actualAnnotations(jCas));
                    }
                }
                catch (UIMAException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ClassNotFoundException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (IOException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }

            add(new Label("expected", "" + expected));
            add(new Label("actual", "" + actual));

            Image overallChart = new NonCachingImage("overallChart");
            Map<String, Integer> overallProgress = new HashMap<String, Integer>();
            overallProgress.put("expected", expected - actual);
            overallProgress.put("actual", actual);
            overallChart.setImageResource(new ChartImageResource(createPieChart(overallProgress),
                    CHART_WIDTH, 140));
            add(overallChart);

            Map<String, Integer> annotationProgress = new HashMap<String, Integer>();
            annotationProgress.put("POS", pos);
            annotationProgress.put("ne", namedEntity);
            annotationProgress.put("dep", dependency);
            annotationProgress.put("coref", coreference);
            annotationProgress.put("coreft", coreferenceType);
            annotationProgress.put("expected", expected - actual);

            // Add overall progress by annotation types
            Image annotationChart = new NonCachingImage("annotationChart");
            annotationChart.setImageResource(new ChartImageResource(
                    createPieChart(annotationProgress), CHART_WIDTH, 140));
            add(annotationChart);

            //Add overall progress by annotators
            RepeatingView annotatorRepeator = new RepeatingView("annotatorRepeator");
            add(annotatorRepeator);

            for (final String username : annotatorProgressExpected.keySet()) {
                AbstractItem item = new AbstractItem(annotatorRepeator.newChildId());

                annotatorRepeator.add(item);
                item.add(new Label("annotator", username));
                item.add(new ProgressBar("annotatorProgress", new ProgressionModel()
                {
                    @Override
                    protected Progression getProgression()
                    {
                        double value =  (double) annotatorProgressActual.get(username) / annotatorProgressExpected
                                .get(username);
                        return new Progression(
                                (int)(value*100));
                    }
                }));
            }

            // Overall progress by Projects
            RepeatingView projectRepeator = new RepeatingView("projectRepeator");
            add(projectRepeator);

            for (final String project : projectsProgressExpected.keySet()) {
                AbstractItem item = new AbstractItem(projectRepeator.newChildId());

                projectRepeator.add(item);
                item.add(new Label("project", project));
                item.add(new ProgressBar("projectProgress", new ProgressionModel()
                {
                    @Override
                    protected Progression getProgression()
                    {
                        double value =  (double) projectsProgressActual.get(project) / projectsProgressExpected
                                .get(project);
                        return new Progression(
                                (int)(value*100));
                    }
                }));
            }

        }
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

    private ChartImageResource createProgressChart(Map<String, Integer> barValues)
    {

        // fill dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String chartValue : barValues.keySet()) {
            dataset.setValue(barValues.get(chartValue), "Completeness", chartValue);
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setInsets(new RectangleInsets(UnitType.ABSOLUTE, 0, 20, 0, 20));
        plot.getRangeAxis().setRange(0.0, 1.0);
        ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0%"));
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setGradientPaintTransformer(new StandardGradientPaintTransformer(
                GradientPaintTransformType.HORIZONTAL));
        renderer.setSeriesPaint(0, new GradientPaint(0f, 0f, Color.RED, 0f, 0f, Color.GREEN));
        chart.getCategoryPlot().setRenderer(renderer);

        return new ChartImageResource(chart, CHART_WIDTH, 30 + (100 * 18));
    }

    private class ProjectProgressPanel
        extends Panel
    {
        private static final long serialVersionUID = -8668945427924328076L;
        private CheckBoxMultipleChoice<User> users;

        public ProjectProgressPanel(String id)
        {
            super(id);
            // TODO
        }
    }

    private class DocumentProgressPanel
        extends Panel
    {
        private static final long serialVersionUID = 2116717853865353733L;
        private ArrayList<String> documents = new ArrayList<String>();
        private ArrayList<String> selectedDocuments = new ArrayList<String>();

        private List<FileUpload> uploadedFiles;
        private FileUploadField fileUpload;

        private ArrayList<String> readableFormats;
        private String selectedFormat;

        private DropDownChoice<String> readableFormatsChoice;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public DocumentProgressPanel(String id)
        {
            super(id);
            // TODO
        }
    };


    private class KeppaStaticsPanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;
    private ArrayList<String> documents = new ArrayList<String>();
    private ArrayList<String> selectedDocuments = new ArrayList<String>();

    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    private ArrayList<String> readableFormats;
    private String selectedFormat;

    private DropDownChoice<String> readableFormatsChoice;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public KeppaStaticsPanel(String id)
    {
        super(id);
        // TODO
    }
    };
}
