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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.perdoc;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PerDocumentAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.DefaultRefreshingView;

public class PerDocumentAgreementTable
    extends GenericPanel<PerDocumentAgreementResult>
{
    private static final long serialVersionUID = 571396822546125376L;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static int ANNOTATORS_LIMIT = 3;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private final RefreshingView<SourceDocument> rows;
    private final Map<String, String> userNameCache = new HashMap<>();

    public PerDocumentAgreementTable(String aId, IModel<PerDocumentAgreementResult> aModel,
            DefaultAgreementTraits aTraits)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        var documents = getModelObject().getDocuments().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();

        queue(new Label("meanScore",
                aModel.map($ -> format("%.4f", $.getAgreementScoreStats().getMean()))));
        queue(new Label("minScore",
                aModel.map($ -> format("%.4f", $.getAgreementScoreStats().getMin()))));
        queue(new Label("maxScore",
                aModel.map($ -> format("%.4f", $.getAgreementScoreStats().getMax()))));
        queue(new Label("varianceScore",
                aModel.map($ -> format("%.4f", $.getAgreementScoreStats().getVariance()))));

        rows = new DefaultRefreshingView<SourceDocument>("rows", Model.ofList(documents))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<SourceDocument> aRowItem)
            {
                var doc = aRowItem.getModelObject();
                aRowItem.add(new Label("documentName", aRowItem.getModelObject().getName()));

                var agreementSummary = aModel.getObject().getResult(doc);
                aRowItem.add(new Label("score", format("%.2f", agreementSummary.getAgreement())));

                var casGroupIds = agreementSummary.getCasGroupIds();
                var annotators = new Label("annotators", renderAnnotators(casGroupIds));
                if (casGroupIds.size() > ANNOTATORS_LIMIT) {
                    annotators.add(AttributeModifier.replace("title", casGroupIds.stream() //
                            .map(PerDocumentAgreementTable.this::getUserName) //
                            .sorted() //
                            .collect(joining(", "))));
                }
                aRowItem.add(annotators);

                // Odd/even coloring is reversed here to account for the header row at index 0
                aRowItem.add(new AttributeAppender("class",
                        (aRowItem.getIndex() % 2 == 0) ? "odd" : "even"));
            }
        };

        add(rows);
    }

    private String renderAnnotators(List<String> aCasGroupIds)
    {
        var head = aCasGroupIds.stream() //
                .map(this::getUserName) //
                .sorted() //
                .limit(ANNOTATORS_LIMIT) //
                .collect(joining(", "));

        if (aCasGroupIds.size() <= ANNOTATORS_LIMIT) {
            return head;
        }

        return head + " + " + (aCasGroupIds.size() - ANNOTATORS_LIMIT);
    }

    private String getUserName(String aUserName)
    {
        return userNameCache.computeIfAbsent(aUserName,
                $ -> Optional.ofNullable(userRepository.getUserOrCurationUser($))
                        .map(User::getUiName).orElse($));
    }
}
