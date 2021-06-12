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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnSelectionChangedEvent;

public class UserSelectToolbar
    extends AbstractToolbar
{
    private static final long serialVersionUID = 8850551593688910044L;

    public UserSelectToolbar(IModel<Set<String>> aSelection, DataTable<DocumentMatrixRow, ?> aTable)
    {
        super(aSelection, aTable);

        RefreshingView<IColumn<DocumentMatrixRow, Void>> headers = new RefreshingView<IColumn<DocumentMatrixRow, Void>>(
                "headers", Model.of(aTable.getColumns()))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<IColumn<DocumentMatrixRow, Void>>> getItemModels()
            {
                List<IModel<IColumn<DocumentMatrixRow, Void>>> columnsModels = new LinkedList<>();

                for (IColumn<DocumentMatrixRow, Void> column : getTable().getColumns()) {
                    columnsModels.add(Model.of(column));
                }

                return columnsModels.iterator();
            }

            @Override
            protected void populateItem(Item<IColumn<DocumentMatrixRow, Void>> item)
            {
                final IColumn<DocumentMatrixRow, ?> column = item.getModelObject();

                // if (column instanceof IStyledColumn) {
                // CssAttributeBehavior cssAttributeBehavior = new DataTable.CssAttributeBehavior()
                // {
                // private static final long serialVersionUID = 1L;
                //
                // @Override
                // protected String getCssClass()
                // {
                // return ((IStyledColumn<?, S>) column).getCssClass();
                // }
                // };
                //
                // header.add(cssAttributeBehavior);
                // }
                //
                // if (column.getHeaderColspan() > 1) {
                // header.add(AttributeModifier.replace("colspan", column.getHeaderColspan()));
                // }
                //
                // if (column.getHeaderRowspan() > 1) {
                // header.add(AttributeModifier.replace("rowspan", column.getHeaderRowspan()));
                // }

                WebMarkupContainer header = new WebMarkupContainer("header");
                item.add(header);
                item.setRenderBodyOnly(true);

                CheckBox selected = new CheckBox("selected");
                selected.setVisible(column instanceof AnnotatorColumn);

                if (column instanceof AnnotatorColumn) {
                    AnnotatorColumn annotatorColumn = (AnnotatorColumn) column;
                    String username = annotatorColumn.getDisplayModel().getObject();
                    selected.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                            _target -> actionSelect(_target, username, selected)));
                    selected.setModel(Model.of(getSelection().contains(username)));
                }

                header.add(selected);
            }
        };
        add(headers);
    }

    private void actionSelect(AjaxRequestTarget aTarget, String aUsername, CheckBox aCheckbox)
    {
        if (aCheckbox.getModelObject()) {
            getSelection().add(aUsername);
        }
        else {
            getSelection().remove(aUsername);
        }

        send(this, BUBBLE,
                new AnnotatorColumnSelectionChangedEvent(aTarget, aUsername, aCheckbox.getModel()));
    }

    @SuppressWarnings("unchecked")
    public Set<String> getSelection()
    {
        return (Set<String>) getDefaultModelObject();
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
