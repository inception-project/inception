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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class AnnotatorColumn
    extends LambdaColumn<DocumentMatrixRow, Void>
{
    private static final long serialVersionUID = 8324173231787296215L;

    public AnnotatorColumn(String aUsername)
    {
        super(Model.of(aUsername), row -> row.getAnnotationDocument(aUsername));
    }

    @Override
    public void populateItem(Item<ICellPopulator<DocumentMatrixRow>> aItem, String aComponentId,
            IModel<DocumentMatrixRow> aRowModel)
    {
        IModel<AnnotationDocument> annDocument = (IModel<AnnotationDocument>) getDataModel(
                aRowModel);
        Label state = new Label(aComponentId,
                stateSymbol(annDocument.map(AnnotationDocument::getState).orElse(NEW).getObject()));
        state.setEscapeModelStrings(false);
        state.add(new AttributeAppender("style", "cursor: pointer", ";"));
        state.add(AjaxEventBehavior.onEvent("click", _target -> actionStateChange(_target,
                aRowModel.getObject().getSourceDocument(), getDisplayModel().getObject())));

        aItem.add(state);
    }

    public abstract void actionStateChange(AjaxRequestTarget aTarget,
            SourceDocument aSourceDocument, String aUsername);

    private String stateSymbol(AnnotationDocumentState aDocState)
    {
        switch (aDocState) {
        case NEW:
            return "<i class=\"far fa-circle\"></i>";
        case IN_PROGRESS:
            return "<i class=\"far fa-play-circle\"></i>";
        case FINISHED:
            return "<i class=\"far fa-check-circle\"></i>";
        case IGNORE:
            return "<i class=\"fas fa-lock\"></i>";
        }

        return "";
    }
}
