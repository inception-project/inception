/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.crowdsource;

import static org.uimafit.util.JCasUtil.select;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonProcessingException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.crowdflower.NamedEntityTaskManager;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EmbeddableImage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.TableDataProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Crowdsource page used to setup and monitor crowd source projects.
 *
 * @author Seid Muhie Yimam
 * @author Benjamin Milde
 *
 */
public class CrowdSourcePage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(CrowdSourcePage.class);

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private static final String CROWD_USER = "crowd_user";

    // TODO: explain how to export these templates from Crowdflower
    // & move to manager
    private static final String CROWD_NERTASK1_TEMPLATE = "NERtask1.template";
    private static final String CROWD_NERTASK2_TEMPLATE = "NERtask2.template";

    // TODO: rename
    private CrowdJob selectedCrowdJob;

    private Project selectedProject;

    boolean createCrowdJob = false;

    private ArrayList<SourceDocument> documents = new ArrayList<SourceDocument>();
    private ArrayList<SourceDocument> goldDocuments = new ArrayList<SourceDocument>();

    private NamedEntityTaskManager namedEntityTaskManager;

    private CrowdSourceForm crowdSourceForm;
    private CrowdJobForm crowdJobForm;
    private CrowdDocumentListForm crowdDocumentListForm;
    private GoldDocumentListForm goldDocumentListForm;
    private CrowdProjectDetailForm crowdJobDetailForm;
    private NumberTextField<Integer> useSentenceTextField;
    private NumberTextField<Integer> useGoldSentenceTextField;

    /**
     * Crowd source page, user interface to manage and upload Crowdjobs with WebAnno
     *
     */
    public CrowdSourcePage()
    {
        crowdSourceForm = new CrowdSourceForm("crowdSourceForm");
        add(crowdSourceForm);

        crowdJobForm = new CrowdJobForm("crowdJobForm");
        crowdJobForm.setVisible(false);
        add(crowdJobForm);

        crowdDocumentListForm = new CrowdDocumentListForm("crowdDocumentListForm");
        crowdDocumentListForm.setVisible(false);
        add(crowdDocumentListForm);

        goldDocumentListForm = new GoldDocumentListForm("goldDocumentListForm");
        goldDocumentListForm.setVisible(false);
        add(goldDocumentListForm);

        crowdJobDetailForm = new CrowdProjectDetailForm("crowdJobDetailForm");
        crowdJobDetailForm.setVisible(false);
        add(crowdJobDetailForm);
    }

    private class CrowdSourceForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = 1L;

        public CrowdSourceForm(String id)
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
                            List<Project> allProjects = repository.listProjects();
                            List<Project> crowdProjects = new ArrayList<Project>();
                            User user = userRepository.get(CROWD_USER);
                            for (Project project : allProjects) {
                                List<User> users = repository
                                        .listProjectUsersWithPermissions(project);
                                if (users.contains(user)) {
                                    crowdProjects.add(project);
                                }
                            }
                            return crowdProjects;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    selectedProject = aNewSelection;
                    crowdJobForm.setVisible(true);
                    crowdDocumentListForm.setVisible(false);
                    crowdJobDetailForm.setVisible(false);
                    updateTable();

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

    private class CrowdJobForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        @SuppressWarnings("unchecked")
        public CrowdJobForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            List<String> columnHeaders = new ArrayList<String>();
            columnHeaders.add("Document");
            columnHeaders.add("Status");
            columnHeaders.add("Task");
            columnHeaders.add("Edit Task");

            List<List<String>> rowData = new ArrayList<List<String>>();
            if (selectedProject != null) {
                List<CrowdJob> crowdJobs = repository.listCrowdJobs(selectedProject);
                // no Document is added yet
                for (CrowdJob crowdJob : crowdJobs) {
                    if (crowdJob.getDocuments().size() == 0) {
                        List<String> cellEntry = new ArrayList<String>();

                        cellEntry.add("__");
                        cellEntry.add("__");
                        cellEntry.add(crowdJob.getName());
                        cellEntry.add(crowdJob.getName());

                        rowData.add(cellEntry);
                    }
                    else {
                        for (SourceDocument sourceDocument : crowdJob.getDocuments()) {
                            List<String> cellEntry = new ArrayList<String>();
                            cellEntry.add(sourceDocument.getName());
                            cellEntry.add(sourceDocument.getState().getName());
                            cellEntry.add(crowdJob.getName());
                            cellEntry.add(crowdJob.getName());

                            rowData.add(cellEntry);
                        }
                    }
                }
            }
            TableDataProvider provider = new TableDataProvider(columnHeaders, rowData);
            List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

            for (int i = 0; i < provider.getColumnCount(); i++) {
                columns.add(new DocumentColumnMetaData(provider, i));
            }
            // TODO: look into what generic is used here
            add(new DefaultDataTable("crowdSourceInformationTable", columns, provider, 20));

            add(new Button("new", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    crowdJobDetailForm.setModelObject(new SelectionModel());
                    crowdJobDetailForm.setVisible(true);
                    createCrowdJob = true;
                    selectedCrowdJob = new CrowdJob();
                }
            });

        }
    }

    /**
     * Details of a crowdsource project
     *
     */

    private class CrowdProjectDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Returns jCases for source documents for the selected Crowdjob
         *
         * @param user
         * @return
         */

        private List<JCas> getSourceDocumentsJCases(User user)
        {
            // Get the source document(2) here
            List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>(
                    selectedCrowdJob.getDocuments());
            List<JCas> jCases = new ArrayList<JCas>();

            // Get the JCASes for each source document
            for (SourceDocument sourceDocument : sourceDocuments) {
                addJCas(jCases, user, sourceDocument);
            }
            return jCases;
        }

        /**
         * Returns jCases for gold documents for the selected crowdjob
         *
         * @param user
         *            - user for the jcases
         * @return list of jCases
         */

        private List<JCas> getGoldDocumentsJCases(User user)
        {
            List<JCas> goldJCases = new ArrayList<JCas>();
            for (SourceDocument sourceDocument : goldDocuments) {
                addJCas(goldJCases, user, sourceDocument);
            }
            return goldJCases;
        }

        /**
         *
         * @return - the number of sentences of all currently selected documents
         */
        private int getSourceDocumentsSentCount()
        {
            User user = userRepository.get(CROWD_USER);
            List<JCas> jCases = getSourceDocumentsJCases(user);

            int numSents = 0;

            for (JCas cas : jCases) {
                numSents += select(cas, Sentence.class).size();
            }

            return numSents;
        }

        /**
         *
         * @return - the number of sentences of all currently selected documents
         */
        private int getGoldDocumentsSentCount()
        {
            User user = userRepository.get(CROWD_USER);
            List<JCas> jCases = getGoldDocumentsJCases(user);

            int numSents = 0;

            for (JCas cas : jCases) {
                numSents += select(cas, Sentence.class).size();
            }

            return numSents;
        }

        public CrowdProjectDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new TextField<String>("name").setRequired(true));

            add(new TextField<String>("apiKey"));

            useSentenceTextField = new NumberTextField<Integer>("useSents",
                    new LoadableDetachableModel<Integer>()
                    {

                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Integer load()
                        {
                            return selectedCrowdJob == null ? -1 : selectedCrowdJob.getUseSents();
                        }
                    });
            useSentenceTextField.setType(Integer.class);
            add(useSentenceTextField);

            useGoldSentenceTextField = new NumberTextField<Integer>("useGoldSents",
                    new LoadableDetachableModel<Integer>()
                    {

                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Integer load()
                        {
                            return selectedCrowdJob == null ? -1
                                    : selectedCrowdJob.getUseGoldSents();
                        }
                    });
            useGoldSentenceTextField.setType(Integer.class);
            add(useGoldSentenceTextField);

            ExternalLink link1;
            ExternalLink link2;

            IModel<String> link1_model = new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = -2140663269255140643L;

                @Override
                protected String load()
                {
                    if (selectedCrowdJob != null) {
                        return selectedCrowdJob.getLink1();
                    }
                    else {
                        return "";
                    }
                }
            };

            IModel<String> link2_model = new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = -2140663269255140643L;

                @Override
                protected String load()
                {
                    if (selectedCrowdJob != null) {
                        return selectedCrowdJob.getLink2();
                    }
                    else {
                        return "";
                    }
                }
            };

            add(link1 = new ExternalLink("link1", link1_model));
            add(link2 = new ExternalLink("link2", link2_model));

            link1.add(new Label("label1", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    return selectedCrowdJob.getLink1();
                }
            }));

            link2.add(new Label("label2", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    return selectedCrowdJob.getLink2();
                }
            }));

            add(new Label("status", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    return selectedCrowdJob.getStatus();
                }
            }));

            add(new Label("availSentences", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    return String.valueOf(getSourceDocumentsSentCount());
                }
            }));

            add(new Label("availGoldSentences", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load()
                {
                    return String.valueOf(getGoldDocumentsSentCount());
                }
            }));

            // TODO: more comments
            add(new ListMultipleChoice<SourceDocument>("documents")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<SourceDocument> load()
                        {

                            if (selectedCrowdJob != null) {
                                documents = new ArrayList<SourceDocument>(selectedCrowdJob
                                        .getDocuments());
                                Collections.sort(documents, new Comparator<SourceDocument>()
                                {
                                    @Override
                                    public int compare(SourceDocument doc1, SourceDocument doc2)
                                    {
                                        return (doc1.getProject().getName() + doc1.getName())
                                                .compareTo(doc2.getProject().getName()
                                                        + doc2.getName());
                                    }
                                });
                            }
                            return documents;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<SourceDocument>()
                    {
                        private static final long serialVersionUID = 4332411944787665963L;

                        @Override
                        public Object getDisplayValue(SourceDocument aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            }).setOutputMarkupId(true);

            add(new ListMultipleChoice<SourceDocument>("goldDocuments")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<SourceDocument> load()
                        {

                            if (selectedCrowdJob != null) {
                                goldDocuments = new ArrayList<SourceDocument>(selectedCrowdJob
                                        .getGoldDocuments());
                                Collections.sort(goldDocuments, new Comparator<SourceDocument>()
                                {
                                    @Override
                                    public int compare(SourceDocument doc1, SourceDocument doc2)
                                    {
                                        return (doc1.getProject().getName() + doc1.getName())
                                                .compareTo(doc2.getProject().getName()
                                                        + doc2.getName());
                                    }
                                });
                            }

                            return goldDocuments;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<SourceDocument>()
                    {
                        private static final long serialVersionUID = 4332411944787665963L;

                        @Override
                        public Object getDisplayValue(SourceDocument aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            }).setOutputMarkupId(true);

            // save the crowd task
            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    String name = crowdJobDetailForm.getModelObject().name;
                    // rename task
                    if (selectedCrowdJob != null && !repository.existsCrowdJob(name)) {
                        selectedCrowdJob.setName(name);
                        selectedCrowdJob.setProject(selectedProject);
                        selectedCrowdJob.setApiKey(crowdJobDetailForm.getModelObject().apiKey);
                        try {
                            repository.createCrowdJob(selectedCrowdJob);
                        }
                        catch (IOException e) {
                            error("Problem Processing Log file");
                        }
                        createCrowdJob = false;
                        updateTable();

                    }
                    // save new task
                    else if (!repository.existsCrowdJob(name)) {
                        CrowdJob crowdJob = new CrowdJob();
                        crowdJob.setName(name);
                        selectedCrowdJob.setProject(selectedProject);
                        try {
                            repository.createCrowdJob(crowdJob);
                        }
                        catch (IOException e) {
                            error("Problem Processing Log file");
                        }
                        selectedCrowdJob = crowdJob;
                        createCrowdJob = false;
                        updateTable();
                    }
                    else {
                        selectedCrowdJob.setApiKey(crowdJobDetailForm.getModelObject().apiKey);
                        try {
                            repository.createCrowdJob(selectedCrowdJob);
                        }
                        catch (IOException e) {
                            error("Problem Processing Log file");
                        }
                        error("Task [" + name + " ] already created!");
                    }
                }
            });

            // remove the crowd project
            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    repository.removeCrowdJob(selectedCrowdJob);
                    updateTable();
                    crowdJobDetailForm.setVisible(false);
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });

            // add documents to the crowd source project
            add(new Button("addDocuments", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    crowdDocumentListForm.setVisible(true);
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });

            // add documents to the crowd source project
            add(new Button("addGoldDocuments", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    goldDocumentListForm.setVisible(true);
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });

            // remove document from the crowd job
            add(new Button("removeDocument", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>(
                            selectedCrowdJob.getDocuments());
                    sourceDocuments
                            .removeAll(CrowdProjectDetailForm.this.getModelObject().documents);
                    selectedCrowdJob.setDocuments(new HashSet<SourceDocument>(sourceDocuments));
                    try {
                        repository.createCrowdJob(selectedCrowdJob);
                    }
                    catch (IOException e) {
                        error("Problem Processing Log file");
                    }
                    documents.removeAll(CrowdProjectDetailForm.this.getModelObject().documents);
                    crowdDocumentListForm.setModelObject(new SelectionModel());
                    updateTable();
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });

            // remove gold documents from the crowd job
            add(new Button("removeGoldDocument", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    List<SourceDocument> goldSourceDocuments = new ArrayList<SourceDocument>(
                            selectedCrowdJob.getGoldDocuments());
                    goldSourceDocuments
                            .removeAll(CrowdProjectDetailForm.this.getModelObject().goldDocuments);
                    selectedCrowdJob.setGoldDocuments(new HashSet<SourceDocument>(
                            goldSourceDocuments));
                    try {
                        repository.createCrowdJob(selectedCrowdJob);
                    }
                    catch (IOException e) {
                        error("Problem Processing Log file");
                    }
                    goldDocuments
                            .removeAll(CrowdProjectDetailForm.this.getModelObject().goldDocuments);
                    goldDocumentListForm.setModelObject(new SelectionModel());

                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });

            // send document to crowd flower and get back status and link
            add(new Button("uploadT1", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {

                    if (namedEntityTaskManager == null) {
                        namedEntityTaskManager = new NamedEntityTaskManager();
                    }

                    namedEntityTaskManager.setAPIKey(selectedCrowdJob.getApiKey());
                    User user = userRepository.get(CROWD_USER);

                    List<JCas> jCases = getSourceDocumentsJCases(user);
                    // Get the JCASes for each gold document
                    List<JCas> goldJCases = getGoldDocumentsJCases(user);
               /*     int useSent = useSentenceTextField.getModelObject() ==0?-1:useSentenceTextField.getModelObject();
                    int useGoldSent = useGoldSentenceTextField.getModelObject() ==0?*/

                    try {
                        String template = FileUtils.readFileToString(repository
                                .getTemplate(CROWD_NERTASK1_TEMPLATE));
                        selectedCrowdJob.setUseSents(useSentenceTextField.getModelObject());
                        selectedCrowdJob.setUseGoldSents(useGoldSentenceTextField.getModelObject());

                        String task1ID = namedEntityTaskManager.uploadNewNERTask1(template, jCases,
                                goldJCases, useSentenceTextField.getModelObject()==0?-1:useSentenceTextField.getModelObject(),
                                useGoldSentenceTextField.getModelObject());
                        selectedCrowdJob.setTask1Id(task1ID);
                        selectedCrowdJob.setStatus(namedEntityTaskManager.getStatusString(task1ID,
                                ""));

                        repository.createCrowdJob(selectedCrowdJob);

                        int omittedSentences = namedEntityTaskManager.getOmittedSentences();
                        if (omittedSentences == 0) {
                            info("Crowdflower task " + task1ID + " uploaded succesfully.");
                        }
                        else {
                            warn("Crowdflower task " + task1ID + " uploaded succesfully, but "
                                    + omittedSentences
                                    + " sentences were omitted due to processing errors (see log).");
                        }
                    }
                    catch (FileNotFoundException e) {
                        error("Could not find the template file for NER task 1: " + e.getMessage());
                    }
                    catch (JsonProcessingException e) {
                        error("Template for NER task 1 is mal formated: " + e.getMessage());
                    }
                    catch (IOException e) {
                        error("There was a problem reading the NER task 1 template file: "
                                + e.getMessage());
                    }
                    catch (Exception e) {
                        error("Something went wrong uploading your document(s) to crowdflower.com: "
                                + e.getMessage());
                    }

                }

            });

            // send document to crowd flower and get back status and link
            add(new Button("uploadT2", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (namedEntityTaskManager == null) {
                        namedEntityTaskManager = new NamedEntityTaskManager();
                    }

                    namedEntityTaskManager.setAPIKey(selectedCrowdJob.getApiKey());
                    User user = userRepository.get(CROWD_USER);

                    List<JCas> jCases = getSourceDocumentsJCases(user);

                    // Get the JCASes for each gold document
                    List<JCas> goldJCases = getGoldDocumentsJCases(user);
                    try {

                        String task1ID = selectedCrowdJob.getTask1Id();

                        if (task1ID == null) {
                            error("Task1 not yet completed");
                            return;
                        }

                        String template = FileUtils.readFileToString(repository
                                .getTemplate(CROWD_NERTASK2_TEMPLATE));
                        String task2ID = namedEntityTaskManager.uploadNewNERTask2(template,
                                task1ID, jCases, goldJCases);
                        selectedCrowdJob.setTask2Id(task2ID);
                        repository.createCrowdJob(selectedCrowdJob);

                        int omittedSentences = namedEntityTaskManager.getOmittedSentences();
                        if (omittedSentences == 0) {
                            info("Crowdflower task " + task2ID + " uploaded succesfully.");
                        }
                        else {
                            warn("Crowdflower task " + task2ID + " uploaded succesfully, but "
                                    + omittedSentences
                                    + " sentences were omitted due to processing errors (see log).");
                        }

                    }
                    catch (FileNotFoundException e) {
                        error("Could not find the template file for NER task 1: " + e.getMessage());
                    }
                    catch (JsonProcessingException e) {
                        error("Template for NER task 1 is mal formated: " + e.getMessage());
                    }
                    catch (IOException e) {
                        error("There was a problem reading the NER task 1 template file: "
                                + e.getMessage());
                    }
                    catch (Exception e) {
                        error("Something went wrong uploading your document(s) to crowdflower.com: "
                                + e.getMessage());
                    }

                }

                @Override
                public boolean isVisible()
                {
                    return true;
                }
            });

            add(new Button("retrieveT2", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    try {
                        if (namedEntityTaskManager == null) {
                            namedEntityTaskManager = new NamedEntityTaskManager();
                        }

                        namedEntityTaskManager.setAPIKey(selectedCrowdJob.getApiKey());
                        User user = userRepository.get(CROWD_USER);

                        List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>(
                                selectedCrowdJob.getDocuments());
                        List<JCas> jCases = getSourceDocumentsJCases(user);

                        String task2ID = selectedCrowdJob.getTask2Id();

                        if (task2ID == null) {
                            error("Task2 not yet completed");
                            return;
                        }

                        // clear previous annotation / imports for the crowd user
                        int i = 0;
                        for (JCas cas : jCases) {
                            BratAnnotatorUtility.clearJcasAnnotations(cas, sourceDocuments.get(i),
                                    user, repository);
                            i++;
                        }

                        // import job results from crowdflwoer
                        namedEntityTaskManager.setCrowdJobAnnotationsInDocs(task2ID, jCases);

                        // save all changed document annotations on disk
                        i = 0;
                        for (JCas cas : jCases) {
                            SourceDocument document = sourceDocuments.get(i);
                            repository.createAnnotationDocumentContent(cas, document, user);
                            i++;
                        }
                        int omittedEntities = namedEntityTaskManager.getOmittedEntities();

                        if (omittedEntities == 0) {
                            info("Crowdflower job annotations imported succesfully for this task.");
                        }
                        else {
                            warn("Warning: Crowdflower job annotations imported succesfully for this task, but "
                                    + omittedEntities
                                    + " annotations were omitted due to processing errors (see log).");
                        }
                    }
                    catch (JsonProcessingException e) {
                        error("Json processing problem: " + e.getMessage());
                    }
                    catch (IOException e) {
                        error("Input/Output exception: " + e.getMessage());
                    }
                    catch (Exception e) {
                        error("Something went wrong importing your document(s) from crowdflower.com: "
                                + e.getMessage());
                    }
                }

                @Override
                public boolean isVisible()
                {
                    return true;
                }
            });

            // update the status of this source document from crowd
            add(new Button("update", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (namedEntityTaskManager == null) {
                        namedEntityTaskManager = new NamedEntityTaskManager();
                    }
                    namedEntityTaskManager.setAPIKey(selectedCrowdJob.getApiKey());

                    String id1 = selectedCrowdJob.getTask1Id();
                    if (id1 == null) {
                        id1 = "";
                    }
                    String id2 = selectedCrowdJob.getTask2Id();
                    if (id2 == null) {
                        id2 = "";
                    }

                    String status = namedEntityTaskManager.getStatusString(id1, id2);
                    selectedCrowdJob.setStatus(status);
                    try {
                        repository.createCrowdJob(selectedCrowdJob);
                    }
                    catch (IOException e) {
                        error("Problem Processing Log file");
                    }
                }

                @Override
                public boolean isVisible()
                {
                    return !createCrowdJob;

                }
            });
        }
    }
    private void addJCas(List<JCas> jCases, User user, SourceDocument sourceDocument)
    {
        AnnotationDocument annotationDocument;
        JCas jCas;
        try {
            if (repository.existsAnnotationDocument(sourceDocument, user)) {

                jCas = repository.readJCas(sourceDocument, selectedProject, user);

                jCases.add(jCas);
            }
            else {
                annotationDocument = new AnnotationDocument();
                annotationDocument.setDocument(sourceDocument);
                annotationDocument.setUser(user.getUsername());
                annotationDocument.setProject(selectedProject);
                // annotationDocument.setState(AnnotationDocumentState.IN_PROGRESS);
                annotationDocument.setName(sourceDocument.getName());
                repository.createAnnotationDocument(annotationDocument);

                jCas = repository.getJCasFromFile(repository
                        .getSourceDocumentContent(sourceDocument), repository.getReadableFormats()
                        .get(sourceDocument.getFormat()));
                repository.createAnnotationDocumentContent(jCas, sourceDocument, user);
                jCases.add(jCas);
            }
        }
        catch (UIMAException e) {
            error(e.getMessage() + " : " + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error(e.getMessage());
        }
        catch (ClassNotFoundException e) {
            error(e.getMessage());
        }
    }

    private class CrowdDocumentListForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = 293213755095594897L;

        public CrowdDocumentListForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new CheckBoxMultipleChoice<SourceDocument>("documents")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<SourceDocument> load()
                        {
                            List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();
                            for (SourceDocument sourceDocument : repository
                                    .listSourceDocuments(selectedProject)) {
                                // if not a GOLD data!
                                if (!sourceDocument.getState().equals(
                                        SourceDocumentState.CURATION_FINISHED)) {
                                    sourceDocuments.add(sourceDocument);
                                }
                            }

                            // filter already added documents
                            for (CrowdJob crowdJob : repository.listCrowdJobs()) {
                                sourceDocuments.removeAll(new ArrayList<SourceDocument>(crowdJob
                                        .getDocuments()));
                            }

                            Collections.sort(sourceDocuments, new Comparator<SourceDocument>()
                            {
                                @Override
                                public int compare(SourceDocument doc1, SourceDocument doc2)
                                {
                                    return (doc1.getProject().getName() + doc1.getName())
                                            .compareTo(doc2.getProject().getName() + doc2.getName());
                                }
                            });

                            return sourceDocuments;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<SourceDocument>()
                    {
                        private static final long serialVersionUID = 5990012140581607347L;

                        @Override
                        public Object getDisplayValue(SourceDocument aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }
            }).setOutputMarkupId(true);
            // add documents to the crowd source job
            add(new Button("add", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {

                    List<SourceDocument> sourceDocuments = CrowdDocumentListForm.this
                            .getModelObject().documents;
                    if (sourceDocuments != null) {
                        Set<SourceDocument> existingDocuments = selectedCrowdJob.getDocuments();
                        sourceDocuments.addAll(new ArrayList<SourceDocument>(existingDocuments));
                        selectedCrowdJob.setDocuments(new HashSet<SourceDocument>(sourceDocuments));
                        try {
                            repository.createCrowdJob(selectedCrowdJob);
                        }
                        catch (IOException e) {
                            error("Problem Processing Log file");
                        }
                    }
                    CrowdDocumentListForm.this.setVisible(false);
                    updateTable();
                }
            });
            // add documents to the crowd source project
            add(new Button("cancel", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    CrowdDocumentListForm.this.setVisible(false);
                }
            });
        }

    }

    private class GoldDocumentListForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = 293213755095594897L;

        public GoldDocumentListForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new CheckBoxMultipleChoice<SourceDocument>("goldDocuments")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = -6821990375210752730L;

                        @Override
                        protected List<SourceDocument> load()
                        {
                            List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();
                            for (SourceDocument sourceDocument : repository
                                    .listSourceDocuments(selectedProject)) {
                                if (sourceDocument.getState().equals(
                                        SourceDocumentState.CURATION_FINISHED)) {
                                    sourceDocuments.add(sourceDocument);
                                }
                            }

                            // remove already added gold documents from the list
                            for (CrowdJob crowdJob : repository.listCrowdJobs()) {
                                sourceDocuments.removeAll(new ArrayList<SourceDocument>(crowdJob
                                        .getGoldDocuments()));
                            }

                            Collections.sort(sourceDocuments, new Comparator<SourceDocument>()
                            {
                                @Override
                                public int compare(SourceDocument doc1, SourceDocument doc2)
                                {
                                    return (doc1.getProject().getName() + doc1.getName())
                                            .compareTo(doc2.getProject().getName() + doc2.getName());
                                }
                            });

                            return sourceDocuments;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<SourceDocument>()
                    {
                        private static final long serialVersionUID = 5990012140581607347L;

                        @Override
                        public Object getDisplayValue(SourceDocument aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }
            }).setOutputMarkupId(true);

            add(new Button("add", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {

                    List<SourceDocument> sourceDocuments = GoldDocumentListForm.this
                            .getModelObject().goldDocuments;
                    if (sourceDocuments != null) {
                        Set<SourceDocument> oldDocuments = selectedCrowdJob.getGoldDocuments();
                        sourceDocuments.addAll(new ArrayList<SourceDocument>(oldDocuments));
                        selectedCrowdJob.setGoldDocuments(new HashSet<SourceDocument>(
                                sourceDocuments));
                        try {
                            repository.createCrowdJob(selectedCrowdJob);
                        }
                        catch (IOException e) {
                            error("Problem Processing Log file");
                        }
                    }
                    GoldDocumentListForm.this.setVisible(false);
                }
            });

            // add documents to the crowd source project
            add(new Button("cancel", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    GoldDocumentListForm.this.setVisible(false);
                }
            });
        }

    }

    private void updateTable()
    {
        remove(crowdJobForm);
        crowdJobForm = new CrowdJobForm("crowdJobForm");
        add(crowdJobForm);
    }

    private class DocumentColumnMetaData
        extends AbstractColumn<List<String>>
    {
        private static final long serialVersionUID = -3632527878408587144L;

        private int columnNumber;

        public DocumentColumnMetaData(final TableDataProvider prov, final int colNumber)
        {
            super(new AbstractReadOnlyModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject()
                {
                    return prov.getColNames().get(colNumber);

                }
            });
            columnNumber = colNumber;
        }

        @Override
        public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
                final String componentId, final IModel<List<String>> rowModel)
        {
            int rowNumber = aCellItem.getIndex();
            aCellItem.setOutputMarkupId(true);

            final String value = rowModel.getObject().get(columnNumber).trim(); // the project

            if (rowNumber == rowModel.getObject().size() - 1) {
                aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                        "/images_small/page_edit.png")));
                aCellItem.add(AttributeModifier.append("class", "centering"));
                aCellItem.add(new AjaxEventBehavior("onclick")
                {
                    private static final long serialVersionUID = -4213621740511947285L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        selectedCrowdJob = repository.getCrowdJob(value);
                        SelectionModel selectionModel = new SelectionModel();
                        selectionModel.name = selectedCrowdJob.getName();
                        selectionModel.apiKey = selectedCrowdJob.getApiKey();
                        selectionModel.link = selectedCrowdJob.getLink();
                        selectionModel.status = selectedCrowdJob.getStatus();

                        crowdJobDetailForm.setModelObject(selectionModel);
                        crowdJobDetailForm.setVisible(true);
                        createCrowdJob = false;
                        aTarget.add(crowdJobDetailForm.setOutputMarkupId(true));
                        aTarget.appendJavaScript("window.location.reload()");
                        aTarget.add(crowdJobForm);

                    }
                });
            }

            else {
                aCellItem.add(new Label(componentId, value));
            }
        }
    }

    static public class SelectionModel
        implements Serializable
    {

        private static final long serialVersionUID = 4339073913927301132L;
        Project project;
        String name;
        String apiKey;
        int useSents;
        int useGoldSents;
        List<SourceDocument> documents;
        List<SourceDocument> goldDocuments;
        String link;
        String link2;
        String status;
    }
}
