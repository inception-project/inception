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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.findWindowStartCenteringOnSelection;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFirstSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getLastDisplayWindowFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextPageFirstSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getPreviousDisplayWindowSentenceBeginAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorViewState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class SentencePagingStrategy
    implements PagingStrategy
{
    @Override
    public void moveToPreviousPage(AnnotatorViewState aState, CAS aCas)
    {
        int firstSentenceAddress = getFirstSentenceAddress(aCas);

        int previousSentenceAddress = getPreviousDisplayWindowSentenceBeginAddress(
                aCas, aState.getFirstVisibleUnitAddress(), aState.getPreferences().getWindowSize());
        // Since BratAjaxCasUtil.getPreviousDisplayWindowSentenceBeginAddress returns same
        // address
        // if there are not much sentences to go back to as defined in windowSize
        if (
                previousSentenceAddress == aState.getFirstVisibleUnitAddress() &&
                // Check whether it's not the beginning of document
                aState.getFirstVisibleUnitAddress() != firstSentenceAddress) 
        {
            previousSentenceAddress = firstSentenceAddress;
        }

        if (aState.getFirstVisibleUnitAddress() == previousSentenceAddress) {
            throw new IllegalStateException("This is First Page!");
        }

        AnnotationFS sentence = selectByAddr(aCas, AnnotationFS.class, previousSentenceAddress);
        aState.setFirstVisibleUnit(sentence);
        aState.setFocusUnitIndex(getSentenceNumber(aCas, sentence.getBegin()));
    }

    @Override
    public void moveToNextPage(AnnotatorViewState aState, CAS aCas)
    {
        int nextSentenceAddress = getNextPageFirstSentenceAddress(aCas,
                aState.getFirstVisibleUnitAddress(), aState.getPreferences().getWindowSize());

        if (aState.getFirstVisibleUnitAddress() == nextSentenceAddress) {
            throw new IllegalStateException("This is last page!");
        }

        AnnotationFS sentence = selectByAddr(aCas, AnnotationFS.class, nextSentenceAddress);
        aState.setFirstVisibleUnit(sentence);
        aState.setFocusUnitIndex(getSentenceNumber(aCas, sentence.getBegin()));
    }

    @Override
    public void moveToFirstPage(AnnotatorViewState aState, CAS aCas)
    {
        int firstSentenceAddress = getFirstSentenceAddress(aCas);

        if (firstSentenceAddress == aState.getFirstVisibleUnitAddress()) {
            throw new IllegalStateException("This is first page!");
        }

        AnnotationFS sentence = selectByAddr(aCas, Sentence.class, firstSentenceAddress);
        aState.setFirstVisibleUnit(sentence);
        aState.setFocusUnitIndex(getSentenceNumber(aCas, sentence.getBegin()));
    }

    @Override
    public void moveToLastPage(AnnotatorViewState aState, CAS aCas)
    {
        int lastDisplayWindowBeginingSentenceAddress = getLastDisplayWindowFirstSentenceAddress(
                aCas, aState.getPreferences().getWindowSize());
        if (lastDisplayWindowBeginingSentenceAddress == aState.getFirstVisibleUnitAddress()) {
            throw new IllegalStateException("This is last page!");
        }

        AnnotationFS sentence = selectByAddr(aCas, Sentence.class,
                lastDisplayWindowBeginingSentenceAddress);
        aState.setFirstVisibleUnit(sentence);
        aState.setFocusUnitIndex(getSentenceNumber(aCas, sentence.getBegin()));
    }

    @Override
    public void moveToUnit(AnnotatorViewState aState, CAS aCas, int aIndex)
    {
        List<AnnotationFS> units = new ArrayList<>(select(aCas, getType(aCas, Sentence.class)));
        
        // Index is 1-based!
        // The code below sets the focus unit index explicitly - see comment on getSentenceNumber
        // in moveToOffset for an explanation. We already know the index here, so no need to
        // calculate it (wrongly) using getSentenceNumber.
        if (aIndex <= 0) {
            moveToOffset(aState, aCas, units.get(0).getBegin());
            aState.setFocusUnitIndex(1);
        }
        else if (aIndex > units.size()) {
            moveToOffset(aState, aCas, units.get(units.size() - 1).getBegin());
            aState.setFocusUnitIndex(units.size());
        }
        else {
            moveToOffset(aState, aCas, units.get(aIndex - 1).getBegin());
            aState.setFocusUnitIndex(aIndex);
        }
    }
    
    @Override
    public void moveToOffset(AnnotatorViewState aState, CAS aCas, int aOffset)
    {
        // Fetch the first sentence on screen or first sentence
        AnnotationFS sentence;
        if (aState.getFirstVisibleUnitAddress() > -1) {
            sentence = selectByAddr(aCas, Sentence.class, aState.getFirstVisibleUnitAddress());
        }
        else {
            sentence = getFirstSentence(aCas);
        }
        
        // Calculate the first sentence in the window in such a way that the annotation
        // currently selected is in the center of the window
        sentence = findWindowStartCenteringOnSelection(aCas, sentence, aOffset,
                aState.getProject(), aState.getDocument(), aState.getPreferences().getWindowSize());
        
        // Move to it
        aState.setFirstVisibleUnit(sentence);
        
        // FIXME getSentenceNumber is not a good option... if we aim for the begin offset of the
        // very last unit, then we get (max-units - 1) instead of (max-units). However, this
        // method is used also in curation and I dimly remember that things broke when I tried
        // to fix it. Probably better to move away from it in the long run. -- REC
        aState.setFocusUnitIndex(getSentenceNumber(aCas, aOffset));
    }

    @Override
    public void moveToSelection(AnnotatorViewState aState, CAS aCas)
    {
        moveToOffset(aState, aCas, aState.getSelection().getBegin());
    }

    @Override
    public void moveForward(AnnotatorViewState aState, CAS aCas)
    {
        // Fetch the first sentence on screen
        AnnotationFS sentence = selectByAddr(aCas, Sentence.class,
                aState.getFirstVisibleUnitAddress());
        // Find the following one
        int address = getNextSentenceAddress(aCas, sentence);
        // Move to it
        aState.setFirstVisibleUnit(selectByAddr(aCas, Sentence.class, address));
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
    public Component createPageNavigator(String aId, AnnotationPageBase aPage)
    {
        return new SentencePagingNavigator(aId, aPage);
    }
}
