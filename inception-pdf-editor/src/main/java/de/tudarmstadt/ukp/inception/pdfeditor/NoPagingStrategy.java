/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.DefaultPagingNavigator;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.PagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.Unit;

public class NoPagingStrategy
    implements PagingStrategy
{
    private static final long serialVersionUID = 1589886937787735472L;

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        return asList(new Unit(0, 0, aCas.getDocumentText().length()));
    }

    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        EmptyPanel emptyPanel = new EmptyPanel(aId);
        // Just to avoid errors when re-rendering this is requested in an AJAX request
        emptyPanel.setOutputMarkupId(true);
        return emptyPanel;
    }

    @Override
    public DefaultPagingNavigator createPageNavigator(String aId, AnnotationPageBase aPage)
    {
        DefaultPagingNavigator navi = new DefaultPagingNavigator(aId, aPage);
        navi.setOutputMarkupPlaceholderTag(true);
        navi.setVisible(false);
        return navi;
    }
}
