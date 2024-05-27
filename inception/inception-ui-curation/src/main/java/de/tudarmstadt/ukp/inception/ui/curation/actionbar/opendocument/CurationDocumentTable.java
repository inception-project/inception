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
package de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.KEYDOWN_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.KeyCodes.ENTER;
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.CREATED;
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.NAME;
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.STATE;
import static de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationDocumentTableSortKeys.UPDATED;
import static java.time.Duration.ofMillis;
import static org.apache.wicket.event.Broadcast.BUBBLE;

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
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentFilterStateChanged;
import de.tudarmstadt.ukp.inception.annotation.filters.SourceDocumentStateFilterPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLambdaColumn;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public class CurationDocumentTable
    extends Panel
{
    private static final long serialVersionUID = 3993790906387166039L;

    private static final String CID_DATA_TABLE = "dataTable";
    private static final String CID_STATE_FILTERS = "stateFilters";

    private final DataTable<SourceDocument, CurationDocumentTableSortKeys> table;
    private TextField<String> nameFilter;

    public CurationDocumentTable(String aId, IModel<List<SourceDocument>> aModel)
    {
        super(aId, aModel);
        setOutputMarkupId(true);

        var dataProvider = new CurationDocumentTableDataProvider(aModel);

        var columns = new ArrayList<IColumn<SourceDocument, CurationDocumentTableSortKeys>>();
        columns.add(new SymbolLambdaColumn<>(new ResourceModel("DocumentState"), STATE,
                item -> item.getState()));
        columns.add(new CurationDocumentOpenActionColumn(this, new ResourceModel("DocumentName"),
                NAME));
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentCreated"), CREATED,
                $ -> renderDate($.getCreated())));
        columns.add(new LambdaColumn<>(new ResourceModel("DocumentUpdated"), UPDATED,
                $ -> renderDate($.getUpdated())));

        table = new DataTable<>(CID_DATA_TABLE, columns, dataProvider, 100);
        table.addTopToolbar(new AjaxNavigationToolbar(table));
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<>(table, dataProvider));
        queue(table);

        queue(new SourceDocumentStateFilterPanel(CID_STATE_FILTERS,
                () -> dataProvider.getFilterState().getStates(), ANNOTATION_IN_PROGRESS,
                ANNOTATION_FINISHED, CURATION_IN_PROGRESS, CURATION_FINISHED));

        nameFilter = new TextField<>("nameFilter",
                PropertyModel.of(dataProvider.getFilterState(), "documentName"), String.class);
        nameFilter.setOutputMarkupPlaceholderTag(true);
        nameFilter.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        nameFilter.add(new LambdaAjaxFormComponentUpdatingBehavior(KEYDOWN_EVENT, this::actionOpen)
                .withKeyCode(ENTER));
        queue(nameFilter);
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

    private void actionOpen(AjaxRequestTarget aTarget)
    {
        if (table.getDataProvider().size() == 1) {
            var document = table.getDataProvider().iterator(0, 1).next();
            send(this, BUBBLE, new CurationDocumentOpenDocumentEvent(aTarget, document));
        }
    }

    @OnEvent
    public void onSourceDocumentFilterStateChanged(SourceDocumentFilterStateChanged aEvent)
    {
        aEvent.getTarget().add(this);
    }

    private String renderDate(Date aDate)
    {
        if (aDate == null) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(aDate);
    }
}
