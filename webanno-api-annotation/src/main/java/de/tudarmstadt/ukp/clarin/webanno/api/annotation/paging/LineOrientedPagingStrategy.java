/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;

public class LineOrientedPagingStrategy
    implements PagingStrategy
{
    private static final long serialVersionUID = -991967885210129525L;

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        // We need to preserve all tokens so we can add a +1 for the line breaks of empty lines.
        String[] lines = splitPreserveAllTokens(aCas.getDocumentText(), '\n');
        
        List<Unit> units = new ArrayList<>();
        int beginOffset = 0;
        for (int i = 0; i < Math.min(lines.length, aLastIndex); i++) {
            
            if (i >= aFirstIndex) {
                units.add(new Unit(i, beginOffset, beginOffset + lines[i].length()));
            }
            
            // The +1 below accounts for the line break which is not included in the token
            beginOffset += lines[i].length() + 1;
        }
        
        return units;
    }
    
    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        Label label = new Label(aId, () -> {
            AnnotatorState state = aModel.getObject();
            return String.format("Showing %d-%d of %d lines [document %d of %d]",
                    state.getFirstVisibleUnitIndex(), state.getLastVisibleUnitIndex(),
                    state.getUnitCount(), state.getDocumentIndex() + 1,
                    state.getNumberOfDocuments());
        });
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public DefaultPagingNavigator createPageNavigator(String aId, AnnotationPageBase aPage)
    {
        return new DefaultPagingNavigator(aId, aPage);
    }
}
