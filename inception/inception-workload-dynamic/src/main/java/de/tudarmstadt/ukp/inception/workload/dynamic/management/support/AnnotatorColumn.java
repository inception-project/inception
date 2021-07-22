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
package de.tudarmstadt.ukp.inception.workload.dynamic.management.support;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueItem;
import de.tudarmstadt.ukp.inception.workload.dynamic.support.AnnotationQueueSortKeys;

public class AnnotatorColumn
    extends LambdaColumn<AnnotationQueueItem, AnnotationQueueSortKeys>
{
    private static final long serialVersionUID = 8324173231787296215L;

    public AnnotatorColumn(IModel<String> aTitle)
    {
        super(aTitle, AnnotationQueueItem::getAnnotationDocuments);
    }

    @Override
    public void populateItem(Item<ICellPopulator<AnnotationQueueItem>> aItem, String aComponentId,
            IModel<AnnotationQueueItem> aRowModel)
    {
        @SuppressWarnings("unchecked")
        IModel<List<AnnotationDocument>> annotators = ((IModel<List<AnnotationDocument>>) getDataModel(
                aRowModel));
        annotators = annotators.map(docs -> docs.stream() //
                .filter(doc -> doc.getState() != AnnotationDocumentState.NEW) //
                .collect(toList()));

        aItem.add(new AnnotationStateList(aComponentId, annotators,
                aRowModel.getObject().getAbandonationTimeout()));
    }
}
