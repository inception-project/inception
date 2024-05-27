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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class SourceDocumentTableDeleteActionColumn
    extends AbstractColumn<SourceDocumentTableRow, SourceDocumentTableSortKeys>
{
    public static final String FID_DELETE_DOCUMENT_COLUMN = "deleteDocumentColumn";

    private static final long serialVersionUID = 8324173231787296215L;

    private MarkupContainer fragmentProvider;

    public SourceDocumentTableDeleteActionColumn(MarkupContainer aFragmentProvider)
    {
        super(null);
        fragmentProvider = aFragmentProvider;
    }

    @Override
    public void populateItem(Item<ICellPopulator<SourceDocumentTableRow>> aItem,
            String aComponentId, IModel<SourceDocumentTableRow> aRowModel)
    {
        var fragment = new Fragment(aComponentId, FID_DELETE_DOCUMENT_COLUMN, fragmentProvider);
        fragment.queue(new LambdaAjaxLink("delete",
                $ -> actionDeleteDocument($, aItem, aRowModel.getObject().getDocument())));
        aItem.add(fragment);
    }

    private void actionDeleteDocument(AjaxRequestTarget aTarget, Component aItem,
            SourceDocument aDocument)
    {
        aItem.send(aItem, BUBBLE, new SourceDocumentTableDeleteDocumentEvent(aTarget, aDocument));
    }
}
