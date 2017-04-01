/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class OpenModalWindowPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    @SpringBean(name = "documentRepository")
    private ProjectService repository;

    @SpringBean(name = "documentRepository")
    private DocumentService documentService;

    @SpringBean(name = "documentRepository")
    private CurationDocumentService curationDocumentService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    // Project list, Document List and buttons List, contained in separet forms
    private final ProjectSelectionForm projectSelectionForm;
    private final DocumentSelectionForm documentSelectionForm;
    private final ButtonsForm buttonsForm;
    private Select<SourceDocument> documentSelection;

    // The first project - selected by default
    private Project selectedProject;
    // The first document in the project // auto selected in the first time.
    private SourceDocument selectedDocument;

    private final String username;
    private final User user;

    // Dialog is for annotation or curation

    private final Mode mode;
    private final AnnotatorState bModel;

    private List<Project> projectesWithFinishedAnnos;
    private Map<Project, String> projectColors = new HashMap<Project, String>();

    public OpenModalWindowPanel(String aId, AnnotatorState aBModel,
            ModalWindow aModalWindow, Mode aSubject)
    {
        super(aId);
        this.mode = aSubject;
        username = SecurityContextHolder.getContext().getAuthentication().getName();
        user = userRepository.get(username);
        if (mode.equals(Mode.CURATION)) {
            projectesWithFinishedAnnos = repository.listProjectsWithFinishedAnnos();
        }
        if (getAllowedProjects().size() > 0) {
            selectedProject = getAllowedProjects().get(0);
        }

        this.bModel = aBModel;
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");
        documentSelectionForm = new DocumentSelectionForm("documentSelectionForm", aModalWindow);
        buttonsForm = new ButtonsForm("buttonsForm", aModalWindow);

        add(buttonsForm);
        add(projectSelectionForm);
        add(documentSelectionForm);
    }

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Select<Project> projectSelection;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            projectSelection = new Select<Project>("projectSelection");
            ListView<Project> lv = new ListView<Project>("projects",
                    new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            return getAllowedProjects();
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<Project> item)
                {
                    String color = projectColors.get(item.getModelObject());
                    if (color == null) {
                        color = "#008000";// not in curation
                    }
                    item.add(new SelectOption<Project>("project", new Model<Project>(item
                            .getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag, item.getModelObject()
                                    .getName());
                        }
                    }.add(new AttributeModifier("style", "color:" + color + ";")));
                }
            };
            add(projectSelection.add(lv));
            projectSelection.setOutputMarkupId(true);
            projectSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().projectSelection;
                    // Remove selected document from other project
                    selectedDocument = null;
                    documentSelection.setModelObject(selectedDocument);
                    aTarget.add(documentSelection);
                }
            }).add(new AjaxEventBehavior("dblclick")
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(final AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().projectSelection;
                    // Remove selected document from other project
                    selectedDocument = null;
                    aTarget.add(documentSelection.setOutputMarkupId(true));
                }
            });
        }
    }

    public List<Project> getAllowedProjects()
    {
        List<Project> allowedProject = new ArrayList<Project>();
        switch (mode) {
        case ANNOTATION:
            for (Project project : repository.listProjects()) {
                if (SecurityUtil.isAnnotator(project, repository, user)
                        && project.getMode().equals(Mode.ANNOTATION)) {
                    allowedProject.add(project);
                }
            }
            break;
        case CURATION:
            for (Project project : repository.listProjects()) {
                if (SecurityUtil.isCurator(project, repository, user)) {
                    allowedProject.add(project);
                    if (projectesWithFinishedAnnos.contains(project)) {
                        projectColors.put(project, "#008000");
                    }
                    else {
                        projectColors.put(project, "#99cc99");
                    }
                }
            }
            break;
        case CORRECTION:
            for (Project project : repository.listProjects()) {
                if (SecurityUtil.isAnnotator(project, repository, user)
                        && project.getMode().equals(Mode.CORRECTION)) {
                    allowedProject.add(project);
                }
            }
            break;
        case AUTOMATION:
            for (Project project : repository.listProjects()) {
                if (SecurityUtil.isAnnotator(project, repository, user)
                        && project.getMode().equals(Mode.AUTOMATION)) {
                    allowedProject.add(project);
                }
            }
            break;
        default:
            break;
        }

        return allowedProject;
    }

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project projectSelection;
        private SourceDocument documentSelection;
    }

    private class DocumentSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        ListView<SourceDocument> lv;
        
        public DocumentSelectionForm(String id, final ModalWindow modalWindow)
        {

            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            final Map<SourceDocument, String> documentColors = new HashMap<SourceDocument, String>();

            documentSelection = new Select<SourceDocument>("documentSelection");
            lv = new ListView<SourceDocument>("documents",
                    new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<SourceDocument> load()
                        {
                            List<SourceDocument> allDocuments = listDocuments(documentColors);
                            return allDocuments;
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<SourceDocument> item)
                {
                    item.add(new SelectOption<SourceDocument>("document",
                            new Model<SourceDocument>(item.getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag, item.getModelObject()
                                    .getName());
                        }
                    }.add(new AttributeModifier("style", "color:"
                            + documentColors.get(item.getModelObject()) + ";")));
                }
            };
            add(documentSelection.add(lv));
            documentSelection.setOutputMarkupId(true);
            documentSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedDocument = getModelObject().documentSelection;
                }
            }).add(new AjaxEventBehavior("dblclick")
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(final AjaxRequestTarget aTarget)
                {
                    // do not use this default layer in that other project
                    if(bModel.getProject()!=null){
                        if(!bModel.getProject().equals(selectedProject)){
                            bModel.setDefaultAnnotationLayer(null);
                        }
                    }
                    if (selectedProject != null && selectedDocument != null) {
                        bModel.setProject(selectedProject);
                        bModel.setDocument(selectedDocument, lv.getModelObject());
                        modalWindow.close(aTarget);
                    }
                }
            });
        }
    }

    private List<SourceDocument> listDocuments(final Map<SourceDocument, String> states)
    {
        if (selectedProject == null) {
            return new ArrayList<SourceDocument>();
        }
        
        List<SourceDocument> allSourceDocuments = new ArrayList<>();

        // Remove from the list source documents that are in IGNORE state OR
        // that do not have at least one annotation document marked as
        // finished for curation dialog
        switch (mode) {
        case ANNOTATION:
        case AUTOMATION:
        case CORRECTION: {
            Map<SourceDocument, AnnotationDocument> docs = documentService
                    .listAnnotatableDocuments(selectedProject, user);

            for (Entry<SourceDocument, AnnotationDocument> e : docs.entrySet()) {
                if (e.getValue() != null) {
                    SourceDocument sourceDocument = e.getKey();
                    AnnotationDocument adoc = e.getValue();
                    if (AnnotationDocumentState.FINISHED.equals(adoc.getState())) {
                        states.put(sourceDocument, "red");
                    }
                    else if (AnnotationDocumentState.IN_PROGRESS.equals(adoc.getState())) {
                        states.put(sourceDocument, "blue");
                    }
                }
            }

            allSourceDocuments = new ArrayList<>(docs.keySet());
            break;
        }
        case CURATION: {
            allSourceDocuments = curationDocumentService.listCuratableSourceDocuments(selectedProject);
            
            for (SourceDocument sourceDocument : allSourceDocuments) {
                if (SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())) {
                    states.put(sourceDocument, "red");
                }
                else if (SourceDocumentState.CURATION_IN_PROGRESS.equals(sourceDocument.getState())) {
                    states.put(sourceDocument, "blue");
                }
            }

            break;
        }
        default:
            break;
        }
        
        return allSourceDocuments;
    }

    private class ButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -1879323194964417564L;

        public ButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            add(new AjaxSubmitLink("openButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    if (selectedProject == null) {
                        aTarget.appendJavaScript("alert('No project is selected!')"); // If there is
                                                                                      // no project
                                                                                      // at all
                    }
                    else if (selectedDocument == null) {
                        if (documentService.existsFinishedAnnotation(selectedProject)) {
                            aTarget.appendJavaScript("alert('Please select a document for project: "
                                    + selectedProject.getName() + "')");
                        }
                        else {
                            aTarget.appendJavaScript("alert('There is no document that is ready for curation yet for project: "
                                    + selectedProject.getName() + "')");
                        }
                    }
                    else {
                        // do not use this default layer in that other project
                        if(bModel.getProject()!=null){
                            if(!bModel.getProject().equals(selectedProject)){
                                bModel.setDefaultAnnotationLayer(null);
                            }
                        }
                        
                        bModel.setProject(selectedProject);
                        bModel.setDocument(selectedDocument,
                                documentSelectionForm.lv.getModelObject());
                        modalWindow.close(aTarget);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("cancelButton")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    projectSelectionForm.detach();
                    documentSelectionForm.detach();
                    if (mode.equals(Mode.CURATION)) {
                        bModel.setDocument(null, null); // on cancel, go welcomePage
                    }
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }
}
