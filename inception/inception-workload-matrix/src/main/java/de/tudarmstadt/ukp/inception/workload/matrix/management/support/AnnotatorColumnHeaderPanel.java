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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;

/**
 * Header for an annotator column in the document matrix. Shows the data owner's name and, for data
 * owners carrying a marker (former annotator, deactivated account or missing/deleted account), a
 * warning icon explaining their status (shown as a tooltip on hover).
 */
public class AnnotatorColumnHeaderPanel
    extends Panel
{
    private static final long serialVersionUID = 1L;

    public AnnotatorColumnHeaderPanel(String aId, AnnotationSet aAnnotationSet)
    {
        super(aId);

        queue(new Label("name", aAnnotationSet.name()));

        // The data owner may carry any marker (FORMER_ANNOTATOR, DEACTIVATED, MISSING), so the
        // tooltip is resolved per-marker rather than assuming "former annotator".
        var aMarker = aAnnotationSet.marker();
        var marker = new WebMarkupContainer("marker");
        marker.setVisible(aMarker != null);
        if (aMarker != null) {
            marker.add(AttributeModifier.replace("title",
                    new ResourceModel("marker." + aMarker.name())));
        }
        queue(marker);
    }
}
