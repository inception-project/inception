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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostActionScrollToAndSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanMovedEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class MoveSpanAnnotationAction
    extends AnnotationAction_ImplBase
    implements RedoableAnnotationAction, UndoableAnnotationAction
{
    private static final long serialVersionUID = -6268918582061776355L;

    private final Range range;
    private final Range oldRange;

    public MoveSpanAnnotationAction(long aRequestId, AnnotationSchemaService aSchemaService,
            SpanMovedEvent aEvent)
    {
        super(aRequestId, aEvent, new VID(aEvent.getAnnotation()));

        range = new Range(aEvent.getAnnotation());
        oldRange = new Range(aEvent.getOldBegin(), aEvent.getOldEnd());
    }

    @Override
    public Optional<PostAction> undo(AnnotationSchemaService aSchemaService, CAS aCas,
            List<LogMessage> aMessages)
        throws AnnotationException
    {
        var adapter = (SpanAdapter) aSchemaService.getAdapter(getLayer());
        AnnotationFS fs = ICasUtil.selectAnnotationByAddr(aCas, getVid().getId());
        adapter.move(getDocument(), getUser(), aCas, fs, oldRange.getBegin(), oldRange.getEnd());
        aMessages.add(LogMessage.info(this, "[%s] moved back", getLayer().getUiName()));
        return Optional.of(new PostActionScrollToAndSelect(getVid()));
    }

    @Override
    public Optional<PostAction> redo(AnnotationSchemaService aSchemaService, CAS aCas,
            List<LogMessage> aMessages)
        throws AnnotationException
    {
        var adapter = (SpanAdapter) aSchemaService.getAdapter(getLayer());
        AnnotationFS fs = ICasUtil.selectAnnotationByAddr(aCas, getVid().getId());
        adapter.move(getDocument(), getUser(), aCas, fs, range.getBegin(), range.getEnd());
        aMessages.add(LogMessage.info(this, "[%s] moved", getLayer().getUiName()));
        return Optional.of(new PostActionScrollToAndSelect(getVid()));
    }
}
