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
package de.tudarmstadt.ukp.inception.revieweditor;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.revieweditor.event.RefreshEvent;
import de.tudarmstadt.ukp.inception.revieweditor.event.SelectAnnotationEvent;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public class ReviewEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = 7926530989189120097L;
    private static final Logger LOG = LoggerFactory.getLogger(ReviewEditor.class);

    public ReviewEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        add(new StructuredReviewDraftPanel("vis", aModel, aCasProvider));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
    }

    @Override
    public void render(AjaxRequestTarget aTarget)
    {
        // TODO: maybe not the best method, however this fixes jumping in the editor on
        // selecting annotations or updating values in the right sidebar which occurs on
        // using aTarget.add(this);
        send(getPage(), Broadcast.BREADTH, new RefreshEvent(aTarget));
    }

    @OnEvent(stop = true)
    public void onSelectAnnotationEvent(SelectAnnotationEvent aEvent)
    {
        // TODO: there was a problem with passing this object down to the SpanAnnotationPanel
        // to call the actionSelection there, hence used events
        try {
            AjaxRequestTarget target = aEvent.getTarget();
            VID vid = aEvent.getVid();
            int begin = aEvent.getBegin();
            int end = aEvent.getEnd();
            CAS cas = getCasProvider().get();

            getModelObject().getSelection().selectSpan(vid, cas, begin, end);

            if (getModelObject().isSlotArmed()) {
                getActionHandler().actionFillSlot(target, cas, vid);
            }
            else {
                getActionHandler().actionSelect(target);
            }
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
        }
        catch (AnnotationException e) {
            LOG.error("Unable to select annotation", e);
        }
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        return Optional.empty();
    }
}
