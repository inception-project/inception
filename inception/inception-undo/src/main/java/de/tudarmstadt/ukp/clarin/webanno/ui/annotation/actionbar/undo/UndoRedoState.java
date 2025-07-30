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

import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RedoableAnnotationAction;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationAction;

public class UndoRedoState
    implements Serializable
{
    private static final int MAX_HISTORY_SIZE = 100;

    private static final long serialVersionUID = 6899707476888758707L;

    private final Deque<UndoableAnnotationAction> undoableActions;
    private final Deque<RedoableAnnotationAction> redoableActions;

    public UndoRedoState()
    {
        undoableActions = new LinkedList<>();
        redoableActions = new LinkedList<>();
    }

    public void pushRedoable(RedoableAnnotationAction aRedo)
    {
        redoableActions.push(aRedo);

        while (redoableActions.size() > MAX_HISTORY_SIZE) {
            redoableActions.removeLast();
        }
    }

    public Optional<RedoableAnnotationAction> peekRedoable()
    {
        return Optional.ofNullable(redoableActions.peek());
    }

    public RedoableAnnotationAction popRedoable()
    {
        return redoableActions.pop();
    }

    public boolean hasRedoableActions()
    {
        return !redoableActions.isEmpty();
    }

    public void clearRedoableActions()
    {
        redoableActions.clear();
    }

    public void pushUndoable(UndoableAnnotationAction aRedo)
    {
        undoableActions.push(aRedo);

        while (undoableActions.size() > 100) {
            undoableActions.removeLast();
        }
    }

    public Optional<UndoableAnnotationAction> peekUndoable()
    {
        return Optional.ofNullable(undoableActions.peek());
    }

    public UndoableAnnotationAction popUndoable()
    {
        return undoableActions.pop();
    }

    public boolean hasUndoableActions()
    {
        return !undoableActions.isEmpty();
    }
}
