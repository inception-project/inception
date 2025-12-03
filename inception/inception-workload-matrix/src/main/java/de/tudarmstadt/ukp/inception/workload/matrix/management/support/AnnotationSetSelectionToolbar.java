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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnSelectionChangedEvent;

public class AnnotationSetSelectionToolbar
    extends AbstractToolbar
{
    private static final long serialVersionUID = 8850551593688910044L;

    public AnnotationSetSelectionToolbar(IModel<Set<AnnotationSet>> aSelection,
            DataTable<DocumentMatrixRow, ?> aTable)
    {
        super(aSelection, aTable);

        var headers = new RefreshingView<IColumn<DocumentMatrixRow, Void>>("headers",
                Model.of(aTable.getColumns()))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<IColumn<DocumentMatrixRow, Void>>> getItemModels()
            {
                var columnsModels = new LinkedList<IModel<IColumn<DocumentMatrixRow, Void>>>();

                for (var column : getTable().getColumns()) {
                    columnsModels.add(Model.of(column));
                }

                return columnsModels.iterator();
            }

            @Override
            protected void populateItem(Item<IColumn<DocumentMatrixRow, Void>> item)
            {
                final IColumn<DocumentMatrixRow, ?> column = item.getModelObject();

                var header = new WebMarkupContainer("header");
                item.add(header);
                item.setRenderBodyOnly(true);

                var selected = new CheckBox("selected");

                if (column instanceof DocumentMatrixAnnotatorColumn annotatorColumn) {
                    var username = annotatorColumn.getAnnotationSet();
                    selected.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                            _target -> actionSelect(_target, username, selected)));
                    selected.setModel(Model.of(getSelection().contains(username)));
                }
                else if (column instanceof DocumentMatrixCuratorColumn curatorColumn) {
                    selected.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                            _target -> actionSelect(_target, CURATION_SET, selected)));
                    selected.setModel(Model.of(getSelection().contains(CURATION_SET)));
                }
                else {
                    selected.setVisible(false);
                }

                header.add(selected);
            }
        };
        add(headers);
    }

    private void actionSelect(AjaxRequestTarget aTarget, AnnotationSet aAnnSet, CheckBox aCheckbox)
    {
        if (aCheckbox.getModelObject()) {
            getSelection().add(aAnnSet);
        }
        else {
            getSelection().remove(aAnnSet);
        }

        send(this, BUBBLE,
                new AnnotatorColumnSelectionChangedEvent(aTarget, aAnnSet, aCheckbox.getModel()));
    }

    @SuppressWarnings("unchecked")
    public Set<AnnotationSet> getSelection()
    {
        return (Set<AnnotationSet>) getDefaultModelObject();
    }

    @SuppressWarnings("unchecked")
    public IModel<Set<String>> getSelectionModel()
    {
        return (IModel<Set<String>>) getDefaultModel();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DataTable<DocumentMatrixRow, Void> getTable()
    {
        return (DataTable<DocumentMatrixRow, Void>) super.getTable();
    }
}
