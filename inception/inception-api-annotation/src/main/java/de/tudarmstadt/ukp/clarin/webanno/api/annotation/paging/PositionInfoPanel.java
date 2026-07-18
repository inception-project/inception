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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class PositionInfoPanel
    extends Panel
{
    private static final long serialVersionUID = 4131266760920882496L;

    private static final String MID_UNIT_POSITION = "unitPosition";
    private static final String MID_DOCUMENT_POSITION = "documentPosition";

    /**
     * @param aId
     *            component id
     * @param aModel
     *            the annotator state supplying the current position
     * @param aUnitName
     *            plural noun for the paging unit, e.g. {@code "sentences"}, {@code "lines"} or
     *            {@code "blocks"}
     */
    public PositionInfoPanel(String aId, IModel<AnnotatorState> aModel, String aUnitName)
    {
        super(aId, aModel);

        // Mirrors the AJAX re-rendering contract the previous position Label had: callers re-add
        // this component to the AjaxRequestTarget by id when paging changes.
        setOutputMarkupPlaceholderTag(true);

        add(new Label(MID_UNIT_POSITION,
                aModel.map(state -> String.format("%d-%d / %d %s", state.getFirstVisibleUnitIndex(),
                        state.getLastVisibleUnitIndex(), state.getUnitCount(), aUnitName))));

        add(new Label(MID_DOCUMENT_POSITION, aModel.map(state -> String.format("[doc %d / %d]",
                state.getDocumentIndex() + 1, state.getNumberOfDocuments()))));
    }
}
