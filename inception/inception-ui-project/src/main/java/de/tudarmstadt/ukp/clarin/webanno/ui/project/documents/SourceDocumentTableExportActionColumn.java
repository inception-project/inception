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

import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class SourceDocumentTableExportActionColumn
    extends AbstractColumn<SourceDocumentTableRow, SourceDocumentTableSortKeys>
{
    private static final long serialVersionUID = 8324173231787296215L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String FID_EXPORT_DOCUMENT_COLUMN = "exportDocumentColumn";

    private static final String CID_EXPORT = "export";

    private SourceDocumentTable table;

    public SourceDocumentTableExportActionColumn(SourceDocumentTable aTable)
    {
        super(null);
        table = aTable;
    }

    @Override
    public void populateItem(Item<ICellPopulator<SourceDocumentTableRow>> aItem,
            String aComponentId, IModel<SourceDocumentTableRow> aRowModel)
    {
        var fragment = new Fragment(aComponentId, FID_EXPORT_DOCUMENT_COLUMN, table);

        fragment.queue(new AjaxDownloadLink(CID_EXPORT,
                LoadableDetachableModel.of(() -> export(aRowModel.getObject().getDocument()))));
        aItem.add(fragment);
    }

    private IResourceStream export(SourceDocument aDocument)
    {
        try {
            return table.getDocumentStorageService().getSourceDocumentResourceStream(aDocument);
        }
        catch (Exception e) {
            var handler = RequestCycle.get().find(AjaxRequestTarget.class);
            WicketExceptionUtil.handleException(LOG, table.getSession(), handler.orElse(null), e);
            return null;
        }
    }
}
