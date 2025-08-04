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
package de.tudarmstadt.ukp.inception.annotation.layer.span.undo;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostActionScrollToAndSelect;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.AnnotationAction_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SegmentationUnitAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.UnitMergedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class MergeUnitAnnotationAction
    extends AnnotationAction_ImplBase
    implements RedoableAnnotationAction, UndoableAnnotationAction
{
    private static final long serialVersionUID = -6268918582061776355L;

    private final int oldBegin;
    private final int oldEnd;
    private final int newBegin;
    private final int newEnd;
    private final VID deletedUnit;

    public MergeUnitAnnotationAction(long aRequestId, UnitMergedEvent aEvent)
    {
        super(aRequestId, aEvent, VID.of(aEvent.getResizedUnit()));
        newBegin = aEvent.getResizedUnit().getBegin();
        newEnd = aEvent.getResizedUnit().getEnd();
        oldBegin = aEvent.getOldBegin();
        oldEnd = aEvent.getOldEnd();
        deletedUnit = VID.of(aEvent.getDeletedUnit());
    }

    @Override
    public Optional<PostAction> undo(AnnotationSchemaService aSchemaService, CAS aCas,
            List<LogMessage> aMessages)
        throws AnnotationException
    {
        var adapter = (SpanAdapter) aSchemaService.getAdapter(getLayer());
        var unitAdapter = new SegmentationUnitAdapter(adapter);
        unitAdapter.unMerge(getDocument(), getUser(), aCas, getVid(), oldBegin, oldEnd,
                deletedUnit);
        aMessages.add(LogMessage.info(this, "[%s] un-merged", getLayer().getUiName()));
        return Optional.of(new PostActionScrollToAndSelect(getVid()));
    }

    @Override
    public Optional<PostAction> redo(AnnotationSchemaService aSchemaService, CAS aCas,
            List<LogMessage> aMessages)
        throws AnnotationException
    {
        var adapter = (SpanAdapter) aSchemaService.getAdapter(getLayer());
        var unitAdapter = new SegmentationUnitAdapter(adapter);
        unitAdapter.unSplit(getDocument(), getUser(), aCas, getVid(), newBegin, newEnd,
                deletedUnit);
        aMessages.add(LogMessage.info(this, "[%s] un-split", getLayer().getUiName()));
        return Optional.of(new PostActionScrollToAndSelect(getVid()));
    }
}
