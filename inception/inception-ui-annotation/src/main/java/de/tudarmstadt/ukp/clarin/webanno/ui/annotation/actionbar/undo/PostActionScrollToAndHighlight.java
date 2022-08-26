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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketExceptionUtil.handleException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class PostActionScrollToAndHighlight
    implements PostAction
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SourceDocument document;
    private final Range range;
    private final String message;

    public PostActionScrollToAndHighlight(SourceDocument aDocument, Range aRange, String aMessage)
    {
        document = aDocument;
        range = aRange;
        message = aMessage;
    }

    @Override
    public void apply(Component aContextComponent, AjaxRequestTarget aTarget)
    {
        try {
            AnnotationPageBase page = aContextComponent.findParent(AnnotationPageBase.class);
            page.getAnnotationActionHandler().actionClear(aTarget);
            page.actionShowSelectedDocument(aTarget, document, range.getBegin(), range.getEnd());
            // FIXME: the highlighting part is still missing...

            if (StringUtils.isNotBlank(message)) {
                aContextComponent.info(message);
                aTarget.addChildren(page, IFeedback.class);
            }
        }
        catch (IOException | AnnotationException e) {
            handleException(LOG, aContextComponent, aTarget, e);
        }
    }
}
