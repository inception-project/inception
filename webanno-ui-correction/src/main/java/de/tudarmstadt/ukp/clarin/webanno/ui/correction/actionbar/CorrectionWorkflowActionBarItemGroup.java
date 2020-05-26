/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.ui.correction.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils.addOrUpdateCasMetadata;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotatorWorkflowActionBarItemGroup;

public class CorrectionWorkflowActionBarItemGroup
    extends AnnotatorWorkflowActionBarItemGroup
{
    private static final long serialVersionUID = -1019694761297268468L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    public CorrectionWorkflowActionBarItemGroup(String aId, AnnotationPageBase aPage)
    {
        super(aId, aPage);
    }

    /*
     * This is overwritten because in addition to the reset logic, we also need to strip the
     * annotations from the CAS.
     */
    @Override
    protected void actionResetDocument(AjaxRequestTarget aTarget) throws Exception
    {
        AnnotatorState state = getAnnotationPage().getModelObject();
        SourceDocument document = state.getDocument();
        User user = state.getUser();
        AnnotationDocument adoc = documentService.getAnnotationDocument(document, user);
        CAS cas = documentService.createOrReadInitialCas(document);
        cas = BratAnnotatorUtility.clearAnnotations(cas);

        // Add/update the CAS metadata
        File casFile = documentService.getCasFile(document, user.getUsername());
        if (casFile.exists()) {
            addOrUpdateCasMetadata(cas, casFile, document, user.getUsername());
        }
        documentService.writeAnnotationCas(cas, document, user, false);
        applicationEventPublisherHolder.get()
                .publishEvent(new AfterDocumentResetEvent(this, adoc, cas));
        getAnnotationPage().actionLoadDocument(aTarget);
    }

}
