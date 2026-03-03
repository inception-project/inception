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

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.paging.Unit;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class SentenceOrientedPagingStrategy
    extends PagingStrategy_ImplBase
{
    private static final long serialVersionUID = -3983123604003839467L;

    @Override
    public List<Unit> units(CAS aCas, int aFirstIndex, int aLastIndex)
    {
        var units = new ArrayList<Unit>();
        int i = 1;
        for (var sentence : selectSentences(aCas)) {
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
        String sentId = null;
        try {
            sentId = FSUtil.getFeature(aSentence, "id", String.class);
        }
        catch (IllegalArgumentException e) {
            // Ignore if there is no "id" feature on the sentence
        }
        return new Unit(VID.of(aSentence), sentId, aIndex, aSentence.getBegin(),
                aSentence.getEnd());
    }

    @Override
    public Component createPositionLabel(String aId, IModel<AnnotatorState> aModel)
    {
        var label = new Label(aId, () -> {
            var state = aModel.getObject();
            return String.format("%d-%d / %d sentences [doc %d / %d]",
                    state.getFirstVisibleUnitIndex(), state.getLastVisibleUnitIndex(),
                    state.getUnitCount(), state.getDocumentIndex() + 1,
                    state.getNumberOfDocuments());
        });
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public DefaultPagingNavigator createPageNavigator(String aId, Page aPage)
    {
        return new DefaultPagingNavigator(aId, (AnnotationPageBase) aPage);
    }
}
