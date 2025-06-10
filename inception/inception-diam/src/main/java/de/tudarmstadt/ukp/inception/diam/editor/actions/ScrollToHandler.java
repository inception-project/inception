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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRangeList;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#scrollToHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_NAVIGATE_HANDLER)
public class ScrollToHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "scrollTo";

    private final DocumentService documentService;

    public ScrollToHandler(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public DefaultAjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var page = getPage();
            var cas = page.getEditorCas();

            var requestParameters = aRequest.getRequestParameters();

            scrollTo(aTarget, page, cas, requestParameters);

            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to scroll to target", e);
        }
    }

    private void scrollTo(AjaxRequestTarget aTarget, AnnotationPageBase page, CAS cas,
            IRequestParameters requestParameters)
        throws IOException, AnnotationException
    {
        var vid = VID
                .parseOptional(requestParameters.getParameterValue(PARAM_ID).toOptionalString());

        var project = page.getModelObject().getProject();
        var doc = page.getModelObject().getDocument();
        var docId = requestParameters.getParameterValue(PARAM_DOCUMENT_ID).toLong(-1);
        if (docId != -1) {
            doc = documentService.getSourceDocument(project.getId(), docId);
            if (doc == null) {
                throw new AnnotationException("Target document not found");
            }
        }

        if (vid.isSet() && !vid.isSynthetic()) {
            var fs = selectAnnotationByAddr(page.getEditorCas(), vid.getId());
            page.actionShowSelectedDocument(aTarget, doc, fs.getBegin(), fs.getEnd());
            return;
        }

        if (!requestParameters.getParameterValue(PARAM_OFFSETS).isEmpty()) {
            var offsets = getRangeFromRequest(requestParameters, cas);
            page.actionShowSelectedDocument(aTarget, doc, offsets.getBegin(), offsets.getEnd());
            return;
        }
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private Range getRangeFromRequest(IRequestParameters request, CAS aCas) throws IOException
    {
        var offsets = request.getParameterValue(PARAM_OFFSETS).toString();

        var offsetLists = JSONUtil.fromJsonString(CompactRangeList.class, offsets);

        var begin = offsetLists.get(0).getBegin();
        var end = offsetLists.get(offsetLists.size() - 1).getEnd();

        return new Range(begin, end);
    }
}
