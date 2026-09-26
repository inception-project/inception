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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.docinfo;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarContext;
import org.wicketstuff.event.annotation.OnEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.ActiveEditorChangedEvent;

public class DocumentInfoSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 6127948490101336779L;

    public DocumentInfoSidebar(String aId, SidebarContext aContext)
    {
        super(aId, aContext);

        var model = getModel();
        add(new Label("id", PropertyModel.of(model, "document.name")));
        add(new Label("format", PropertyModel.of(model, "document.format")));
        add(new Label("timestamp", PropertyModel.of(model, "document.timestamp")));
    }

    @OnEvent
    public void onActiveEditorChanged(ActiveEditorChangedEvent aEvent)
    {
        if (aEvent.getRequestHandler() != null) {
            aEvent.getRequestHandler().add(this);
        }
    }
}
