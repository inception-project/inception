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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.documents.SourceDocumentTableSortKeys.CREATED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.documents.SourceDocumentTableSortKeys.FORMAT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.documents.SourceDocumentTableSortKeys.NAME;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.documents.SourceDocumentTableSortKeys.STATE;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.NonEscapingLambdaColumn;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketExceptionUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentFilterStateChanged;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentStateFilterPanel;

public class SourceDocumentTable
    extends Panel
{
    private static final long serialVersionUID = 3993790906387166039L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CID_BULK_ACTION_DROPDOWN_BUTTON = "bulkActionDropdownButton";
    private static final String CID_BULK_ACTION_DROPDOWN = "bulkActionDropdown";
    private static final String CID_DATA_TABLE = "dataTable";
    private static final String CID_STATE_FILTERS = "stateFilters";
    private static final String CID_NAME_FILTER = "nameFilter";
    private static final String CID_TOGGLE_BULK_CHANGE = "toggleBulkChange";

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentImportExportService importExportService;

    private SourceDocumentTableDataProvider dataProvider;
    private DataTable<SourceDocumentTableRow, SourceDocumentTableSortKeys> table;
    private TextField<String> nameFilter;
    private LambdaAjaxLink toggleBulkChange;
    private ChallengeResponseDialog confirmationDialog;
    private WebMarkupContainer bulkActionDropdown;
    private WebMarkupContainer bulkActionDropdownButton;

    private boolean bulkChangeMode = false;
    private SourceDocumentSelectColumn selectColumns;

    public SourceDocumentTable(String aId, IModel<List<SourceDocument>> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        dataProvider = new SourceDocumentTableDataProvider(aModel
                .map(docs -> docs.stream().map(SourceDocumentTableRow::new).collect(toList())));

        var columns = new ArrayList<IColumn<SourceDocumentTableRow, SourceDocumentTableSortKeys>>();
        selectColumns = new SourceDocumentSelectColumn(this, dataProvider);
        columns.add(selectColumns);
        columns.add(new NonEscapingLambdaColumn<>(new ResourceModel("DocumentState"), STATE,
                $ -> $.getDocument().getState().symbol()));
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentName"), NAME,
                $ -> $.getDocument().getName()));
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentFormat"), FORMAT,
                $ -> renderFormat($.getDocument().getFormat())));
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentCreated"), CREATED,
                $ -> renderDate($.getDocument().getCreated())));
        columns.add(new SourceDocumentTableDeleteActionColumn(this));
        columns.add(new SourceDocumentTableExportActionColumn(this));

        table = new DataTable<>(CID_DATA_TABLE, columns, dataProvider, 100);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new AjaxNavigationToolbar(table));
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<>(table, dataProvider));
        queue(table);

        nameFilter = new TextField<>(CID_NAME_FILTER,
                PropertyModel.of(dataProvider.getFilterState(), "documentName"), String.class);
        nameFilter.setOutputMarkupPlaceholderTag(true);
        nameFilter.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        queue(nameFilter);

        toggleBulkChange = new LambdaAjaxLink(CID_TOGGLE_BULK_CHANGE, this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        queue(toggleBulkChange);

        bulkActionDropdown = new WebMarkupContainer(CID_BULK_ACTION_DROPDOWN);
        bulkActionDropdown.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer(CID_BULK_ACTION_DROPDOWN_BUTTON);
        bulkActionDropdownButton.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdownButton);

        queue(new LambdaAjaxLink("bulkDelete", this::actionBulkDeleteDocuments));
        queue(new AjaxDownloadLink("bulkExport", //
                Model.of("files.zip"), //
                LoadableDetachableModel.of(this::exportDocuments)));

        queue(new SourceDocumentStateFilterPanel(CID_STATE_FILTERS,
                () -> dataProvider.getFilterState().getStates()));

        queue(confirmationDialog = new ChallengeResponseDialog("confirmationDialog"));
    }

    @SuppressWarnings("unchecked")
    public IModel<List<SourceDocument>> getModel()
    {
        return (IModel<List<SourceDocument>>) getDefaultModel();
    }

    private void actionToggleBulkChange(AjaxRequestTarget aTarget)
    {
        bulkChangeMode = !bulkChangeMode;
        selectColumns.setVisible(bulkChangeMode);
        dataProvider.refresh();
        aTarget.add(this);
    }

    private String renderFormat(String aFormatId)
    {
        return importExportService.getFormatById(aFormatId).map(FormatSupport::getName)
                .orElse("Unsupported [" + aFormatId + "]");
    }

    private String renderDate(Date aDate)
    {
        if (aDate == null) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(aDate);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        WicketUtil.ajaxFallbackFocus(aResponse, nameFilter);
    }

    private void actionApplyFilter(AjaxRequestTarget aTarget)
    {
        aTarget.add(table);
    }

    public SourceDocumentTableDataProvider getDataProvider()
    {
        return dataProvider;
    }

    @OnEvent
    public void onSourceDocumentFilterStateChanged(SourceDocumentFilterStateChanged aEvent)
    {
        aEvent.getTarget().add(table);
    }

    @OnEvent
    public void onDocumentRowSelectionChangedEvent(
            SourceDocumentTableRowSelectionChangedEvent aEvent)
    {
        aEvent.getTarget().add(table);
    }

    @OnEvent
    public void onSourceDocumentTableDeleteDocumentEvent(
            SourceDocumentTableDeleteDocumentEvent aEvent)
    {
        actionDeleteDocument(aEvent.getTarget(), aEvent.getDocument());
    }

    @OnEvent
    public void onSourceDocumentTableToggleSelectAllEvent(
            SourceDocumentTableToggleSelectAllEvent aEvent)
    {
        int changed = 0;
        boolean targetValue = aEvent.isSelectAll();
        for (var row : dataProvider.getModel().getObject()) {
            if (row.isSelected() != targetValue) {
                changed++;
                row.setSelected(targetValue);
            }
        }

        info(changed + " documents have been " + (targetValue ? "selected" : "unselected"));
        aEvent.getTarget().addChildren(getPage(), IFeedback.class);
        aEvent.getTarget().add(table);
    }

    private List<SourceDocument> getSelectedDocuments()
    {
        var selectedDocuments = dataProvider.getModel().getObject().stream() //
                .filter(SourceDocumentTableRow::isSelected) //
                .map(SourceDocumentTableRow::getDocument) //
                .collect(toList());
        return selectedDocuments;
    }

    private IResourceStream exportDocuments()
    {
        var selectedDocuments = getSelectedDocuments();
        return new PipedStreamResource(
                os -> documentService.exportSourceDocuments(os, selectedDocuments));
    }

    private void actionDeleteDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        IModel<String> documentNameModel = Model.of(aDocument.getName());
        confirmationDialog
                .setTitleModel(new StringResourceModel("DeleteDocumentDialog.title", this));
        confirmationDialog
                .setChallengeModel(new StringResourceModel("DeleteDocumentDialog.text", this)
                        .setParameters(documentNameModel));
        confirmationDialog.setResponseModel(documentNameModel);
        confirmationDialog
                .setConfirmAction($ -> actionConfirmDeleteDocuments($, asList(aDocument)));
        confirmationDialog.show(aTarget);
    }

    private void actionBulkDeleteDocuments(AjaxRequestTarget aTarget)
    {
        var selectedDocuments = getSelectedDocuments();

        if (selectedDocuments.isEmpty()) {
            info("No documents have been selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var project = selectedDocuments.get(0).getProject();

        IModel<String> projectNameModel = Model.of(project.getName());
        confirmationDialog
                .setTitleModel(new StringResourceModel("BulkDeleteDocumentDialog.title", this));
        confirmationDialog
                .setChallengeModel(new StringResourceModel("BulkDeleteDocumentDialog.text", this)
                        .setParameters(selectedDocuments.size(), projectNameModel));
        confirmationDialog.setResponseModel(projectNameModel);
        confirmationDialog
                .setConfirmAction($ -> actionConfirmDeleteDocuments($, selectedDocuments));
        confirmationDialog.show(aTarget);
    }

    private void actionConfirmDeleteDocuments(AjaxRequestTarget aTarget,
            List<SourceDocument> aSelectedDocuments)
    {
        var count = 0;
        for (SourceDocument doc : aSelectedDocuments) {
            try {
                documentService.removeSourceDocument(doc);
                count++;
            }
            catch (IOException e) {
                WicketExceptionUtil.handleException(LOG, this, aTarget, e);
            }
        }

        if (aSelectedDocuments.size() == 1) {
            success("Document [" + aSelectedDocuments.get(0).getName() + "] has been deleted");
        }
        else {
            success(count + " documents have been deleted");
        }

        aTarget.addChildren(getPage(), IFeedback.class);
        dataProvider.refresh();
        aTarget.add(table);
    }

    DocumentService getDocumentService()
    {
        return documentService;
    }
}
