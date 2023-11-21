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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CurationDocumentOpenActionColumn
    extends AbstractColumn<SourceDocument, CurationDocumentTableSortKeys>
{
    public static final String FID_OPEN_DOCUMENT_COLUMN = "openDocumentColumn";

    public static final String CID_OPEN = "open";

    private static final long serialVersionUID = 8324173231787296215L;

    private MarkupContainer fragmentProvider;

    public CurationDocumentOpenActionColumn(MarkupContainer aFragmentProvider,
            IModel<String> aTitle, CurationDocumentTableSortKeys aSortProperty)
    {
        super(aTitle, aSortProperty);
        fragmentProvider = aFragmentProvider;
    }

    @Override
    public void populateItem(Item<ICellPopulator<SourceDocument>> aItem, String aComponentId,
            IModel<SourceDocument> aRowModel)
    {
        var fragment = new Fragment(aComponentId, FID_OPEN_DOCUMENT_COLUMN, fragmentProvider);
        fragment.queue(new Label("name", aRowModel.map(SourceDocument::getName)));
        aItem.add(AttributeModifier.replace("role", "button"));
        aItem.add(AjaxEventBehavior.onEvent("click",
                _target -> actionOpenDocument(_target, aItem, aRowModel.getObject())));
        aItem.add(fragment);
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget, Component aItem,
            SourceDocument aDocument)
    {
        aItem.send(aItem, BUBBLE, new CurationDocumentOpenDocumentEvent(aTarget, aDocument));
    }
}
