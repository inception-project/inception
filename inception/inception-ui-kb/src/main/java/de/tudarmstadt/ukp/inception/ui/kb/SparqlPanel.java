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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.eclipse.rdf4j.query.explanation.Explanation.Level.Timed;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

public class SparqlPanel
    extends GenericPanel<KnowledgeBase>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -8435410869046010021L;

    private @SpringBean KnowledgeBaseService knowledgeBaseService;

    private WebMarkupContainer resultsContainer;
    private DataTable<Map<String, String>, Void> results;

    public SparqlPanel(String aId, IModel<KnowledgeBase> aModel)
    {
        super(aId, aModel);

        var form = new Form<QueryModel>("form", new CompoundPropertyModel<>(new QueryModel()));
        queue(form);
        queue(new TextArea<>("query"));
        queue(new LambdaAjaxSubmitLink<QueryModel>("runQuery", this::actionQuery));
        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));
        var level = new DropDownChoice<Explanation.Level>("level");
        level.setChoices(asList(Explanation.Level.values()));
        level.setNullValid(true);
        level.setChoiceRenderer(new EnumChoiceRenderer<>(level));
        queue(level);
        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        queue(resultsContainer);
        results = new DataTable<>("results", emptyList(), new TupleQueryResultDataProvider(), 10);
        queue(results);
        queue(new NumberTextField<Integer>("timeout").setMinimum(1).setRequired(true));
        queue(new Label("explanation", form.getModel().map(m -> m.explanation)) //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(form.getModel().map(m -> m.explanation != null))));
        queue(new Label("duration", form.getModel().map(m -> m.duration)) //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(form.getModel().map(m -> m.duration >= 0))));
        queue(new Label("explanationDuration", form.getModel().map(m -> m.explanationDuration)) //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(form.getModel().map(m -> m.explanationDuration >= 0))));
    }

    private void actionQuery(AjaxRequestTarget aTarget, Form<QueryModel> aForm)
    {
        var model = aForm.getModelObject();

        var columns = new ArrayList<IColumn<Map<String, String>, Void>>();

        var dataProvider = knowledgeBaseService.read(getModelObject(), conn -> {
            var tupleQuery = conn.prepareTupleQuery(model.query);

            if (model.level != null) {
                var start = currentTimeMillis();
                model.explanation = tupleQuery.explain(model.level).toString();
                model.explanationDuration = currentTimeMillis() - start;
                LOG.info("SPARQL explanation: {}", model.explanation);
            }
            else {
                model.explanationDuration = -1;
                model.explanation = null;
            }

            try {
                tupleQuery.setMaxExecutionTime(model.timeout);
                var start = currentTimeMillis();
                var tupleResult = tupleQuery.evaluate();
                for (var bindingName : tupleResult.getBindingNames()) {
                    columns.add(new LambdaColumn<>(Model.of(bindingName), $ -> $.get(bindingName)));
                }

                var data = new TupleQueryResultDataProvider(tupleResult);
                model.duration = currentTimeMillis() - start;
                return data;
            }
            catch (QueryEvaluationException e) {
                error("SPARQL query evaluation failed: " + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
                return new TupleQueryResultDataProvider();
            }
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

        String query = """
                SELECT ?s ?p ?o WHERE {
                  ?s ?p ?o .
                }
                """;

        Explanation.Level level = Timed;
        String explanation;
        int timeout = 60;
        long explanationDuration = -1l;
        long duration = -1l;
    }
}
