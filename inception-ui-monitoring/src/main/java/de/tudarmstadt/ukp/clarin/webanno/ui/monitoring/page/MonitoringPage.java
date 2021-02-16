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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.IGNORE_TO_NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.SetModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.AnnotatorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.AnnotatorColumnSelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.CuratorColumnCellClickEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.CuratorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.DocumentRowSelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.AnnotatorColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.CuratorColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixDataProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixRow;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixSortKey;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.SourceDocumentNameColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.SourceDocumentSelectColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.SourceDocumentStateColumn;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.UserSelectToolbar;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/monitoring")
public class MonitoringPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean CurationDocumentService curationService;

    private DataTable<DocumentMatrixRow, DocumentMatrixSortKey> documentMatrix;
    private LambdaAjaxLink toggleBulkChange;
    private WebMarkupContainer actionContainer;
    private WebMarkupContainer bulkActionDropdown;
    private WebMarkupContainer bulkActionDropdownButton;
    private ChallengeResponseDialog resetDocumentDialog;
    private ContextMenu contextMenu;

    private boolean bulkChangeMode = false;
    private IModel<Set<String>> selectedUsers = new SetModel<>(new HashSet<>());

    public MonitoringPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        User user = userRepository.getCurrentUser();

        Project project = getProject();

        requireProjectRole(user, CURATOR, MANAGER);

        add(new Label("name", project.getName()));

        // add(annotatorsProgressImage = createAnnotatorProgress());
        // add(annotatorsProgressPercentageImage = createAnnotatorProgressPercentage());

        add(documentMatrix = createDocumentMatrix("documentMatrix", bulkChangeMode));

        actionContainer = new WebMarkupContainer("actionContainer");
        actionContainer.setOutputMarkupPlaceholderTag(true);
        add(actionContainer);

        bulkActionDropdown = new WebMarkupContainer("bulkActionDropdown");
        bulkActionDropdown.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        actionContainer.add(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer("bulkActionDropdownButton");
        bulkActionDropdownButton.add(LambdaBehavior.visibleWhen(() -> bulkChangeMode));
        actionContainer.add(bulkActionDropdownButton);

        toggleBulkChange = new LambdaAjaxLink("toggleBulkChange", this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        actionContainer.add(toggleBulkChange);

        bulkActionDropdown.add(new LambdaAjaxLink("bulkLock", this::actionBulkLock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkUnlock", this::actionBulkUnlock));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkFinish", this::actionBulkFinish));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkResume", this::actionBulkResume));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkOpen", this::actionBulkOpen));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkClose", this::actionBulkClose));
        bulkActionDropdown.add(new LambdaAjaxLink("bulkReset", this::actionBulkResetDocument));

        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog"));
        add(contextMenu = new ContextMenu("contextMenu"));
    }

    private DataTable<DocumentMatrixRow, DocumentMatrixSortKey> createDocumentMatrix(
            String aComponentId, boolean aBulkChangeMode)
    {
        DocumentMatrixDataProvider dataProvider = new DocumentMatrixDataProvider(getMatrixData());

        if (documentMatrix != null) {
            dataProvider.setSort(
                    ((DocumentMatrixDataProvider) documentMatrix.getDataProvider()).getSort());
        }

        List<IColumn<DocumentMatrixRow, DocumentMatrixSortKey>> columns = new ArrayList<>();
        SourceDocumentSelectColumn sourceDocumentSelectColumn = new SourceDocumentSelectColumn();
        sourceDocumentSelectColumn.setVisible(bulkChangeMode);
        columns.add(sourceDocumentSelectColumn);
        columns.add(new SourceDocumentStateColumn());
        columns.add(new SourceDocumentNameColumn());
        columns.add(new CuratorColumn());

        List<User> annotators = projectService.listProjectUsersWithPermissions(getProject(),
                ANNOTATOR);
        for (User annotator : annotators) {
            columns.add(new AnnotatorColumn(annotator.getUsername(), selectedUsers));
        }

        DataTable<DocumentMatrixRow, DocumentMatrixSortKey> table = new DefaultDataTable<>(
                aComponentId, columns, dataProvider, 50);
        table.setOutputMarkupId(true);

        if (aBulkChangeMode) {
            table.addTopToolbar(new UserSelectToolbar(selectedUsers, table));
        }

        return table;
    }

    private void actionToggleBulkChange(AjaxRequestTarget aTarget)
    {
        bulkChangeMode = !bulkChangeMode;
        selectedUsers.getObject().clear();

        DataTable<DocumentMatrixRow, DocumentMatrixSortKey> newMatrix = createDocumentMatrix(
                "documentMatrix", bulkChangeMode);
        documentMatrix.replaceWith(newMatrix);
        documentMatrix = newMatrix;

        aTarget.add(documentMatrix, toggleBulkChange, actionContainer);
    }

    private void actionBulkResetDocument(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS || annDoc.getState() == FINISHED)
                .collect(Collectors.toList());

        IModel<String> projectNameModel = Model.of(getProject().getName());
        resetDocumentDialog
                .setTitleModel(new StringResourceModel("BulkResetDocumentDialog.title", this));
        resetDocumentDialog
                .setChallengeModel(new StringResourceModel("BulkResetDocumentDialog.text", this)
                        .setParameters(selectedDocuments.size(), projectNameModel));
        resetDocumentDialog.setResponseModel(projectNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            Map<String, User> userCache = new HashMap<>();
            for (AnnotationDocument document : selectedDocuments) {
                User user = userCache.computeIfAbsent(document.getUser(),
                        username -> userRepository.get(username));
                documentService.resetAnnotationCas(document.getDocument(), user);
            }

            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionResetAnnotationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            String aUser)
    {
        IModel<String> documentNameModel = Model.of(aDocument.getName());
        resetDocumentDialog
                .setTitleModel(new StringResourceModel("ResetDocumentDialog.title", this));
        resetDocumentDialog
                .setChallengeModel(new StringResourceModel("ResetDocumentDialog.text", this)
                        .setParameters(documentNameModel, aUser));
        resetDocumentDialog.setResponseModel(documentNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            User user = userRepository.get(aUser);
            documentService.resetAnnotationCas(aDocument, user);
            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionResetCurationDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        IModel<String> documentNameModel = Model.of(aDocument.getName());
        resetDocumentDialog
                .setTitleModel(new StringResourceModel("ResetCurationDocumentDialog.title", this));
        resetDocumentDialog
                .setChallengeModel(new StringResourceModel("ResetCurationDocumentDialog.text", this)
                        .setParameters(documentNameModel));
        resetDocumentDialog.setResponseModel(documentNameModel);
        resetDocumentDialog.setConfirmAction(_target -> {
            curationService.deleteCurationCas(aDocument);
            documentService.setSourceDocumentState(aDocument, ANNOTATION_IN_PROGRESS);
            reloadMatrixData();
            _target.add(documentMatrix);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionBulkOpen(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments();

        List<AnnotationDocument> lockedDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == IGNORE).collect(toList());
        documentService.bulkSetAnnotationDocumentState(lockedDocuments, NEW);

        List<AnnotationDocument> finishedDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == FINISHED).collect(toList());
        documentService.bulkSetAnnotationDocumentState(finishedDocuments, IN_PROGRESS);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkClose(AjaxRequestTarget aTarget)
    {
        Collection<AnnotationDocument> selectedDocuments = selectedAnnotationDocuments();

        List<AnnotationDocument> newDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == NEW).collect(toList());
        documentService.bulkSetAnnotationDocumentState(newDocuments, IGNORE);

        List<AnnotationDocument> inProgressDocuments = selectedDocuments.stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS).collect(toList());
        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, FINISHED);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkLock(AjaxRequestTarget aTarget)
    {
        List<AnnotationDocument> newDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == NEW).collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, IGNORE);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkUnlock(AjaxRequestTarget aTarget)
    {
        List<AnnotationDocument> newDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IGNORE).collect(toList());

        documentService.bulkSetAnnotationDocumentState(newDocuments, NEW);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkFinish(AjaxRequestTarget aTarget)
    {
        List<AnnotationDocument> inProgressDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == IN_PROGRESS).collect(toList());

        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, FINISHED);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private void actionBulkResume(AjaxRequestTarget aTarget)
    {
        List<AnnotationDocument> inProgressDocuments = selectedAnnotationDocuments().stream()
                .filter(annDoc -> annDoc.getState() == FINISHED).collect(toList());

        documentService.bulkSetAnnotationDocumentState(inProgressDocuments, IN_PROGRESS);

        reloadMatrixData();
        aTarget.add(documentMatrix);
    }

    private Collection<AnnotationDocument> selectedAnnotationDocuments()
    {
        List<User> annotators = projectService.listProjectUsersWithPermissions(getProject(),
                ANNOTATOR);

        Map<String, User> annotatorIndex = new HashMap<>();
        annotators.forEach(annotator -> annotatorIndex.put(annotator.getUsername(), annotator));

        Set<User> selectedUserObjects = new HashSet<>();
        selectedUsers.getObject()
                .forEach(username -> selectedUserObjects.add(annotatorIndex.get(username)));

        Map<Pair<SourceDocument, String>, AnnotationDocument> annotationDocumentsToChange = new HashMap<>();

        List<DocumentMatrixRow> rows = ((DocumentMatrixDataProvider) documentMatrix
                .getDataProvider()).getMatrixData();

        for (DocumentMatrixRow row : rows) {
            // Collect annotation documents by row
            if (row.isSelected()) {
                for (User annotator : annotators) {
                    AnnotationDocument annDoc = row.getAnnotationDocument(annotator.getUsername());
                    if (annDoc == null) {
                        annDoc = documentService
                                .createOrGetAnnotationDocument(row.getSourceDocument(), annotator);
                    }

                    annotationDocumentsToChange
                            .put(Pair.of(row.getSourceDocument(), annotator.getUsername()), annDoc);
                }
            }

            // Collect annotation documents by column
            for (User anotator : selectedUserObjects) {
                Pair<SourceDocument, String> key = Pair.of(row.getSourceDocument(),
                        anotator.getUsername());
                if (!annotationDocumentsToChange.containsKey(key)) {
                    AnnotationDocument annDoc = documentService
                            .createOrGetAnnotationDocument(row.getSourceDocument(), anotator);
                    annotationDocumentsToChange.put(key, annDoc);
                }
            }
        }

        return annotationDocumentsToChange.values();
    }

    @OnEvent
    public void onAnnotatorColumnSelectionChangedEvent(AnnotatorColumnSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onDocumentRowSelectionChangedEvent(DocumentRowSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onAnnotatorColumnCellClickEvent(AnnotatorColumnCellClickEvent aEvent)
    {
        User user = userRepository.get(aEvent.getUsername());
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aEvent.getSourceDocument(), user);

        AnnotationDocumentStateTransition transition;
        switch (annotationDocument.getState()) {
        case NEW:
            transition = NEW_TO_IGNORE;
            break;
        case IGNORE:
            transition = IGNORE_TO_NEW;
            break;
        case IN_PROGRESS:
            transition = ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
            break;
        case FINISHED:
            transition = ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
            break;
        default:
            return;
        }

        documentService.transitionAnnotationDocumentState(annotationDocument, transition);

        reloadMatrixData();

        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onCuratorColumnCellClickEvent(CuratorColumnCellClickEvent aEvent)
    {
        SourceDocumentStateTransition transition;
        switch (aEvent.getSourceDocument().getState()) {
        case CURATION_IN_PROGRESS:
            transition = CURATION_IN_PROGRESS_TO_CURATION_FINISHED;
            break;
        case CURATION_FINISHED:
            transition = CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
            break;
        default:
            info("Curation state can only be changed once curation has started.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        documentService.transitionSourceDocumentState(aEvent.getSourceDocument(), transition);

        reloadMatrixData();

        aEvent.getTarget().add(documentMatrix);
    }

    @OnEvent
    public void onAnnotatorColumnCellOpenContextMenuEvent(
            AnnotatorColumnCellOpenContextMenuEvent aEvent)
    {
        if (aEvent.getState() == NEW || aEvent.getState() == IGNORE) {
            info("Documents on which work has not yet been started cannot be reset.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        List<IMenuItem> items = contextMenu.getItemList();
        items.clear();

        // The AnnotatorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        SourceDocument document = aEvent.getSourceDocument();
        String username = aEvent.getUsername();
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetAnnotationDocument(_target, document, username)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    @OnEvent
    public void onCuratorColumnCellOpenContextMenuEvent(
            CuratorColumnCellOpenContextMenuEvent aEvent)
    {
        SourceDocumentState state = aEvent.getSourceDocument().getState();

        if (state != CURATION_IN_PROGRESS && state != CURATION_FINISHED) {
            info("Documents on which curation has not yet been started cannot be reset.");
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            return;
        }

        List<IMenuItem> items = contextMenu.getItemList();
        items.clear();

        // The CuratorColumnCellOpenContextMenuEvent is not serializable, so we need to extract
        // the information we need in the menu item here
        SourceDocument document = aEvent.getSourceDocument();
        items.add(new LambdaMenuItem("Reset",
                _target -> actionResetCurationDocument(_target, document)));

        contextMenu.onOpen(aEvent.getTarget(), aEvent.getCell());
    }

    private void reloadMatrixData()
    {
        ((DocumentMatrixDataProvider) documentMatrix.getDataProvider())
                .setMatrixData(getMatrixData());
    }

    private List<DocumentMatrixRow> getMatrixData()
    {
        Map<SourceDocument, DocumentMatrixRow> documentMatrixRows = new LinkedHashMap<>();
        for (SourceDocument srcDoc : documentService.listSourceDocuments(getProject())) {
            documentMatrixRows.put(srcDoc, new DocumentMatrixRow(srcDoc));
        }

        for (AnnotationDocument annDoc : documentService.listAnnotationDocuments(getProject())) {
            documentMatrixRows.get(annDoc.getDocument()).add(annDoc);
        }

        return new ArrayList<>(documentMatrixRows.values());
    }
}
