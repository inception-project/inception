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
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.jquery.core.renderer.ITextRenderer;
import org.wicketstuff.jquery.core.renderer.TextRenderer;

import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.ui.kb.feature.KnowledgeBaseItemAutoCompleteField;

public class IRIValueEditor
    extends ValueEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(IRIValueEditor.class);

    private static final long serialVersionUID = -1646737090861147804L;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean ConceptLinkingService clService;

    private KnowledgeBaseItemAutoCompleteField value;
    private IModel<KBStatement> statement;
    private IModel<KBProperty> property;
    private IModel<KnowledgeBase> kb;

    public IRIValueEditor(String aId, IModel<KBStatement> aModel, IModel<KBProperty> aProperty,
            IModel<KnowledgeBase> aKB)
    {
        super(aId, CompoundPropertyModel.of(aModel));
        statement = aModel;
        property = aProperty;
        kb = aKB;

        ITextRenderer<KBHandle> renderer = new TextRenderer<KBHandle>("uiLabel")
        {
            private static final long serialVersionUID = 6523122841966543569L;

            @Override
            public String getText(KBHandle aObject)
            {
                // For the feature value editor, we do *not* want IRIs to be abbreviated to a human
                // readable name.
                if (aObject != null && StringUtils.isBlank(aObject.getName())) {
                    return aObject.getIdentifier();
                }

                return super.getText(aObject);
            }
        };

        value = new KnowledgeBaseItemAutoCompleteField("value", this::listChoices, renderer);
        // Explicitly constructing this as a LambdaModelAdapter<Object> is necessary to avoid
        // a ClassCastException when the AutoCompleteField sends a String value which it (for
        // whatever reason sometimes) fails to map to a KBHandle.
        value.setDefaultModel(
                new LambdaModelAdapter<Object>(this::getKBHandleModel, this::setKBHandleModel));
        value.setOutputMarkupId(true);
        // Statement values cannot be null/empty - well, in theory they could be the empty string,
        // but we treat the empty string as null
        value.setRequired(true);
        add(value);
    }

    private List<KBHandle> listChoices(String aInput)
    {
        if (aInput == null) {
            return emptyList();
        }

        List<KBHandle> choices;
        try {
            KnowledgeBase kbase = kb.getObject();

            choices = clService.getLinkingInstancesInKBScope(kbase.getRepositoryId(),
                    property.getObject().getRange(), ConceptFeatureValueType.ANY_OBJECT, aInput,
                    null, -1, null, kbase.getProject());
        }
        catch (Exception e) {
            choices = asList(new KBHandle("http://ERROR", "ERROR", e.getMessage(), "en"));
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            LOG.error("An error occurred while retrieving entity candidates", e);
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }

        // In case the user is entering a IRI, add that as the first choice - maybe the user wants
        // to manually enter an IRI
        try {
            if (URIUtil.isValidURIReference(aInput)) {
                choices.add(0, new KBHandle(aInput));
            }
        }
        catch (IllegalArgumentException e) {
            // Ignore
        }

        return choices;
    }

    @Override
    public Component getFocusComponent()
    {
        return value;
    }

    private void setKBHandleModel(Object aValue)
    {
        if (aValue == null) {
            statement.getObject().setValue(null);
            return;
        }

        if (!(aValue instanceof KBHandle)) {
            return;
        }

        KBHandle handle = (KBHandle) aValue;
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        statement.getObject().setValue(vf.createIRI(handle.getIdentifier()));
    }

    private KBHandle getKBHandleModel()
    {
        Object statementValue = statement.getObject().getValue();
        if (statementValue instanceof IRI) {
            String iri = ((IRI) statementValue).stringValue();
            return kbService
                    .read(kb.getObject(),
                            conn -> SPARQLQueryBuilder.forItems(kb.getObject()).withIdentifier(iri)
                                    .retrieveLabel().asHandle(conn, false))
                    .orElse(new KBHandle(iri));
        }
        return null;
    }
}
