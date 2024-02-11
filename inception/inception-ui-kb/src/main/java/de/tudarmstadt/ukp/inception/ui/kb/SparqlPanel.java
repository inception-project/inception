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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

public class SparqlPanel
    extends GenericPanel<KnowledgeBase>
{
    private static final long serialVersionUID = -8435410869046010021L;

    private @SpringBean KnowledgeBaseService knowledgeBaseService;

    private WebMarkupContainer resultsContainer;
    private DataTable<Map<String, String>, Void> results;

    public SparqlPanel(String aId, IModel<KnowledgeBase> aModel)
    {
        super(aId, aModel);

        queue(new Form<QueryModel>("form", new CompoundPropertyModel<>(new QueryModel())));
        queue(new TextArea<>("query"));
        queue(new LambdaAjaxSubmitLink<QueryModel>("runQuery", this::actionQuery));
        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));
        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        queue(resultsContainer);
        results = new DataTable<>("results", Collections.emptyList(),
                new TupleQueryResultDataProvider(), 10);
        queue(results);
    }

    private void actionQuery(AjaxRequestTarget aTarget, Form<QueryModel> aForm)
    {
        var model = aForm.getModelObject();

        var columns = new ArrayList<IColumn<Map<String, String>, Void>>();

        var dataProvider = knowledgeBaseService.read(getModelObject(), conn -> {
            var tupleQuery = conn.prepareTupleQuery(model.query);

            var tupleResult = tupleQuery.evaluate();

            for (var bindingName : tupleResult.getBindingNames()) {
                columns.add(new LambdaColumn<>(Model.of(bindingName), $ -> $.get(bindingName)));
            }

            return new TupleQueryResultDataProvider(tupleResult);
        });

        var table = new DataTable<>("results", columns, dataProvider, 10);
        table.setOutputMarkupId(true);
        table.addTopToolbar(new AjaxNavigationToolbar(table));
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<Void>(table, dataProvider));
        results = (DataTable<Map<String, String>, Void>) results.replaceWith(table);

        aTarget.add(resultsContainer);
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    private static class QueryModel
        implements Serializable
    {
        private static final long serialVersionUID = -1407131006099282400L;

        String query;
    }
}
