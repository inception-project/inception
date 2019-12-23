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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class SentenceOrientedPagingStrategy
    implements PagingStrategy
{
    private static final long serialVersionUID = -3983123604003839467L;

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        List<Unit> units = new ArrayList<>();
        int i = 1;
        for (AnnotationFS sentence : selectSentences(aCas)) {
            if (i > aLastIndex) {
                break;
            }
            
            if (i >= aFirstIndex) {
                units.add(toUnit(i, sentence));
            }
            
            i++;
        }
        return units;
    }
    
    @Override
    public int unitCount(CAS aCas)
    {
        // This is way faster than the default implementation which first materializes all units
        return aCas.getAnnotationIndex(CasUtil.getType(aCas, Sentence.class)).size(); 
    }
    
    private Unit toUnit(int aIndex, AnnotationFS aSentence)
    {
        // If there is a sentence ID, then make it accessible to the user via a sentence-level
        // comment.
        String sentId = FSUtil.getFeature(aSentence, "id", String.class);
        return new Unit(sentId, aIndex, aSentence.getBegin(), aSentence.getEnd());
    }
    
    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        Label label = new Label(aId, () -> {
            AnnotatorState state = aModel.getObject();
            return String.format("Showing %d-%d of %d sentences [document %d of %d]",
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
