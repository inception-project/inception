/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.monitoring.page;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.UnitType;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.statistics.TwoPairedKappa;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.DynamicColumnMetaData;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.TableDataProvider;
import de.tudarmstadt.ukp.clarin.webanno.project.page.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

/**
 * A Page To display different monitoring and statistics measurements tabularly and graphically.
 *
 * @author Seid Muhie Yimam
 *
 */
public class MonitoringPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final int CHART_WIDTH = 300;

    /**
     * The user column in the user-document status table
     */
    public static final String USER = "user:";

    /**
     * The document column in the user-document status table
     */
    public static final String DOCUMENT = "document:";

    public static final String CURATION = "curation";

    public static final String LAST_ACCESS = "last access:";
    public static final String LAST_ACCESS_ROW = "last access";

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private final ProjectSelectionForm projectSelectionForm;
    private final MonitoringDetailForm monitoringDetailForm;
    private final Image annotatorsProgressImage;
    private final Image annotatorsProgressPercentageImage;
    private final Image overallProjectProgressImage;
    private final TrainingResultForm trainingResultForm;

    private Label overview;
    private DefaultDataTable<?> annotationDocumentStatusTable;
    private DefaultDataTable<?> agreementTable;
    private final Label projectName;
    private AgreementForm agreementForm;
    private final AnnotationTypeSelectionForm annotationTypeSelectionForm;
    private ListChoice<TagSet> tagSets;
    private transient Map<SourceDocument, Map<User, JCas>> documentJCases;

    private String result;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MonitoringPage()
        throws UIMAException, IOException, ClassNotFoundException
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");

        agreementForm = new AgreementForm("agreementForm", new Model<AnnotationType>(),
                new Model<Project>());
        agreementForm.setVisible(false);
        add(agreementForm);

        trainingResultForm = new TrainingResultForm("trainingResultForm");
        trainingResultForm.setVisible(false);
        add(trainingResultForm);

        annotationTypeSelectionForm = new AnnotationTypeSelectionForm("annotationTypeSelectionForm");
        annotationTypeSelectionForm.setVisible(false);
        add(annotationTypeSelectionForm);

        annotatorsProgressImage = new NonCachingImage("annotator");
        annotatorsProgressImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressImage.setVisible(false);

        annotatorsProgressPercentageImage = new NonCachingImage("annotatorPercentage");
        annotatorsProgressPercentageImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressPercentageImage.setVisible(false);

        overallProjectProgressImage = new NonCachingImage("overallProjectProgressImage");
        final Map<String, Integer> overallProjectProgress = getOverallProjectProgress();
        overallProjectProgressImage.setImageResource(createProgressChart(overallProjectProgress,
                100, true));
        overallProjectProgressImage.setOutputMarkupPlaceholderTag(true);
        overallProjectProgressImage.setVisible(true);
        add(overallProjectProgressImage);
        add(overview = new Label("overview", "overview of projects"));

        add(projectSelectionForm);
        projectName = new Label("projectName", "");

        Project project = repository.listProjects().get(0);

        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
        List<SourceDocument> dc = repository.listSourceDocuments(project);
        for (int j = 0; j < repository.listProjectUsersWithPermissions(project).size(); j++) {
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
            cols.add(new DocumentStatusColumnMetaData(prov, i, new Project(), repository));
        }
        annotationDocumentStatusTable = new DefaultDataTable("rsTable", cols, prov, 2);
        monitoringDetailForm.setVisible(false);
        add(monitoringDetailForm.add(annotatorsProgressImage)
                .add(annotatorsProgressPercentageImage).add(projectName)
                .add(annotationDocumentStatusTable));
        annotationDocumentStatusTable.setVisible(false);
    }

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
                            User user = repository.getUser(username);

                            List<Project> allProjects = repository.listProjects();
                            List<Authority> authorities = repository.listAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ProjectUtil.isProjectAdmin(project, repository, user)
                                        || ProjectUtil.isCurator(project, repository, user)) {
                                    allowedProject.add(project);
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    List<User> users = repository.listProjectUsersWithPermissions(aNewSelection,
                            PermissionLevel.USER);
                    List<SourceDocument> sourceDocuments = repository
                            .listSourceDocuments(aNewSelection);
                    documentJCases = getJCases(users, sourceDocuments);

                    if (aNewSelection == null) {
                        return;
                    }

                    monitoringDetailForm.setModelObject(aNewSelection);
                    monitoringDetailForm.setVisible(true);
                    annotationTypeSelectionForm.setVisible(true);
                    monitoringDetailForm.setVisible(true);

                    if (aNewSelection.getMode().equals(Mode.AUTOMATION)) {
                        trainingResultForm.setVisible(true);
                    }
                    else {
                        trainingResultForm.setVisible(false);
                    }
                    result = "";

                    annotationTypeSelectionForm.setModelObject(new SelectionModel());
                    updateAgreementForm();

                    ProjectSelectionForm.this.setVisible(true);

                    final Map<String, Integer> annotatorsProgress = new TreeMap<String, Integer>();
                    final Map<String, Integer> annotatorsProgressInPercent = new TreeMap<String, Integer>();
                    final Project project = aNewSelection;
                    List<SourceDocument> documents = repository.listSourceDocuments(project);

                    final int totalDocuments = documents.size();

                    // Annotator's Progress
                    if (project != null) {
                        annotatorsProgressInPercent
                                .putAll(getPercentageOfFinishedDocumentsPerUser(project));
                        annotatorsProgress.putAll(getFinishedDocumentsPerUser(project));

                    }
                    projectName.setDefaultModelObject(project.getName());
                    overallProjectProgressImage.setVisible(false);
                    overview.setVisible(false);

                    annotatorsProgressImage.setImageResource(createProgressChart(
                            annotatorsProgress, totalDocuments, false));
                    annotatorsProgressImage.setVisible(true);

                    annotatorsProgressPercentageImage.setImageResource(createProgressChart(
                            annotatorsProgressInPercent, 100, true));
                    annotatorsProgressPercentageImage.setVisible(true);

                    List<String> documentListAsColumnHeader = new ArrayList<String>();
                    documentListAsColumnHeader.add("Documents");

                    // A column for curation user annotation document status
                    documentListAsColumnHeader.add(CURATION);

                    // List of users with USER permission level
                    List<User> usersWithPermissions = repository.listProjectUsersWithPermissions(
                            project, PermissionLevel.USER);

                    for (User user : usersWithPermissions) {
                        documentListAsColumnHeader.add(user.getUsername());
                    }

                    List<List<String>> userAnnotationDocumentStatusList = new ArrayList<List<String>>();

                    // Add a timestamp row for every user.
                    List<String> projectTimeStamp = new ArrayList<String>();
                    projectTimeStamp.add(LAST_ACCESS + LAST_ACCESS_ROW); // first column
                    if (repository.existsProjectTimeStamp(aNewSelection)) {
                        projectTimeStamp.add(LAST_ACCESS
                                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(repository
                                        .getProjectTimeStamp(aNewSelection)));
                    }
                    else {
                        projectTimeStamp.add(LAST_ACCESS + "__");
                    }

                    for (User user : usersWithPermissions) {
                        if (repository.existsProjectTimeStamp(project, user.getUsername())) {
                            projectTimeStamp.add(LAST_ACCESS
                                    + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(repository
                                            .getProjectTimeStamp(project, user.getUsername())));
                        }
                        else {
                            projectTimeStamp.add(LAST_ACCESS + "__");
                        }
                    }

                    userAnnotationDocumentStatusList.add(projectTimeStamp);

                    for (SourceDocument document : documents) {
                        List<String> userAnnotationDocuments = new ArrayList<String>();
                        userAnnotationDocuments.add(DOCUMENT + document.getName());

                        // Curation Document status
                        userAnnotationDocuments.add(CurationPanel.CURATION_USER + "-" + DOCUMENT
                                + document.getName());

                        for (User user : usersWithPermissions) {
                            // annotation document status for this annotator
                            userAnnotationDocuments.add(user.getUsername() + "-" + DOCUMENT
                                    + document.getName());
                        }

                        userAnnotationDocumentStatusList.add(userAnnotationDocuments);
                    }

                    TableDataProvider provider = new TableDataProvider(documentListAsColumnHeader,
                            userAnnotationDocumentStatusList);

                    List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

                    for (int i = 0; i < provider.getColumnCount(); i++) {
                        columns.add(new DocumentStatusColumnMetaData(provider, i, project,
                                repository));
                    }
                    annotationDocumentStatusTable.remove();
                    annotationDocumentStatusTable = new DefaultDataTable("rsTable", columns,
                            provider, 20);
                    annotationDocumentStatusTable.add(new AjaxEventBehavior("onclick")
                    {
                        private static final long serialVersionUID = -4468099385971446135L;

                        @Override
                        protected void onEvent(AjaxRequestTarget aTarget)
                        {
                            annotatorsProgress.clear();
                            annotatorsProgress.putAll(getFinishedDocumentsPerUser(project));
                            annotatorsProgressImage.setImageResource(createProgressChart(
                                    annotatorsProgress, totalDocuments, false));
                            aTarget.add(annotatorsProgressImage.setOutputMarkupId(true));

                            annotatorsProgressInPercent.clear();
                            annotatorsProgressInPercent
                                    .putAll(getPercentageOfFinishedDocumentsPerUser(project));
                            annotatorsProgressPercentageImage.setImageResource(createProgressChart(
                                    annotatorsProgressInPercent, 100, true));
                            aTarget.add(annotatorsProgressPercentageImage.setOutputMarkupId(true));

                            aTarget.add(monitoringDetailForm.setOutputMarkupId(true));
                            updateAgreementTable(aTarget);
                            aTarget.add(agreementForm.setOutputMarkupId(true));

                        }
                    });
                    monitoringDetailForm.add(annotationDocumentStatusTable);
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

        public AnnotationTypeSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(tagSets = new ListChoice<TagSet>("tagSets")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<TagSet> load()
                        {
                            return annotationService.listTagSets(projectSelectionForm
                                    .getModelObject().project);
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<TagSet>("name", "id"));
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            tagSets.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    updateAgreementTable(aTarget);
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

    private Map<String, Integer> getFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
        if (aProject != null) {
            for (User user : repository.listProjectUsersWithPermissions(aProject)) {
                for (SourceDocument document : repository.listSourceDocuments(aProject)) {
                    if (repository.isAnnotationFinished(document, user)) {
                        if (annotatorsProgress.get(user.getUsername()) == null) {
                            annotatorsProgress.put(user.getUsername(), 1);
                        }
                        else {
                            int previousValue = annotatorsProgress.get(user.getUsername());
                            annotatorsProgress.put(user.getUsername(), previousValue + 1);
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

    private Map<String, Integer> getPercentageOfFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
        if (aProject != null) {
            for (User user : repository.listProjectUsersWithPermissions(aProject)) {
                int finished = 0;
                int ignored = 0;
                int totalDocs = 0;
                for (SourceDocument document : repository.listSourceDocuments(aProject)) {
                    totalDocs++;
                    if (repository.isAnnotationFinished(document, user)) {
                        finished++;
                    }
                    else if (repository.existsAnnotationDocument(document, user)) {
                        AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                                document, user);
                        if (annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {
                            ignored++;
                        }
                    }
                }
                annotatorsProgress.put(user.getUsername(),
                        (int) Math.round((double) (finished * 100) / (totalDocs - ignored)));
            }
        }
        return annotatorsProgress;
    }

    private Map<String, Integer> getOverallProjectProgress()
    {
        Map<String, Integer> overallProjectProgress = new LinkedHashMap<String, Integer>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = repository.getUser(username);
        for (Project project : repository.listProjects()) {
            if (ProjectUtil.isCurator(project, repository, user)
                    || ProjectUtil.isProjectAdmin(project, repository, user)) {
                int annoFinished = repository.listFinishedAnnotationDocuments(project).size();
                int allAnno = repository.numberOfExpectedAnnotationDocuments(project);
                int progress = (int) Math.round((double) (annoFinished * 100) / (allAnno));
                overallProjectProgress.put(project.getName(), progress);
            }
        }
        return overallProjectProgress;
    }

    static public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        public Project project;
        public TagSet tagSets;
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

        }
    }

    @SuppressWarnings("rawtypes")
    private class AgreementForm
        extends Form<DefaultDataTable>
    {
        private static final long serialVersionUID = 344165080600348157L;

        @SuppressWarnings({ "unchecked" })
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
                columns.add(new DynamicColumnMetaData(provider, m));
            }
            add(agreementTable = new DefaultDataTable("agreementTable", columns, provider, 10));
        }
    }

    private void updateAgreementForm()
    {
        agreementForm.remove();
        agreementForm = new AgreementForm("agreementForm", new Model<AnnotationType>(),
                new Model<Project>());
        add(agreementForm);
        agreementForm.setVisible(true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateAgreementTable(AjaxRequestTarget aTarget)
    {

        Project project = projectSelectionForm.getModelObject().project;
        List<User> users = repository
                .listProjectUsersWithPermissions(project, PermissionLevel.USER);
        double[][] results = new double[users.size()][users.size()];
        if (tagSets.getModelObject() != null
                && !tagSets.getModelObject().getLayer().getName()
                        .equals(WebAnnoConst.COREFERENCE)) {

            TypeAdapter adapter = TypeUtil.getAdapter(tagSets.getModelObject(), annotationService);

            // assume all users finished only one document
            double[][] multipleDocumentsFinished = new double[users.size()][users.size()];
            for (int m = 0; m < users.size(); m++) {
                for (int j = 0; j < users.size(); j++) {
                    multipleDocumentsFinished[m][j] = 1.0;
                }
            }

            List<SourceDocument> sourceDocuments = repository.listSourceDocuments(project);

            // a map that contains list of finished annotation documents for a given user
            Map<User, List<SourceDocument>> finishedDocumentLists = new HashMap<User, List<SourceDocument>>();
            for (User user : users) {
                List<SourceDocument> finishedDocuments = new ArrayList<SourceDocument>();
                for (SourceDocument document : sourceDocuments) {
                    AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                            document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        finishedDocuments.add(document);
                    }
                }
                finishedDocumentLists.put(user, finishedDocuments);
            }

            results = computeKappa(users, adapter, finishedDocumentLists, documentJCases);

            // Users with some annotations of this type

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
                    if (j < i) {
                        agreementResult.add("");
                    }
                    else {
                        agreementResult.add((double) Math.round(results[i][j] * 100) / 100 + "");
                    }
                }
                i++;
                agreementResults.add(agreementResult);
            }

            TableDataProvider provider = new TableDataProvider(usersListAsColumnHeader,
                    agreementResults);

            List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

            for (int m = 0; m < provider.getColumnCount(); m++) {
                columns.add(new DynamicColumnMetaData(provider, m));
            }
            agreementTable.remove();
            agreementTable = new DefaultDataTable("agreementTable", columns, provider, 10);
            agreementForm.add(agreementTable);
            aTarget.add(agreementForm);
        }
    }

    private class TrainingResultForm
        extends Form<ResultMOdel>
    {
        private static final long serialVersionUID = 1037668483966897381L;

        ListChoice<MiraTemplate> resultChoice;
        Label resultLabel;

        public TrainingResultForm(String id)
        {
            super(id, new CompoundPropertyModel<ResultMOdel>(new ResultMOdel()));

            add(resultLabel = (Label) new Label("resultLabel",
                    new LoadableDetachableModel<String>()
                    {
                        private static final long serialVersionUID = 891566759811286173L;

                        @Override
                        protected String load()
                        {
                            return result;

                        }
                    }).setOutputMarkupId(true));

            add(resultChoice = new ListChoice<MiraTemplate>("layerResult")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<MiraTemplate>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<MiraTemplate> load()
                        {

                            return repository.listMiraTemplates(projectSelectionForm
                                    .getModelObject().project);

                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<MiraTemplate>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(MiraTemplate aObject)
                        {
                            return "[" + aObject.getTrainTagSet().getLayer().getName() + "] "
                                    + aObject.getTrainTagSet().getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            resultChoice.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    result = getModelObject().layerResult.getResult();
                    aTarget.add(resultLabel);
                }
            }).setOutputMarkupId(true).setOutputMarkupId(true);
        }

    }

    public class ResultMOdel
        implements Serializable
    {
        private static final long serialVersionUID = 3611186385198494181L;
        public MiraTemplate layerResult;

    }

    /**
     * Compute kappa using the {@link TwoRaterKappaAgreement}. The matrix of kappa result is
     * computed for a user against every other users if and only if both users have finished the
     * same document <br>
     * The result is per {@link AnnotationType} for all {@link Tag}s
     */

    public static double[][] computeKappa(List<User> users, TypeAdapter adapter,
            Map<User, List<SourceDocument>> finishedDocumentLists,
            Map<SourceDocument, Map<User, JCas>> documentJCases)
    {
        double[][] results = new double[users.size()][users.size()];
        TwoPairedKappa twoPairedKappa = new TwoPairedKappa();
        int userInRow = 0;
        List<User> rowUsers = new ArrayList<User>();
        for (User user1 : users) {
            if (finishedDocumentLists.get(user1).size() == 0) {
                userInRow++;
                continue;
            }
            int userInColumn = 0;
            for (User user2 : users) {
                if (user1.getUsername().equals(user2.getUsername())) {// no need to
                                                                      // compute
                    // with itself, diagonal one
                    results[userInRow][userInColumn] = 1.0;
                    userInColumn++;
                    continue;
                }
                else if (rowUsers.contains(user2)) {// already done, upper part of
                                                    // matrix
                    userInColumn++;
                    continue;
                }

                Map<String, Map<String, String>> allUserAnnotations = new TreeMap<String, Map<String, String>>();

                if (finishedDocumentLists.get(user2).size() != 0) {
                    List<SourceDocument> user1Documents = new ArrayList<SourceDocument>();
                    user1Documents.addAll(finishedDocumentLists.get(user1));

                    List<SourceDocument> user2Documents = new ArrayList<SourceDocument>();
                    user2Documents.addAll(finishedDocumentLists.get(user2));

                    // sameDocuments finished (intersection of anno docs)
                    user1Documents.retainAll(user2Documents);
                    for (SourceDocument document : user1Documents) {
                        twoPairedKappa.getStudy(adapter.getAnnotationTypeName(),
                                adapter.getLabelFeatureName(), user1, user2, allUserAnnotations,
                                document, documentJCases.get(document));

                    }
                }

                if (twoPairedKappa.getAgreement(allUserAnnotations).length != 0) {
                    double[][] thisResults = twoPairedKappa.getAgreement(allUserAnnotations);
                    // for a user with itself, we have
                    // u1
                    // u1 1.0
                    // we took it as it is
                    if (allUserAnnotations.keySet().size() == 1) {
                        results[userInRow][userInColumn] = (double) Math
                                .round(thisResults[0][0] * 100) / 100;
                    }
                    else {
                        // in result for the two users will be in the form of
                        // u1 u2
                        // --------------
                        // u1 1.0 0.84
                        // u2 0.84 1.0
                        // only value from first row, second column is important
                        results[userInRow][userInColumn] = (double) Math
                                .round(thisResults[0][1] * 100) / 100;
                    }
                    rowUsers.add(user1);
                }
                userInColumn++;
            }
            userInRow++;
        }
        return results;
    }

    /**
     * Get all Cases that is not either new or ignore per user, per document. we need those in
     * progress if the admin tries to finish it from the monitoring page
     */
    private Map<SourceDocument, Map<User, JCas>> getJCases(List<User> users,
            List<SourceDocument> sourceDocuments)
    {
        // Store Jcases so that we can re-use for different iterations
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        for (SourceDocument document : sourceDocuments) {
            Map<User, JCas> jCases = new HashMap<User, JCas>();
            for (User user : users) {
                if (repository.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                            document, user);
                    if (!(annotationDocument.getState().equals(AnnotationDocumentState.IGNORE) || annotationDocument
                            .getState().equals(AnnotationDocumentState.NEW))) {
                        try {
                            JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
                            jCases.put(user, jCas);
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (DataRetrievalFailureException e) {
                            error(e.getCause().getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }

                    }
                }
            }
            documentJCases.put(document, jCases);
        }
        return documentJCases;
    }

    private ChartImageResource createProgressChart(Map<String, Integer> chartValues, int aMaxValue,
            boolean aIsPercentage)
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
        // For documents lessan 10, avoid repeating the number of documents such as 0 0 1 1 1
        // NumberTickUnit automatically determin the range
        if (!aIsPercentage && aMaxValue <= 10) {
            TickUnits standardUnits = new TickUnits();
            NumberAxis tick = new NumberAxis();
            tick.setTickUnit(new NumberTickUnit(1));
            standardUnits.add(tick.getTickUnit());
            plot.getRangeAxis().setStandardTickUnits(standardUnits);
        }
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
