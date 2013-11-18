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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.statistics.TwoPairedKappa;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.DynamicColumnMetaData;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.TableDataProvider;
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
    private RepositoryService projectRepository;

    private final ProjectSelectionForm projectSelectionForm;
    private final MonitoringDetailForm monitoringDetailForm;
    private final Image annotatorsProgressImage;
    private final Image annotatorsProgressPercentageImage;
    private final Image overallProjectProgressImage;
    private DefaultDataTable<?> annotationDocumentStatusTable;
    private DefaultDataTable<?> agreementTable;
    private final Label projectName;
    private AgreementForm agreementForm;
    private final AnnotationTypeSelectionForm annotationTypeSelectionForm;
    private ListChoice<AnnotationType> annotationTypes;
    private transient Map<SourceDocument, Map<User, JCas>> documentJCases;

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
        overallProjectProgressImage.setOutputMarkupPlaceholderTag(true);
        overallProjectProgressImage.setVisible(false);

        add(projectSelectionForm);
        projectName = new Label("projectName", "");

        Project project = projectRepository.listProjects().get(0);

        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
        List<SourceDocument> dc = projectRepository.listSourceDocuments(project);
        for (int j = 0; j < projectRepository.listProjectUsersWithPermissions(project).size(); j++) {
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
            cols.add(new DocumentStatusColumnMetaData(prov, i, new Project(), projectRepository));
        }
        annotationDocumentStatusTable = new DefaultDataTable("rsTable", cols, prov, 2);
        monitoringDetailForm.setVisible(false);
        add(monitoringDetailForm.add(annotatorsProgressImage)
                .add(annotatorsProgressPercentageImage).add(projectName)
                .add(annotationDocumentStatusTable).add(overallProjectProgressImage));
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
                            User user = projectRepository.getUser(username);

                            List<Project> allProjects = projectRepository.listProjects();
                            List<Authority> authorities = projectRepository.listAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ProjectUtil.isProjectAdmin(project, projectRepository, user)
                                        || ProjectUtil.isCurator(project, projectRepository, user)) {
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
                    List<User> users = projectRepository.listProjectUsersWithPermissions(
                            aNewSelection, PermissionLevel.USER);
                    List<SourceDocument> sourceDocuments = projectRepository
                            .listSourceDocuments(aNewSelection);
                    documentJCases = getJCases(users, sourceDocuments);

                    if (aNewSelection == null) {
                        return;
                    }

                    monitoringDetailForm.setModelObject(aNewSelection);
                    monitoringDetailForm.setVisible(true);
                    annotationTypeSelectionForm.setVisible(true);
                    monitoringDetailForm.setVisible(true);

                    annotationTypeSelectionForm.setModelObject(new SelectionModel());
                    updateAgreementForm();

                    ProjectSelectionForm.this.setVisible(true);

                    final Map<String, Integer> annotatorsProgress = new TreeMap<String, Integer>();
                    final Map<String, Integer> annotatorsProgressInPercent = new TreeMap<String, Integer>();
                    final Map<String, Integer> overallProjectProgress = new TreeMap<String, Integer>();
                    final Project project = aNewSelection;
                    List<SourceDocument> documents = projectRepository.listSourceDocuments(project);

                    final int totalDocuments = documents.size();

                    // Annotator's Progress
                    if (project != null) {
                        annotatorsProgressInPercent
                                .putAll(getPercentageOfFinishedDocumentsPerUser(project));
                        annotatorsProgress.putAll(getFinishedDocumentsPerUser(project));

                    }
                    projectName.setDefaultModelObject(project.getName());

                    overallProjectProgress.putAll(getOverallProjectProgress());
                    overallProjectProgressImage.setImageResource(createProgressChart(
                            overallProjectProgress, 100, true));
                    overallProjectProgressImage.setVisible(true);

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
                    List<User> usersWithPermissions = projectRepository
                            .listProjectUsersWithPermissions(project, PermissionLevel.USER);

                    for (User user : usersWithPermissions) {
                        documentListAsColumnHeader.add(user.getUsername());
                    }

                    List<List<String>> userAnnotationDocumentStatusList = new ArrayList<List<String>>();

                    // Add a timestamp row for every user.
                    List<String> projectTimeStamp = new ArrayList<String>();
                    projectTimeStamp.add(LAST_ACCESS + LAST_ACCESS_ROW); // first column
                    if (projectRepository.existsProjectTimeStamp(aNewSelection)) {
                        projectTimeStamp.add(LAST_ACCESS
                                + projectRepository.getProjectTimeStamp(aNewSelection));
                    }
                    else {
                        projectTimeStamp.add(LAST_ACCESS + "__");
                    }

                    for (User user : usersWithPermissions) {
                        if (projectRepository.existsProjectTimeStamp(project, user.getUsername())) {
                            projectTimeStamp.add(LAST_ACCESS
                                    + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                            .format(projectRepository.getProjectTimeStamp(project,
                                                    user.getUsername())));
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
                                projectRepository));
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

                            overallProjectProgress.clear();
                            overallProjectProgress.putAll(getOverallProjectProgress());
                            overallProjectProgressImage.setImageResource(createProgressChart(
                                    overallProjectProgress, 100, true));
                            aTarget.add(overallProjectProgressImage.setOutputMarkupId(true));

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
                            return annotationService.listAnnotationType(projectSelectionForm
                                    .getModelObject().project);
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
            for (User user : projectRepository.listProjectUsersWithPermissions(aProject)) {
                for (SourceDocument document : projectRepository.listSourceDocuments(aProject)) {
                    if (projectRepository.isAnnotationFinished(document, user)) {
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
            for (User user : projectRepository.listProjectUsersWithPermissions(aProject)) {
                int finished = 0;
                int ignored = 0;
                int totalDocs = 0;
                for (SourceDocument document : projectRepository.listSourceDocuments(aProject)) {
                    totalDocs++;
                    if (projectRepository.isAnnotationFinished(document, user)) {
                        finished++;
                    }
                    else if (projectRepository.existsAnnotationDocument(document, user)) {
                        AnnotationDocument annotationDocument = projectRepository
                                .getAnnotationDocument(document, user);
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
        Map<String, Integer> overallProjectProgress = new HashMap<String, Integer>();
        for (Project project : projectRepository.listProjects()) {
            int annoFinished = projectRepository.listFinishedAnnotationDocuments(project).size();
            int allAnno = projectRepository.numberOfExpectedAnnotationDocuments(project);
            int progress = (int) Math.round((double) (annoFinished * 100) / (allAnno));
            overallProjectProgress.put(project.getName(), progress);
        }
        return overallProjectProgress;
    }

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        @SuppressWarnings("unused")
        private AnnotationType annotationTypes;
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
        agreementForm.setVisible(false);
        add(agreementForm);
        agreementForm.setVisible(true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateAgreementTable(AjaxRequestTarget aTarget)
    {

        Project project = projectSelectionForm.getModelObject().project;
        List<User> users = projectRepository.listProjectUsersWithPermissions(project,
                PermissionLevel.USER);
        double[][] results = new double[users.size()][users.size()];
        if (annotationTypes.getModelObject() != null
                && !annotationTypes.getModelObject().getName()
                        .equals(AnnotationTypeConstant.COREFERENCE)) {

            TypeAdapter adapter = TypeUtil.getAdapter(annotationTypes.getModelObject());

            // assume all users finished only one document
            double[][] multipleDocumentsFinished = new double[users.size()][users.size()];
            for (int m = 0; m < users.size(); m++) {
                for (int j = 0; j < users.size(); j++) {
                    multipleDocumentsFinished[m][j] = 1.0;
                }
            }

            List<SourceDocument> sourceDocuments = projectRepository.listSourceDocuments(project);

            // a map that contains list of finished annotation documents for a given user
            Map<User, List<SourceDocument>> finishedDocumentLists = new HashMap<User, List<SourceDocument>>();
            for (User user : users) {
                List<SourceDocument> finishedDocuments = new ArrayList<SourceDocument>();
                for (SourceDocument document : sourceDocuments) {
                    AnnotationDocument annotationDocument = projectRepository
                            .getAnnotationDocument(document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        finishedDocuments.add(document);
                    }
                }
                finishedDocumentLists.put(user, finishedDocuments);
            }

            results = computeKappa(project, users, adapter, finishedDocumentLists);
        }

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

    /**
     * Compute kappa using the {@link TwoRaterKappaAgreement}. The matrix of kappa result is
     * computed for a user against every other users if and only if both users have finished the
     * same document <br>
     * The result is per {@link AnnotationType} for all {@link Tag}s
     */

    private double[][] computeKappa(Project project, List<User> users, TypeAdapter adapter,
            Map<User, List<SourceDocument>> finishedDocumentLists)
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
                    List<SourceDocument> sameDocuments = finishedDocumentLists.get(user1);
                    // sameDocuments finished (intersection of anno docs)
                    sameDocuments.retainAll(finishedDocumentLists.get(user2));
                    for (SourceDocument document : sameDocuments) {
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
                if (projectRepository.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = projectRepository
                            .getAnnotationDocument(document, user);
                    if (!(annotationDocument.getState().equals(AnnotationDocumentState.IGNORE) || annotationDocument
                            .getState().equals(AnnotationDocumentState.NEW))) {
                        try {
                            JCas jCas = projectRepository
                                    .getAnnotationDocumentContent(annotationDocument);
                            jCases.put(user, jCas);
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
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
