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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import de.tudarmstadt.ukp.inception.bootstrap.dialog.ConfirmationDialogContentPanel_ImplBase;

public class MergeAllConfirmationDialogPanel
    extends ConfirmationDialogContentPanel_ImplBase
{
    private static final long serialVersionUID = -1716497452466338113L;

    /**
     * @param aId
     *            the component ID
     * @param aCount
     *            the number of the annotator's annotations on the layer - an upper bound for the
     *            number of annotations that are merged
     * @param aLayerName
     *            the UI name of the layer
     * @param aAnnotatorName
     *            the UI name of the annotator
     */
    public MergeAllConfirmationDialogPanel(String aId, long aCount, String aLayerName,
            String aAnnotatorName)
    {
        super(aId);
        queue(new Label("message", new StringResourceModel("message")
                .setParameters(Model.of(aCount), Model.of(aLayerName), Model.of(aAnnotatorName))));
    }
}
