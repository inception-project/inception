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
package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import static de.tudarmstadt.ukp.inception.workload.ui.WorkloadCssClasses.CSS_CLASS_CURATION_NOT_READY;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLambdaColumn;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * The document state column of the dynamic workload management page. In addition to rendering the
 * state symbol, it marks documents on which curation has been started although annotation on them
 * is not (or no longer) complete - the counterpart of the marker on the curation cell of the matrix
 * workload management page.
 */
public class DocumentStateColumn
    extends SymbolLambdaColumn<AnnotationQueueItem, AnnotationQueueSortKeys>
{
    private static final long serialVersionUID = -4344518995089564027L;

    public DocumentStateColumn(IModel<String> aDisplayModel, AnnotationQueueSortKeys aSortProperty)
    {
        super(aDisplayModel, aSortProperty, AnnotationQueueItem::getState);
    }

    @Override
    public void populateItem(Item<ICellPopulator<AnnotationQueueItem>> aItem, String aComponentId,
            IModel<AnnotationQueueItem> aRowModel)
    {
        super.populateItem(aItem, aComponentId, aRowModel);

        var row = aRowModel.getObject();

        // Marking only - the document keeps its state and stays editable. Readiness is an entry
        // condition for curation, not a continuous invariant.
        if (row.isInCuration() && !row.isReadyForCuration()) {
            var cell = aItem.get(aComponentId);
            cell.add(new CssClassNameAppender(CSS_CLASS_CURATION_NOT_READY));
            cell.add(new AttributeModifier("title", Strings.getString("curation-not-ready.hint")));
        }
    }
}
