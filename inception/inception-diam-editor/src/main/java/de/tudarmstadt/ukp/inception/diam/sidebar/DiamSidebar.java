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
package de.tudarmstadt.ukp.inception.diam.sidebar;

import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;

public class DiamSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3062641971181309172L;

    private final String userPreferencesKey;

    private DiamAnnotationBrowser browser;
    private ContextMenu contextMenu;

    public DiamSidebar(String aId, AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPageBase2 aAnnotationPage, String aUserPreferencesKey)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        userPreferencesKey = aUserPreferencesKey;

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        add(browser = new DiamAnnotationBrowser("vis", userPreferencesKey, contextMenu));
    }

    @OnEvent
    public void onDocumentOpenedEvent(DocumentOpenedEvent aEvent)
    {
        browser = (DiamAnnotationBrowser) browser
                .replaceWith(new DiamAnnotationBrowser("vis", userPreferencesKey, contextMenu));
    }
}
