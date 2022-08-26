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

import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostActionScrollToAndHighlight;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.PostActionScrollToAndSelect;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public class CreateRelationAnnotationAction
    extends AnnotationAction_ImplBase
    implements RedoableAnnotationAction, UndoableAnnotationAction
{
    private static final long serialVersionUID = -6268918582061776355L;

    private final Range range;

    public CreateRelationAnnotationAction(AnnotationSchemaService aSchemaService,
            RelationEvent aEvent)
    {
        super(aEvent, new VID(aEvent.getAnnotation()));

        range = new Range(aEvent.getSourceAnnotation());
    }

    @Override
    public Optional<PostAction> undo(AnnotationSchemaService aSchemaService, CAS aCas)
        throws AnnotationException
    {
        var adapter = aSchemaService.getAdapter(getLayer());
        adapter.delete(getDocument(), getUser(), aCas, getVid());
        return Optional.of(new PostActionScrollToAndHighlight(getDocument(), range,
                "[" + getLayer().getUiName() + "] deleted"));
    }

    @Override
    public Optional<PostAction> redo(AnnotationSchemaService aSchemaService, CAS aCas)
        throws AnnotationException
    {
        var adapter = (RelationAdapter) aSchemaService.getAdapter(getLayer());
        adapter.restore(getDocument(), getUser(), aCas, getVid());
        return Optional.of(new PostActionScrollToAndSelect(getVid(),
                "[" + getLayer().getUiName() + "] restored"));
    }
}
