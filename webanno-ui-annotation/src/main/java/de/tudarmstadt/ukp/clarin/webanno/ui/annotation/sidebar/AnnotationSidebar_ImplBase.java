/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.io.IOException;
import java.util.Collection;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class AnnotationSidebar_ImplBase
    extends Panel
{
    private static final long serialVersionUID = 8637373389151630602L;

    private AnnotationActionHandler actionHandler;
    private JCasProvider jcasProvider;
    private AnnotationPage annotationPage;
    private @SpringBean DocumentService documentService;

    public AnnotationSidebar_ImplBase(final String aId, final IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel);
        actionHandler = aActionHandler;
        jcasProvider = aJCasProvider;

        annotationPage = aAnnotationPage;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The sidebar is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);
    }

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    public AnnotationActionHandler getActionHandler()
    {
        return actionHandler;
    }

    public JCasProvider getJCasProvider()
    {
        return jcasProvider;
    }

    /**
     * Show the next document if it exists
     */
    protected void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        annotationPage.actionShowSelectedDocument(aTarget, aDocument);
    }

    /**
     * Show the next document if it exists, starting in a certain token position
     */
    @Deprecated
    protected void actionShowSelectedDocumentByTokenPosition(AjaxRequestTarget aTarget,
            SourceDocument aDocument, int aTokenNumber)
        throws IOException
    {
        annotationPage.actionShowSelectedDocument(aTarget, aDocument);

        AnnotatorState state = getModelObject();

        JCas jCas = annotationPage.getEditorCas();

        Collection<Token> tokenCollection = JCasUtil.select(jCas, Token.class);
        Token[] tokens = tokenCollection.toArray(new Token[tokenCollection.size()]);

        int sentenceNumber = WebAnnoCasUtil.getSentenceNumber(jCas,
                tokens[aTokenNumber].getBegin());
        Sentence sentence = WebAnnoCasUtil.getSentence(jCas, tokens[aTokenNumber].getBegin());

        annotationPage.getGotoPageTextField().setModelObject(sentenceNumber);

        state.setFirstVisibleUnit(sentence);
        state.setFocusUnitIndex(sentenceNumber);

        annotationPage.actionRefreshDocument(aTarget);
    }

    /**
     * Show the next document if it exists, starting in a certain begin offset
     */
    protected void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBeginOffset)
        throws IOException
    {
        annotationPage.actionShowSelectedDocument(aTarget, aDocument);

        AnnotatorState state = getModelObject();

        JCas jCas = annotationPage.getEditorCas();

        int sentenceNumber = WebAnnoCasUtil.getSentenceNumber(jCas, aBeginOffset);
        Sentence sentence = WebAnnoCasUtil.getSentence(jCas, aBeginOffset);

        annotationPage.getGotoPageTextField().setModelObject(sentenceNumber);

        state.setFirstVisibleUnit(sentence);
        state.setFocusUnitIndex(sentenceNumber);

        annotationPage.actionRefreshDocument(aTarget);
    }
}
